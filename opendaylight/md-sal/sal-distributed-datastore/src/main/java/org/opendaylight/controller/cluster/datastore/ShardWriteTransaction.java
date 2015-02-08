/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardWriteTransaction extends ShardTransaction {

    private final MutableCompositeModification compositeModification = new MutableCompositeModification();
    private final DOMStoreWriteTransaction transaction;

    public ShardWriteTransaction(DOMStoreWriteTransaction transaction, ActorRef shardActor,
            SchemaContext schemaContext, ShardStats shardStats, String transactionID,
            short clientTxVersion) {
        super(shardActor, schemaContext, shardStats, transactionID, clientTxVersion);
        this.transaction = transaction;
    }

    @Override
    protected DOMStoreTransaction getDOMStoreTransaction() {
        return transaction;
    }

    @Override
    public void handleReceive(Object message) throws Exception {

        if (message instanceof BatchedModifications) {
            batchedModifications((BatchedModifications)message);
        } else if (message instanceof ReadyTransaction) {
            readyTransaction(transaction, !SERIALIZED_REPLY);
        } else if(ReadyTransaction.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readyTransaction(transaction, SERIALIZED_REPLY);
        } else if(WriteData.isSerializedType(message)) {
            writeData(transaction, WriteData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(MergeData.isSerializedType(message)) {
            mergeData(transaction, MergeData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(DeleteData.isSerializedType(message)) {
            deleteData(transaction, DeleteData.fromSerializable(message), SERIALIZED_REPLY);

        } else if (message instanceof GetCompositedModification) {
            // This is here for testing only
            getSender().tell(new GetCompositeModificationReply(compositeModification), getSelf());
        } else {
            super.handleReceive(message);
        }
    }

    private void batchedModifications(BatchedModifications batched) {
        try {
            for(Modification modification: batched.getModifications()) {
                compositeModification.addModification(modification);
                modification.apply(transaction);
            }

            getSender().tell(BatchedModificationsReply.INSTANCE, getSelf());
        } catch (Exception e) {
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void writeData(DOMStoreWriteTransaction transaction, WriteData message,
            boolean returnSerialized) {
        LOG.debug("writeData at path : {}", message.getPath());

        compositeModification.addModification(
                new WriteModification(message.getPath(), message.getData()));
        try {
            transaction.write(message.getPath(), message.getData());
            WriteDataReply writeDataReply = WriteDataReply.INSTANCE;
            getSender().tell(returnSerialized ? writeDataReply.toSerializable(message.getVersion()) :
                writeDataReply, getSelf());
        }catch(Exception e){
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void mergeData(DOMStoreWriteTransaction transaction, MergeData message,
            boolean returnSerialized) {
        LOG.debug("mergeData at path : {}", message.getPath());

        compositeModification.addModification(
                new MergeModification(message.getPath(), message.getData()));

        try {
            transaction.merge(message.getPath(), message.getData());
            MergeDataReply mergeDataReply = MergeDataReply.INSTANCE;
            getSender().tell(returnSerialized ? mergeDataReply.toSerializable(message.getVersion()) :
                mergeDataReply, getSelf());
        }catch(Exception e){
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void deleteData(DOMStoreWriteTransaction transaction, DeleteData message,
            boolean returnSerialized) {
        LOG.debug("deleteData at path : {}", message.getPath());

        compositeModification.addModification(new DeleteModification(message.getPath()));
        try {
            transaction.delete(message.getPath());
            DeleteDataReply deleteDataReply = DeleteDataReply.INSTANCE;
            getSender().tell(returnSerialized ? deleteDataReply.toSerializable(message.getVersion()) :
                deleteDataReply, getSelf());
        }catch(Exception e){
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void readyTransaction(DOMStoreWriteTransaction transaction, boolean returnSerialized) {
        String transactionID = getTransactionID();

        LOG.debug("readyTransaction : {}", transactionID);

        DOMStoreThreePhaseCommitCohort cohort =  transaction.ready();

        getShardActor().forward(new ForwardedReadyTransaction(transactionID, getClientTxVersion(),
                cohort, compositeModification, returnSerialized), getContext());

        // The shard will handle the commit from here so we're no longer needed - self-destruct.
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    // These classes are in here for test purposes only

    static class GetCompositedModification {
    }

    static class GetCompositeModificationReply {
        private final CompositeModification modification;


        GetCompositeModificationReply(CompositeModification modification) {
            this.modification = modification;
        }

        public CompositeModification getModification() {
            return modification;
        }
    }
}
