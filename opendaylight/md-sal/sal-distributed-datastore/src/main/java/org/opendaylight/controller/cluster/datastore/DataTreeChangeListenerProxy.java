/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
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
    private final ActorUtils actorUtils;
    private final YangInstanceIdentifier registeredPath;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    DataTreeChangeListenerProxy(final ActorUtils actorUtils, final T listener,
            final YangInstanceIdentifier registeredPath) {
        super(listener);
        this.actorUtils = requireNonNull(actorUtils);
        this.registeredPath = requireNonNull(registeredPath);
        this.dataChangeListenerActor = actorUtils.getActorSystem().actorOf(
                DataTreeChangeListenerActor.props(getInstance(), registeredPath)
                    .withDispatcher(actorUtils.getNotificationDispatcherPath()));

        LOG.debug("{}: Created actor {} for DTCL {}", actorUtils.getDatastoreContext().getLogicalStoreType(),
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
        Future<ActorRef> findFuture = actorUtils.findLocalShardAsync(shardName);
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
        }, actorUtils.getClientDispatcher());
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

        Future<Object> future = actorUtils.executeOperationAsync(shard,
                new RegisterDataTreeChangeListener(registeredPath, dataChangeListenerActor,
                        getInstance() instanceof ClusteredDOMDataTreeChangeListener),
                actorUtils.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object result) {
                if (failure != null) {
                    LOG.error("{}: Failed to register DataTreeChangeListener {} at path {}", logContext(),
                            getInstance(), registeredPath, failure);
                } else {
                    RegisterDataTreeNotificationListenerReply reply = (RegisterDataTreeNotificationListenerReply)result;
                    setListenerRegistrationActor(actorUtils.actorSelection(
                            reply.getListenerRegistrationPath()));
                }
            }
        }, actorUtils.getClientDispatcher());
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
        return actorUtils.getDatastoreContext().getLogicalStoreType().toString();
    }
}
