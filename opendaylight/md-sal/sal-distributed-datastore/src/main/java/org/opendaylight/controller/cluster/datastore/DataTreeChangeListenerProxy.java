/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
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
    private final ActorRef dataChangeListenerActor;
    private final ActorContext actorContext;
    private final YangInstanceIdentifier registeredPath;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    DataTreeChangeListenerProxy(final ActorContext actorContext, final T listener,
            final YangInstanceIdentifier registeredPath) {
        super(listener);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.registeredPath = Preconditions.checkNotNull(registeredPath);
        this.dataChangeListenerActor = actorContext.getActorSystem().actorOf(
                DataTreeChangeListenerActor.props(getInstance(), registeredPath)
                    .withDispatcher(actorContext.getNotificationDispatcherPath()));

        LOG.debug("{}: Created actor {} for DTCL {}", actorContext.getDatastoreContext().getLogicalStoreType(),
                dataChangeListenerActor, listener);
    }

    @Override
    protected synchronized void removeRegistration() {
        if (listenerRegistrationActor != null) {
            listenerRegistrationActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(),
                    ActorRef.noSender());
            listenerRegistrationActor = null;
        }

        dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    void init(final String shardName) {
        Future<ActorRef> findFuture = actorContext.findLocalShardAsync(shardName);
        findFuture.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("{}: No local shard found for {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", logContext(), shardName, getInstance(), registeredPath);
                } else if (failure != null) {
                    LOG.error("{}: Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", logContext(), shardName, getInstance(), registeredPath,
                            failure);
                } else {
                    doRegistration(shard);
                }
            }
        }, actorContext.getClientDispatcher());
    }

    private void setListenerRegistrationActor(final ActorSelection actor) {
        if (actor == null) {
            LOG.debug("{}: Ignoring null actor on {}", logContext(), this);
            return;
        }

        synchronized (this) {
            if (!isClosed()) {
                this.listenerRegistrationActor = actor;
                return;
            }
        }

        // This registration has already been closed, notify the actor
        actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), null);
    }

    private void doRegistration(final ActorRef shard) {

        Future<Object> future = actorContext.executeOperationAsync(shard,
                new RegisterDataTreeChangeListener(registeredPath, dataChangeListenerActor,
                        getInstance() instanceof ClusteredDOMDataTreeChangeListener),
                actorContext.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object result) {
                if (failure != null) {
                    LOG.error("{}: Failed to register DataTreeChangeListener {} at path {}", logContext(),
                            getInstance(), registeredPath, failure);
                } else {
                    RegisterDataTreeNotificationListenerReply reply = (RegisterDataTreeNotificationListenerReply)result;
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

    private String logContext() {
        return actorContext.getDatastoreContext().getLogicalStoreType().toString();
    }
}
