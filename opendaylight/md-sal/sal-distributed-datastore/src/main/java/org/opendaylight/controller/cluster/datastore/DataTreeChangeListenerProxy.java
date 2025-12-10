/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.dispatch.OnComplete;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy class for holding required state to lazily instantiate a listener registration with an
 * asynchronously-discovered actor.
 *
 * @param <T> listener type
 */
final class DataTreeChangeListenerProxy extends AbstractObjectRegistration<DOMDataTreeChangeListener> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerProxy.class);
    private final ActorRef dataChangeListenerActor;
    private final ActorUtils actorUtils;
    private final YangInstanceIdentifier registeredPath;
    private final boolean clustered;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    @VisibleForTesting
    private DataTreeChangeListenerProxy(final ActorUtils actorUtils, final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath, final boolean clustered, final String shardName) {
        super(listener);
        this.actorUtils = requireNonNull(actorUtils);
        this.registeredPath = requireNonNull(registeredPath);
        this.clustered = clustered;
        dataChangeListenerActor = actorUtils.getActorSystem()
            .actorOf(DataTreeChangeListenerActor.props(getInstance(), registeredPath)
                .withDispatcher(actorUtils.getNotificationDispatcherPath()));
        LOG.debug("{}: Created actor {} for DTCL {}", actorUtils.getDatastoreContext().getLogicalStoreType(),
            dataChangeListenerActor, listener);
    }

    static @NonNull DataTreeChangeListenerProxy of(final ActorUtils actorUtils,
            final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath,
            final boolean clustered, final String shardName) {
        return ofTesting(actorUtils, listener, registeredPath, clustered, shardName, MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    static @NonNull DataTreeChangeListenerProxy ofTesting(final ActorUtils actorUtils,
            final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath,
            final boolean clustered, final String shardName, final Executor executor) {
        final var ret = new DataTreeChangeListenerProxy(actorUtils, listener, registeredPath, clustered, shardName);
        executor.execute(() -> {
            LOG.debug("{}: Starting discovery of shard {}", ret.logContext(), shardName);
            actorUtils.findLocalShardAsync(shardName).onComplete(new OnComplete<>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef shard) {
                    if (failure == null) {
                        ret.doRegistration(shard);
                        return;
                    }

                    if (failure instanceof LocalShardNotFoundException) {
                        LOG.debug("{}: No local shard found for {} - DataTreeChangeListener {} at path {} cannot be "
                            + "registered", ret.logContext(), shardName, listener, registeredPath);
                    } else {
                        LOG.error("{}: Failed to find local shard {} - DataTreeChangeListener {} at path {} cannot be "
                            + "registered", ret.logContext(), shardName, listener, registeredPath, failure);
                    }
                }
            }, actorUtils.getClientDispatcher());
        });
        return ret;
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

    private void setListenerRegistrationActor(final ActorSelection actor) {
        if (actor == null) {
            LOG.debug("{}: Ignoring null actor on {}", logContext(), this);
            return;
        }

        synchronized (this) {
            if (!isClosed()) {
                listenerRegistrationActor = actor;
                return;
            }
        }

        // This registration has already been closed, notify the actor
        actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), null);
    }

    private void doRegistration(final ActorRef shard) {
        actorUtils.executeOperationAsync(shard,
            new RegisterDataTreeChangeListener(registeredPath, dataChangeListenerActor, clustered),
            actorUtils.getDatastoreContext().getShardInitializationTimeout()).onComplete(new OnComplete<>() {
                @Override
                public void onComplete(final Throwable failure, final Object result) {
                    if (failure != null) {
                        LOG.error("{}: Failed to register DataTreeChangeListener {} at path {}", logContext(),
                            getInstance(), registeredPath, failure);
                    } else {
                        setListenerRegistrationActor(actorUtils.actorSelection(
                            ((RegisterDataTreeNotificationListenerReply) result).getListenerRegistrationPath()));
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
