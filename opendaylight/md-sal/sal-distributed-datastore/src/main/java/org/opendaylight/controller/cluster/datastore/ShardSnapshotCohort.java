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
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.ShardSnapshotActor;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
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

    private final LocalHistoryIdentifier applyHistoryId;
    private final ActorRef snapshotActor;
    private final ShardDataTree store;
    private final String logId;
    private final Logger log;

    private long applyCounter;

    private ShardSnapshotCohort(final LocalHistoryIdentifier applyHistoryId, final ActorRef snapshotActor,
            final ShardDataTree store, final Logger log, final String logId) {
        this.applyHistoryId = Preconditions.checkNotNull(applyHistoryId);
        this.snapshotActor = Preconditions.checkNotNull(snapshotActor);
        this.store = Preconditions.checkNotNull(store);
        this.log = log;
        this.logId = logId;
    }

    static ShardSnapshotCohort create(final ActorContext actorContext, final MemberName memberName,
            final ShardDataTree store, final Logger log, final String logId) {
        final LocalHistoryIdentifier applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);
        final String snapshotActorName = "shard-" + memberName.getName() + ':' + "snapshot-read";

        // Create a snapshot actor. This actor will act as a worker to offload snapshot serialization for all
        // requests.
        final ActorRef snapshotActor = actorContext.actorOf(ShardSnapshotActor.props(), snapshotActorName);

        return new ShardSnapshotCohort(applyHistoryId, snapshotActor, store, log, logId);
    }

    @Override
    public void createSnapshot(final ActorRef actorRef) {
        // Forward the request to the snapshot actor
        ShardSnapshotActor.requestSnapshot(snapshotActor, store.takeRecoverySnapshot(), actorRef);
    }

    private void deserializeAndApplySnapshot(final byte[] snapshotBytes) {
        final ShardDataTreeSnapshot snapshot;
        try {
            snapshot = ShardDataTreeSnapshot.deserialize(snapshotBytes);
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

    void syncCommitTransaction(final ReadWriteShardDataTreeTransaction transaction)
            throws ExecutionException, InterruptedException {
        ShardDataTreeCohort commitCohort = store.finishTransaction(transaction);
        commitCohort.preCommit().get();
        commitCohort.commit().get();
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
