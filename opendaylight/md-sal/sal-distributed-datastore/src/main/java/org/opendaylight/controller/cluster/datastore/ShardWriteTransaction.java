/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.modification.Modification;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardWriteTransaction extends ShardTransaction {

    private int totalBatchedModificationsReceived;
    private Exception lastBatchedModificationsException;
    private final ReadWriteShardDataTreeTransaction transaction;

    public ShardWriteTransaction(ReadWriteShardDataTreeTransaction transaction, ActorRef shardActor,
            ShardStats shardStats, String transactionID) {
        super(shardActor, shardStats, transactionID);
        this.transaction = transaction;
    }

    @Override
    protected ReadWriteShardDataTreeTransaction getDOMStoreTransaction() {
        return transaction;
    }

    @Override
    public void handleReceive(Object message) throws Exception {

        if (message instanceof BatchedModifications) {
            batchedModifications((BatchedModifications)message);
        } else {
            super.handleReceive(message);
        }
    }

    private void batchedModifications(BatchedModifications batched) {
        if (checkClosed()) {
            if (batched.isReady()) {
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
            return;
        }

        try {
            for(Modification modification: batched.getModifications()) {
                modification.apply(transaction.getSnapshot());
            }

            totalBatchedModificationsReceived++;
            if(batched.isReady()) {
                if(lastBatchedModificationsException != null) {
                    throw lastBatchedModificationsException;
                }

                if(totalBatchedModificationsReceived != batched.getTotalMessagesSent()) {
                    throw new IllegalStateException(String.format(
                            "The total number of batched messages received %d does not match the number sent %d",
                            totalBatchedModificationsReceived, batched.getTotalMessagesSent()));
                }

                readyTransaction(false, batched.isDoCommitOnReady(), batched.getVersion());
            } else {
                getSender().tell(new BatchedModificationsReply(batched.getModifications().size()), getSelf());
            }
        } catch (Exception e) {
            lastBatchedModificationsException = e;
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());

            if(batched.isReady()) {
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
        }
    }

    protected final void dataExists(DataExists message) {
        super.dataExists(transaction, message);
    }

    protected final void readData(ReadData message) {
        super.readData(transaction, message);
    }

    private boolean checkClosed() {
        if (transaction.isClosed()) {
            getSender().tell(new akka.actor.Status.Failure(new IllegalStateException("Transaction is closed, no modifications allowed")), getSelf());
            return true;
        } else {
            return false;
        }
    }

    private void readyTransaction(boolean returnSerialized, boolean doImmediateCommit, short clientTxVersion) {
        String transactionID = getTransactionID();

        LOG.debug("readyTransaction : {}", transactionID);

        getShardActor().forward(new ForwardedReadyTransaction(transactionID, clientTxVersion,
                transaction, doImmediateCommit), getContext());

        // The shard will handle the commit from here so we're no longer needed - self-destruct.
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }
}
