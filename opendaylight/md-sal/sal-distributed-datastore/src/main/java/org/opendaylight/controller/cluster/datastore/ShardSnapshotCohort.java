/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.ShardSnapshotActor;
import org.opendaylight.controller.cluster.datastore.messages.CreateSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;

/**
 * Participates in raft snapshotting on behalf of a Shard actor.
 *
 * @author Thomas Pantelis
 */
class ShardSnapshotCohort implements RaftActorSnapshotCohort {
    private static final FrontendType SNAPSHOT_APPLY = FrontendType.forName("snapshot-apply");
    private static final String SNAPSHOT_READ = "snapshot-read";

    private final LocalHistoryIdentifier applyHistoryId;
    private final String snapshotActorNamePrefix;
    private final ActorContext actorContext;
    private final ShardDataTree store;
    private final String logId;
    private final Logger log;

    private long applyCounter;
    private long readCounter;

    ShardSnapshotCohort(final ActorContext actorContext, final MemberName memberName, final ShardDataTree store,
        final Logger log, final String logId) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.store = Preconditions.checkNotNull(store);
        this.log = log;
        this.logId = logId;

        this.applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);

        final StringBuilder sb = new StringBuilder("shard-");
        sb.append(memberName.getName()).append(':').append(SNAPSHOT_READ).append('-');
        this.snapshotActorNamePrefix = sb.toString();
    }

    private String nextSnapshotActorName() {
        return snapshotActorNamePrefix + Long.toUnsignedString(readCounter++);
    }

    @Override
    public void createSnapshot(final ActorRef actorRef) {
        // Create a transaction actor. We are really going to treat the transaction as a worker
        // so that this actor does not get block building the snapshot. THe transaction actor will
        // after processing the CreateSnapshot message.

        final ActorRef snapshotActor = actorContext.actorOf(ShardSnapshotActor.props(store.takeRecoverySnapshot()),
            nextSnapshotActorName());
        snapshotActor.tell(CreateSnapshot.INSTANCE, actorRef);
    }

    private void deserializeAndApplySnapshot(final byte[] snapshotBytes) {
        final AbstractShardDataTreeSnapshot snapshot;
        try {
            snapshot = AbstractShardDataTreeSnapshot.deserialize(snapshotBytes);
        } catch (IOException e) {
            log.error("{}: Failed to deserialize snapshot", logId, e);
            return;
        }

        try {
            final ReadWriteShardDataTreeTransaction transaction = store.newReadWriteTransaction(
                new TransactionIdentifier(applyHistoryId, applyCounter++));

            // delete everything first
            transaction.getSnapshot().delete(YangInstanceIdentifier.EMPTY);

            final Optional<NormalizedNode<?, ?>> maybeNode = snapshot.getRootNode();
            if (maybeNode.isPresent()) {
                // Add everything from the remote node back
                transaction.getSnapshot().write(YangInstanceIdentifier.EMPTY, maybeNode.get());
            }

            store.applyRecoveryTransaction(transaction);
        } catch (Exception e) {
            log.error("{}: An exception occurred when applying snapshot", logId, e);
        }
    }

    @Override
    public void applySnapshot(final byte[] snapshotBytes) {
        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        log.info("{}: Applying snapshot", logId);
        deserializeAndApplySnapshot(snapshotBytes);
        log.info("{}: Done applying snapshot", logId);
    }
}
