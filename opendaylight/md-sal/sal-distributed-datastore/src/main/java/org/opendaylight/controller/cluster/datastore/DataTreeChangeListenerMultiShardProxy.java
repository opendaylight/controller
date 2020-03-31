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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

public class DataTreeChangeListenerMultiShardProxy extends DataTreeChangeListenerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerMultiShardProxy.class);

    private List<ActorSelection> registrations;

    public DataTreeChangeListenerMultiShardProxy(final ActorUtils actorUtils,
            final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath) {
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
    }

    @Override
    protected void unregister() {
        if (registrations != null) {
            for (ActorSelection actor : registrations) {
                actor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(),
                        ActorRef.noSender());
            }
            registrations.clear();
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
