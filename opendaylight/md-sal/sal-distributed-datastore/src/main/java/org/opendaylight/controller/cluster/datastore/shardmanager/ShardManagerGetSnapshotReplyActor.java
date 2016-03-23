/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status.Failure;
import akka.actor.UntypedActor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Temporary actor used by the ShardManager to compile GetSnapshot replies from the Shard actors and return
 * a DatastoreSnapshot instance reply.
 *
 * @author Thomas Pantelis
 */
class ShardManagerGetSnapshotReplyActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerGetSnapshotReplyActor.class);

    private final Set<String> remainingShardNames;
    private final Params params;
    private final List<ShardSnapshot> shardSnapshots = new ArrayList<>();

    private ShardManagerGetSnapshotReplyActor(Params params) {
        this.params = params;
        remainingShardNames = new HashSet<>(params.shardNames);

        LOG.debug("{}: Expecting {} shard snapshot replies", params.id, params.shardNames.size());

        getContext().setReceiveTimeout(params.receiveTimeout);
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof GetSnapshotReply) {
            onGetSnapshotReply((GetSnapshotReply)message);
        } else if(message instanceof Failure) {
            LOG.debug("{}: Received {}", params.id, message);

            params.replyToActor.tell(message, getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        } else if (message instanceof ReceiveTimeout) {
            String msg = String.format(
                    "Timed out after %s ms while waiting for snapshot replies from %d shard(s). %d shard(s) %s did not respond.",
                        params.receiveTimeout.toMillis(), params.shardNames.size(), remainingShardNames.size(),
                        remainingShardNames);
            LOG.warn("{}: {}", params.id, msg);
            params.replyToActor.tell(new Failure(new TimeoutException(msg)), getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    private void onGetSnapshotReply(GetSnapshotReply getSnapshotReply) {
        LOG.debug("{}: Received {}", params.id, getSnapshotReply);

        ShardIdentifier shardId = ShardIdentifier.builder().fromShardIdString(getSnapshotReply.getId()).build();
        shardSnapshots.add(new ShardSnapshot(shardId.getShardName(), getSnapshotReply.getSnapshot()));

        remainingShardNames.remove(shardId.getShardName());
        if(remainingShardNames.isEmpty()) {
            LOG.debug("{}: All shard snapshots received", params.id);

            DatastoreSnapshot datastoreSnapshot = new DatastoreSnapshot(params.datastoreType, params.shardManagerSnapshot,
                    shardSnapshots);
            params.replyToActor.tell(datastoreSnapshot, getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    public static Props props(Collection<String> shardNames, String datastoreType, byte[] shardManagerSnapshot,
            ActorRef replyToActor, String id, Duration receiveTimeout) {
        return Props.create(ShardManagerGetSnapshotReplyActor.class, new Params(shardNames, datastoreType,
                shardManagerSnapshot, replyToActor, id, receiveTimeout));
    }

    private static final class Params {
        final Collection<String> shardNames;
        final String datastoreType;
        final byte[] shardManagerSnapshot;
        final ActorRef replyToActor;
        final String id;
        final Duration receiveTimeout;

        Params(Collection<String> shardNames, String datastoreType, byte[] shardManagerSnapshot, ActorRef replyToActor,
                String id, Duration receiveTimeout) {
            this.shardNames = shardNames;
            this.datastoreType = datastoreType;
            this.shardManagerSnapshot = shardManagerSnapshot;
            this.replyToActor = replyToActor;
            this.id = id;
            this.receiveTimeout = receiveTimeout;
        }
    }
}
