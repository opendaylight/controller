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
import java.util.concurrent.ExecutionException;
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
    private final ShardTransactionActorFactory transactionActorFactory;
    private final ShardDataTree store;
    private final String logId;
    private final Logger log;

    private int createSnapshotTransactionCounter;

    ShardSnapshotCohort(ShardTransactionActorFactory transactionActorFactory, ShardDataTree store,
            Logger log, String logId) {
        this.transactionActorFactory = transactionActorFactory;
        this.store = Preconditions.checkNotNull(store);
        this.log = log;
        this.logId = logId;
    }

    @Override
    public void createSnapshot(ActorRef actorRef) {
        // Create a transaction actor. We are really going to treat the transaction as a worker
        // so that this actor does not get block building the snapshot. THe transaction actor will
        // after processing the CreateSnapshot message.

        ActorRef createSnapshotTransaction = transactionActorFactory.newShardTransaction(
                TransactionType.READ_ONLY, "createSnapshot" + ++createSnapshotTransactionCounter, "");

        createSnapshotTransaction.tell(CreateSnapshot.INSTANCE, actorRef);
    }

    @Override
    public void applySnapshot(byte[] snapshotBytes) {
        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        log.info("{}: Applying snapshot", logId);

        try {
            ReadWriteShardDataTreeTransaction transaction = store.newReadWriteTransaction("snapshot-" + logId, null);

            NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);

            // delete everything first
            transaction.getSnapshot().delete(YangInstanceIdentifier.EMPTY);

            // Add everything from the remote node back
            transaction.getSnapshot().write(YangInstanceIdentifier.EMPTY, node);
            syncCommitTransaction(transaction);
        } catch (InterruptedException | ExecutionException e) {
            log.error("{}: An exception occurred when applying snapshot", logId, e);
        } finally {
            log.info("{}: Done applying snapshot", logId);
        }

    }

    void syncCommitTransaction(final ReadWriteShardDataTreeTransaction transaction)
            throws ExecutionException, InterruptedException {
        ShardDataTreeCohort commitCohort = store.finishTransaction(transaction);
        commitCohort.preCommit().get();
        commitCohort.commit().get();
    }
}
