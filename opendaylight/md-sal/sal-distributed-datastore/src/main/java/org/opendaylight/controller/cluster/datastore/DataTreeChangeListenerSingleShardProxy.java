/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

public class DataTreeChangeListenerSingleShardProxy extends DataTreeChangeListenerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerSingleShardProxy.class);

    @GuardedBy("this")
    private ActorSelection registrationActor;

    public DataTreeChangeListenerSingleShardProxy(final ActorUtils actorUtils,
            final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath) {
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
                                    + "cannot be registered", logContext(), shardName, getInstance(), getRegisteredPath(),
                            failure);
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
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
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
