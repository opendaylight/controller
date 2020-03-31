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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
abstract class DataTreeChangeListenerProxy<T extends DOMDataTreeChangeListener>
        extends AbstractListenerRegistration<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerProxy.class);
    private final ActorRef dataChangeListenerActor;
    private final ActorUtils actorUtils;
    private final YangInstanceIdentifier registeredPath;

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

    public static <T extends DOMDataTreeChangeListener> DataTreeChangeListenerProxy<T> create(
            final ActorUtils actorUtils, final T listener, final YangInstanceIdentifier registeredPath) {
        if (registeredPath.isEmpty() && actorUtils.getConfiguration().getAllShardNames().size() > 1) {
            return new DataTreeChangeListenerMultiShardProxy<>(actorUtils, listener, registeredPath);
        }
        return new DataTreeChangeListenerSingleShardProxy<>(actorUtils, listener, registeredPath);
    }

    public static <T extends DOMDataTreeChangeListener> DataTreeChangeListenerProxy<T> createForPrefixShard(
        final ActorUtils actorUtils, final T listener, final String shardName,
        final YangInstanceIdentifier insidePath) {
        return new DataTreeChangeListenerPrefixShardProxy<>(actorUtils, listener, shardName, insidePath);
    }

    public abstract void init();

    protected abstract void unregister();

    protected abstract void setListenerRegistrationActor(ActorSelection actor);

    @Override
    protected synchronized void removeRegistration() {
        unregister();
        dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    protected void doRegistration(final ActorRef shard) {

        Future<Object> future = actorUtils.executeOperationAsync(shard,
                new RegisterDataTreeChangeListener(registeredPath, dataChangeListenerActor,
                        getInstance() instanceof ClusteredDOMDataTreeChangeListener),
                actorUtils.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<Object>() {
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
    ActorRef getDataChangeListenerActor() {
        return dataChangeListenerActor;
    }

    protected String logContext() {
        return actorUtils.getDatastoreContext().getLogicalStoreType().toString();
    }

    protected ActorUtils getActorUtils() {
        return actorUtils;
    }

    protected YangInstanceIdentifier getRegisteredPath() {
        return registeredPath;
    }

    static final class DataTreeChangeListenerMultiShardProxy<T extends DOMDataTreeChangeListener>
            extends DataTreeChangeListenerProxy<T> {
        private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerMultiShardProxy.class);

        private List<ActorSelection> registrations;

        DataTreeChangeListenerMultiShardProxy(final ActorUtils actorUtils, final T listener,
                final YangInstanceIdentifier registeredPath) {
            super(actorUtils, listener, registeredPath);
        }

        @Override
        public void init() {
            final Set<String> allShardNames = getActorUtils().getConfiguration().getAllShardNames();
            for (String shardName : allShardNames) {
                Future<ActorRef> findFuture = getActorUtils().findLocalShardAsync(shardName);
                findFuture.onComplete(new OnComplete<ActorRef>() {
                    @Override
                    public void onComplete(final Throwable failure, final ActorRef shard) {
                        if (failure instanceof LocalShardNotFoundException) {
                            LOG.debug("{}: No local shard found for {} - DataTreeChangeListener {} at path {} "
                                    + "cannot be registered", logContext(), shardName, getInstance(),
                                    getRegisteredPath());
                        } else if (failure != null) {
                            LOG.error("{}: Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                                            + "cannot be registered", logContext(), shardName, getInstance(),
                                    getRegisteredPath(), failure);
                        } else {
                            doRegistration(shard);
                        }
                    }
                }, getActorUtils().getClientDispatcher());
            }
        }

        @Override
        protected void unregister() {
            synchronized (this) {
                if (registrations != null) {
                    for (ActorSelection actor : registrations) {
                        actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(),
                                ActorRef.noSender());
                    }
                    registrations.clear();
                }
            }
        }

        @Override
        protected void setListenerRegistrationActor(final ActorSelection actor) {
            if (actor == null) {
                LOG.debug("{}: Ignoring null actor on {}", logContext(), this);
                return;
            }

            synchronized (this) {
                if (!isClosed()) {
                    if (registrations == null) {
                        registrations = new LinkedList<>();
                    }
                    this.registrations.add(actor);
                    return;
                }
            }

            // This registration has already been closed, notify the actor
            actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), null);
        }
    }

    static final class DataTreeChangeListenerSingleShardProxy<T extends DOMDataTreeChangeListener>
            extends DataTreeChangeListenerProxy<T> {
        private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerSingleShardProxy.class);

        @GuardedBy("this")
        private ActorSelection registrationActor;

        DataTreeChangeListenerSingleShardProxy(final ActorUtils actorUtils,
                final T listener, final YangInstanceIdentifier registeredPath) {
            super(actorUtils, listener, registeredPath);
        }

        @Override
        public void init() {
            final String shardName = getActorUtils().getShardStrategyFactory().getStrategy(getRegisteredPath())
                    .findShard(getRegisteredPath());
            Future<ActorRef> findFuture = getActorUtils().findLocalShardAsync(shardName);
            findFuture.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef shard) {
                    if (failure instanceof LocalShardNotFoundException) {
                        LOG.debug("{}: No local shard found for {} - DataTreeChangeListener {} at path {} "
                                + "cannot be registered", logContext(), shardName, getInstance(), getRegisteredPath());
                    } else if (failure != null) {
                        LOG.error("{}: Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                                        + "cannot be registered", logContext(), shardName, getInstance(),
                                getRegisteredPath(), failure);
                    } else {
                        doRegistration(shard);
                    }
                }
            }, getActorUtils().getClientDispatcher());
        }

        @Override
        protected void unregister() {
            if (registrationActor != null) {
                registrationActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(),
                        ActorRef.noSender());
                registrationActor = null;
            }
        }

        @Override
        protected void setListenerRegistrationActor(final ActorSelection actor) {
            if (actor == null) {
                LOG.debug("{}: Ignoring null actor on {}", logContext(), this);
                return;
            }

            synchronized (this) {
                if (!isClosed()) {
                    this.registrationActor = actor;
                    return;
                }
            }

            // This registrationActor has already been closed, notify the actor
            actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), null);
        }

        @VisibleForTesting
        synchronized ActorSelection getListenerRegistrationActor() {
            return registrationActor;
        }
    }

    /**
     * When registering other proxies, the shard is determined from the registered path. This is not the case with
     * prefix shards. They are registered to specific shard and specific path inside that shard. Therefore the path can
     * be very generic (such as "/") since it is relative to the specified shard.
     */
    static final class DataTreeChangeListenerPrefixShardProxy<T extends DOMDataTreeChangeListener>
        extends DataTreeChangeListenerProxy<T> {
        private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerPrefixShardProxy.class);

        @GuardedBy("this")
        private ActorSelection registrationActor;

        private final String shardName;

        DataTreeChangeListenerPrefixShardProxy(final ActorUtils actorUtils, final T listener,
            final String shardName, final YangInstanceIdentifier insidePath) {
            super(actorUtils, listener, insidePath);
            this.shardName = shardName;
        }

        @Override
        public void init() {
            Future<ActorRef> findFuture = getActorUtils().findLocalShardAsync(shardName);
            findFuture.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef shard) {
                    if (failure instanceof LocalShardNotFoundException) {
                        LOG.debug("{}: No local shard found for {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", logContext(), shardName, getInstance(), getRegisteredPath());
                    } else if (failure != null) {
                        LOG.error("{}: Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                                + "cannot be registered", logContext(), shardName, getInstance(),
                            getRegisteredPath(), failure);
                    } else {
                        doRegistration(shard);
                    }
                }
            }, getActorUtils().getClientDispatcher());
        }

        @Override
        protected void unregister() {
            if (registrationActor != null) {
                registrationActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(),
                    ActorRef.noSender());
                registrationActor = null;
            }
        }

        @Override
        protected void setListenerRegistrationActor(final ActorSelection actor) {
            if (actor == null) {
                LOG.debug("{}: Ignoring null actor on {}", logContext(), this);
                return;
            }

            synchronized (this) {
                if (!isClosed()) {
                    this.registrationActor = actor;
                    return;
                }
            }

            // This registrationActor has already been closed, notify the actor
            actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), null);
        }
    }
}
