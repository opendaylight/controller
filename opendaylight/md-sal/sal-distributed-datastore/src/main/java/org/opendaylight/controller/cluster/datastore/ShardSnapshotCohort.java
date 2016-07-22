/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
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
    private static final FrontendType SNAPSHOT_READ = FrontendType.forName("snapshot-read");

    private final ShardTransactionActorFactory transactionActorFactory;
    private final LocalHistoryIdentifier applyHistoryId;
    private final LocalHistoryIdentifier readHistoryId;
    private final ShardDataTree store;
    private final String logId;
    private final Logger log;

    private long applyCounter;
    private long readCounter;

    ShardSnapshotCohort(final MemberName memberName, final ShardTransactionActorFactory transactionActorFactory, final ShardDataTree store,
            final Logger log, final String logId) {
        this.transactionActorFactory = Preconditions.checkNotNull(transactionActorFactory);
        this.store = Preconditions.checkNotNull(store);
        this.log = log;
        this.logId = logId;

        this.applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);
        this.readHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_READ), 0), 0);
    }

    @Override
    public void createSnapshot(final ActorRef actorRef) {
        // Create a transaction actor. We are really going to treat the transaction as a worker
        // so that this actor does not get block building the snapshot. THe transaction actor will
        // after processing the CreateSnapshot message.

        ActorRef createSnapshotTransaction = transactionActorFactory.newShardTransaction(
                TransactionType.READ_ONLY, new TransactionIdentifier(readHistoryId, readCounter++));

        createSnapshotTransaction.tell(CreateSnapshot.INSTANCE, actorRef);
    }

    @Override
    public void applySnapshot(final byte[] snapshotBytes) {
        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        log.info("{}: Applying snapshot", logId);

        try {
            ReadWriteShardDataTreeTransaction transaction = store.newReadWriteTransaction(
                new TransactionIdentifier(applyHistoryId, applyCounter++));

            NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);

            // delete everything first
            transaction.getSnapshot().delete(YangInstanceIdentifier.EMPTY);

            // Add everything from the remote node back
            transaction.getSnapshot().write(YangInstanceIdentifier.EMPTY, node);
            syncCommitTransaction(transaction);
        } catch (Exception e) {
            log.error("{}: An exception occurred when applying snapshot", logId, e);
        } finally {
            log.info("{}: Done applying snapshot", logId);
        }

    }

    void syncCommitTransaction(final ReadWriteShardDataTreeTransaction transaction) throws Exception {
        ShardDataTreeCohort commitCohort = store.finishTransaction(transaction);
        commitCohort.preCommit();
        commitCohort.commit();
    }
}
