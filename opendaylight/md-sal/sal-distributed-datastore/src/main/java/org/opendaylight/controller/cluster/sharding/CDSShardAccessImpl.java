/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListenerRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CDSShardAccessImpl implements CDSShardAccess, LeaderLocationListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CDSShardAccessImpl.class);

    private final DOMDataTreeIdentifier prefix;
    private final ActorContext actorContext;
    private final Collection<LeaderLocationListener> listeners = Sets.newHashSet();

    private LeaderLocation currentLeader = LeaderLocation.UNKNOWN;
    private boolean closed = false;

    public CDSShardAccessImpl(final DOMDataTreeIdentifier prefix, final ActorContext actorContext) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        init();
    }

    private void init() {
        Optional<ActorRef> localShardReply =
                actorContext.findLocalShard(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));
        Preconditions.checkState(localShardReply.isPresent());
        actorContext.getActorSystem().actorOf(NotifySubscriberActor.props(localShardReply.get(), this));
    }

    public DOMDataTreeIdentifier getShardIdentifier() {
        return prefix;
    }

    public LeaderLocation getLeaderLocation() {
        return currentLeader;
    }

    public CompletionStage<Void> makeLeaderLocal() {
        throw new UnsupportedOperationException();
    }

    public <L extends LeaderLocationListener> LeaderLocationListenerRegistration<L>
            registerLeaderLocationListener(final L listener) {

        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(listeners.contains(listener));

        listeners.add(listener);

        return new LeaderLocationListenerRegistration<L>() {
            @Override
            public L getInstance() {
                return listener;
            }

            @Override
            public void close() {
                listeners.remove(listener);
            }
        };
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onLeaderLocationChanged(final LeaderLocation location) {
        if (closed) {
            return;
        }

        currentLeader = location;
        listeners.forEach(listener -> {
            try {
                listener.onLeaderLocationChanged(location);
            } catch (Exception e) {
                LOG.warn("LeaderLocationListener {} threw an exception {}", listener, e);
            }
        });
    }

    @Override
    public void close() throws Exception {
        closed = true;
    }
}
