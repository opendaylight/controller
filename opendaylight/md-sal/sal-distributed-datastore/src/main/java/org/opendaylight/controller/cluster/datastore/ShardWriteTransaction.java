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
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
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
            ShardStats shardStats, String transactionID, short clientTxVersion) {
        super(shardActor, shardStats, transactionID, clientTxVersion);
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
        } else if (message instanceof ReadyTransaction) {
            readyTransaction(!SERIALIZED_REPLY, false);
        } else if(ReadyTransaction.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readyTransaction(SERIALIZED_REPLY, false);
        } else if(WriteData.isSerializedType(message)) {
            writeData(WriteData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(MergeData.isSerializedType(message)) {
            mergeData(MergeData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(DeleteData.isSerializedType(message)) {
            deleteData(DeleteData.fromSerializable(message), SERIALIZED_REPLY);
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

                readyTransaction(false, batched.isDoCommitOnReady());
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

    protected final void dataExists(DataExists message, final boolean returnSerialized) {
        super.dataExists(transaction, message, returnSerialized);
    }

    protected final void readData(ReadData message, final boolean returnSerialized) {
        super.readData(transaction, message, returnSerialized);
    }

    private boolean checkClosed() {
        if (transaction.isClosed()) {
            getSender().tell(new akka.actor.Status.Failure(new IllegalStateException("Transaction is closed, no modifications allowed")), getSelf());
            return true;
        } else {
            return false;
        }
    }

    private void writeData(WriteData message, boolean returnSerialized) {
        LOG.debug("writeData at path : {}", message.getPath());
        if (checkClosed()) {
            return;
        }

        try {
            transaction.getSnapshot().write(message.getPath(), message.getData());
            WriteDataReply writeDataReply = WriteDataReply.INSTANCE;
            getSender().tell(returnSerialized ? writeDataReply.toSerializable(message.getVersion()) :
                writeDataReply, getSelf());
        }catch(Exception e){
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void mergeData(MergeData message, boolean returnSerialized) {
        LOG.debug("mergeData at path : {}", message.getPath());
        if (checkClosed()) {
            return;
        }

        try {
            transaction.getSnapshot().merge(message.getPath(), message.getData());
            MergeDataReply mergeDataReply = MergeDataReply.INSTANCE;
            getSender().tell(returnSerialized ? mergeDataReply.toSerializable(message.getVersion()) :
                mergeDataReply, getSelf());
        }catch(Exception e){
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void deleteData(DeleteData message, boolean returnSerialized) {
        LOG.debug("deleteData at path : {}", message.getPath());
        if (checkClosed()) {
            return;
        }

        try {
            transaction.getSnapshot().delete(message.getPath());
            DeleteDataReply deleteDataReply = DeleteDataReply.INSTANCE;
            getSender().tell(returnSerialized ? deleteDataReply.toSerializable(message.getVersion()) :
                deleteDataReply, getSelf());
        } catch(Exception e) {
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void readyTransaction(boolean returnSerialized, boolean doImmediateCommit) {
        String transactionID = getTransactionID();

        LOG.debug("readyTransaction : {}", transactionID);

        ShardDataTreeCohort cohort =  transaction.ready();

        getShardActor().forward(new ForwardedReadyTransaction(transactionID, getClientTxVersion(),
                cohort, returnSerialized, doImmediateCommit), getContext());

        // The shard will handle the commit from here so we're no longer needed - self-destruct.
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }
}
