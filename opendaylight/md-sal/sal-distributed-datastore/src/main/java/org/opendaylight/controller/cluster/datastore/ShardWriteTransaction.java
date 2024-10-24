/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.modification.Modification;

/**
 * Actor for a shard write-only transaction.
 *
 * @author syedbahm
 */
@Deprecated(since = "9.0.0", forRemoval = true)
public class ShardWriteTransaction extends ShardTransaction {
    private int totalBatchedModificationsReceived;
    private Exception lastBatchedModificationsException;
    private final ReadWriteShardDataTreeTransaction transaction;

    public ShardWriteTransaction(final ReadWriteShardDataTreeTransaction transaction, final ActorRef shardActor,
            final ShardStats shardStats) {
        super(shardActor, shardStats, transaction.getIdentifier());
        this.transaction = transaction;
    }

    @Override
    protected ReadWriteShardDataTreeTransaction getDOMStoreTransaction() {
        return transaction;
    }

    @Override
    public void handleReceive(final Object message) {
        if (message instanceof BatchedModifications) {
            batchedModifications((BatchedModifications)message);
        } else {
            super.handleReceive(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void batchedModifications(final BatchedModifications batched) {
        if (checkClosed()) {
            if (batched.isReady()) {
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
            return;
        }

        try {
            for (Modification modification: batched.getModifications()) {
                modification.apply(transaction.getSnapshot());
            }

            totalBatchedModificationsReceived++;
            if (batched.isReady()) {
                if (lastBatchedModificationsException != null) {
                    throw lastBatchedModificationsException;
                }

                if (totalBatchedModificationsReceived != batched.getTotalMessagesSent()) {
                    throw new IllegalStateException(String.format(
                            "The total number of batched messages received %d does not match the number sent %d",
                            totalBatchedModificationsReceived, batched.getTotalMessagesSent()));
                }

                readyTransaction(batched);
            } else {
                getSender().tell(new BatchedModificationsReply(batched.getModifications().size()), getSelf());
            }
        } catch (Exception e) {
            lastBatchedModificationsException = e;
            getSender().tell(new org.apache.pekko.actor.Status.Failure(e), getSelf());

            if (batched.isReady()) {
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
        }
    }

    protected final void dataExists(final DataExists message) {
        super.dataExists(transaction, message);
    }

    protected final void readData(final ReadData message) {
        super.readData(transaction, message);
    }

    private boolean checkClosed() {
        final boolean ret = transaction.isClosed();
        if (ret) {
            getSender().tell(new org.apache.pekko.actor.Status.Failure(new IllegalStateException(
                    "Transaction is closed, no modifications allowed")), getSelf());
        }
        return ret;
    }

    private void readyTransaction(final BatchedModifications batched) {
        TransactionIdentifier transactionID = getTransactionId();

        LOG.debug("readyTransaction : {}", transactionID);

        getShardActor().forward(new ForwardedReadyTransaction(transactionID, batched.getVersion(),
                transaction, batched.isDoCommitOnReady(), batched.getParticipatingShardNames()), getContext());

        // The shard will handle the commit from here so we're no longer needed - self-destruct.
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }
}
