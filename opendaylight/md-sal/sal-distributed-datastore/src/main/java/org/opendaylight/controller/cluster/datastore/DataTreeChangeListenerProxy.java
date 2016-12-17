/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath$;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.InvalidActorNameException;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Proxy class for holding required state to lazily instantiate a listener registration with an
 * asynchronously-discovered actor.
 *
 * @param <T> listener type
 */
final class DataTreeChangeListenerProxy<T extends DOMDataTreeChangeListener> extends AbstractListenerRegistration<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerProxy.class);
    private final ActorContext actorContext;

    private ActorRef dataChangeListenerActor;
    private String dataChangeListenerActorName;
    private static final AtomicInteger NEXT_INSTANCE_ID = new AtomicInteger(1);
    private static final char ACTOR_NAME_SEP_CH = '-';

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    DataTreeChangeListenerProxy(final ActorContext actorContext, final T listener) {
        super(listener);
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    @Override
    protected synchronized void removeRegistration() {
        if (listenerRegistrationActor != null) {
            listenerRegistrationActor.tell(CloseDataTreeChangeListenerRegistration.getInstance(), ActorRef.noSender());
            listenerRegistrationActor = null;
        }

        if (dataChangeListenerActor != null) {
            dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    public static String replaceInvalidActorPathCharacterWith(final String strToBeReplaced,
                                                              final char replaceWithCharacter) {
        StringBuilder actorNameBuilder = new StringBuilder();
        for (char charInStr : strToBeReplaced.toCharArray()) {
            if (ActorPath$.MODULE$.isValidPathElement(String.valueOf(charInStr))) {
                actorNameBuilder.append(charInStr);
            } else {
                actorNameBuilder.append(replaceWithCharacter);
            }
        }
        return actorNameBuilder.toString();
    }

    public static String actorNameStrFromYangPathStr(final String yangPathStr) {
        String replacedYangPathStr = yangPathStr.replace('/', '$').replace('?', '@');
        final char charForOtherInvalidChars = '~';
        return replaceInvalidActorPathCharacterWith(replacedYangPathStr, charForOtherInvalidChars);
    }

    protected static int newInstanceId() {
        return NEXT_INSTANCE_ID.getAndIncrement();
    }

    private boolean initActor(final YangInstanceIdentifier treeId) {
        StringBuilder sbActorName = new StringBuilder();
        String actorName = sbActorName.append("DataTreeChangeListener")
                                .append(ACTOR_NAME_SEP_CH)
                                .append(newInstanceId())
                                .append(ACTOR_NAME_SEP_CH)
                                .append(actorNameStrFromYangPathStr(treeId.toString()))
                                .toString();
        try {
            ActorRef actorRef = actorContext.getActorSystem().actorOf(
                DataTreeChangeListenerActor.props(getInstance())
                    .withDispatcher(actorContext.getNotificationDispatcherPath())
                    .withMailbox(ActorContext.BOUNDED_MAILBOX),
                actorName);
            LOG.info("Success to create actor with name \"{}\" for listener listening to \"{}\"",
                     actorName, treeId);
            dataChangeListenerActor     = actorRef;
            dataChangeListenerActorName = actorName;
        } catch (InvalidActorNameException | IllegalStateException e) {
            LOG.error("Failed to create actor with name \"{}\" for listener listening to \"{}\" due to {}",
                      actorName, treeId, e);
            if (dataChangeListenerActor != null) {
                LOG.warn("Data tree change listener actor with name \"{}\" will be removed!",
                         dataChangeListenerActorName);
            }
            dataChangeListenerActor     = null;
            dataChangeListenerActorName = null;
        }

        return dataChangeListenerActor != null;
    }

    void init(final String shardName, final YangInstanceIdentifier treeId) {
        Future<ActorRef> findFuture = actorContext.findLocalShardAsync(shardName);
        findFuture.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", shardName, getInstance(), treeId);
                } else if (failure != null) {
                    LOG.error("Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered: {}", shardName, getInstance(), treeId, failure);
                } else {
                    if (initActor(treeId)) {
                        doRegistration(shard, treeId);
                    }
                }
            }
        }, actorContext.getClientDispatcher());
    }

    private void setListenerRegistrationActor(final ActorSelection actor) {
        if (actor == null) {
            LOG.debug("Ignoring null actor on {}", this);
            return;
        }

        synchronized (this) {
            if (!isClosed()) {
                this.listenerRegistrationActor = actor;
                return;
            }
        }

        // This registration has already been closed, notify the actor
        actor.tell(CloseDataTreeChangeListenerRegistration.getInstance(), null);
    }

    private void doRegistration(final ActorRef shard, final YangInstanceIdentifier path) {

        Future<Object> future = actorContext.executeOperationAsync(shard,
                new RegisterDataTreeChangeListener(path, dataChangeListenerActor,
                        getInstance() instanceof ClusteredDOMDataTreeChangeListener),
                actorContext.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object result) {
                if (failure != null) {
                    LOG.error("Failed to register DataTreeChangeListener {} at path {}",
                            getInstance(), path.toString(), failure);
                } else {
                    RegisterDataTreeChangeListenerReply reply = (RegisterDataTreeChangeListenerReply) result;
                    setListenerRegistrationActor(actorContext.actorSelection(
                            reply.getListenerRegistrationPath()));
                }
            }
        }, actorContext.getClientDispatcher());
    }

    @VisibleForTesting
    synchronized ActorSelection getListenerRegistrationActor() {
        return listenerRegistrationActor;
    }

    @VisibleForTesting
    ActorRef getDataChangeListenerActor() {
        return dataChangeListenerActor;
    }
}
