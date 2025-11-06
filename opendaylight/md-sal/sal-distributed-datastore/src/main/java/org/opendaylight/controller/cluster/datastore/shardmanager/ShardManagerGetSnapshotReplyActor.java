/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.ReceiveTimeout;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary actor used by the ShardManager to compile GetSnapshot replies from the Shard actors and return
 * a DatastoreSnapshot instance reply.
 *
 * @author Thomas Pantelis
 */
final class ShardManagerGetSnapshotReplyActor extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerGetSnapshotReplyActor.class);

    private final Set<String> remainingShardNames;
    private final Params params;
    private final List<ShardSnapshot> shardSnapshots = new ArrayList<>();

    private ShardManagerGetSnapshotReplyActor(final Params params) {
        this.params = params;
        remainingShardNames = new HashSet<>(params.shardNames);

        LOG.debug("{}: Expecting {} shard snapshot replies", params.id, params.shardNames.size());

        getContext().setReceiveTimeout(params.receiveTimeout);
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof GetSnapshotReply msg) {
            onGetSnapshotReply(msg);
        } else if (message instanceof Failure) {
            LOG.debug("{}: Received {}", params.id, message);

            params.replyToActor.tell(message, self());
            self().tell(PoisonPill.getInstance(), self());
        } else if (message instanceof ReceiveTimeout) {
            LOG.warn("{}: Timed out after {} ms while waiting for snapshot replies from {} shard(s). "
                + "{} shard(s) {} did not respond", params.id, params.receiveTimeout.toMillis(),
                params.shardNames.size(), remainingShardNames.size(), remainingShardNames);
            params.replyToActor.tell(new Failure(new TimeoutException(String.format(
                "Timed out after %s ms while waiting for snapshot replies from %d shard(s). %d shard(s) %s "
                + "did not respond.", params.receiveTimeout.toMillis(), params.shardNames.size(),
                remainingShardNames.size(), remainingShardNames))), self());
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    private void onGetSnapshotReply(final GetSnapshotReply getSnapshotReply) {
        LOG.debug("{}: Received {}", params.id, getSnapshotReply);

        final var shardName = ShardIdentifier.fromShardIdString(getSnapshotReply.id()).getShardName();
        shardSnapshots.add(new ShardSnapshot(shardName, getSnapshotReply.snapshot()));

        remainingShardNames.remove(shardName);
        if (remainingShardNames.isEmpty()) {
            LOG.debug("{}: All shard snapshots received", params.id);

            final var datastoreSnapshot = new DatastoreSnapshot(params.datastoreType, params.shardManagerSnapshot,
                shardSnapshots);
            params.replyToActor.tell(datastoreSnapshot, self());
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    public static Props props(final Collection<String> shardNames, final String datastoreType,
            final ShardManagerSnapshot shardManagerSnapshot, final ActorRef replyToActor, final String id,
            final Duration receiveTimeout) {
        return Props.create(ShardManagerGetSnapshotReplyActor.class, new Params(shardNames, datastoreType,
                shardManagerSnapshot, replyToActor, id, receiveTimeout));
    }

    private static final class Params {
        final Collection<String> shardNames;
        final String datastoreType;
        final ShardManagerSnapshot shardManagerSnapshot;
        final ActorRef replyToActor;
        final String id;
        final Duration receiveTimeout;

        Params(final Collection<String> shardNames, final String datastoreType,
                final ShardManagerSnapshot shardManagerSnapshot, final ActorRef replyToActor, final String id,
                final Duration receiveTimeout) {
            this.shardNames = shardNames;
            this.datastoreType = datastoreType;
            this.shardManagerSnapshot = shardManagerSnapshot;
            this.replyToActor = replyToActor;
            this.id = id;
            this.receiveTimeout = receiveTimeout;
        }
    }
}
