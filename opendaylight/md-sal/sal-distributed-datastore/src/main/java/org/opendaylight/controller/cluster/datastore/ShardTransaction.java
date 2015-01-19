/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.japi.Creator;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * The ShardTransaction Actor represents a remote transaction
 * <p>
 * The ShardTransaction Actor delegates all actions to DOMDataReadWriteTransaction
 * </p>
 * <p>
 * Even though the DOMStore and the DOMStoreTransactionChain implement multiple types of transactions
 * the ShardTransaction Actor only works with read-write transactions. This is just to keep the logic simple. At this
 * time there are no known advantages for creating a read-only or write-only transaction which may change over time
 * at which point we can optimize things in the distributed store as well.
 * </p>
 * <p>
 * Handles Messages <br/>
 * ---------------- <br/>
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.ReadData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.WriteData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.MergeData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.DeleteData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.CloseTransaction}
 * </p>
 */
public abstract class ShardTransaction extends AbstractUntypedActorWithMetering {

    protected static final boolean SERIALIZED_REPLY = true;

    private final ActorRef shardActor;
    private final SchemaContext schemaContext;
    private final ShardStats shardStats;
    private final String transactionID;
    private final short clientTxVersion;

    protected ShardTransaction(ActorRef shardActor, SchemaContext schemaContext,
            ShardStats shardStats, String transactionID, short clientTxVersion) {
        super("shard-tx"); //actor name override used for metering. This does not change the "real" actor name
        this.shardActor = shardActor;
        this.schemaContext = schemaContext;
        this.shardStats = shardStats;
        this.transactionID = transactionID;
        this.clientTxVersion = clientTxVersion;
    }

    public static Props props(DOMStoreTransaction transaction, ActorRef shardActor,
            SchemaContext schemaContext,DatastoreContext datastoreContext, ShardStats shardStats,
            String transactionID, short txnClientVersion) {
        return Props.create(new ShardTransactionCreator(transaction, shardActor, schemaContext,
           datastoreContext, shardStats, transactionID, txnClientVersion));
    }

    protected abstract DOMStoreTransaction getDOMStoreTransaction();

    protected ActorRef getShardActor() {
        return shardActor;
    }

    protected String getTransactionID() {
        return transactionID;
    }

    protected SchemaContext getSchemaContext() {
        return schemaContext;
    }

    protected short getClientTxVersion() {
        return clientTxVersion;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message.getClass().equals(CloseTransaction.SERIALIZABLE_CLASS)) {
            closeTransaction(true);
        } else if (message instanceof ReceiveTimeout) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Got ReceiveTimeout for inactivity - closing Tx");
            }
            closeTransaction(false);
        } else {
            throw new UnknownMessageException(message);
        }
    }

    private void closeTransaction(boolean sendReply) {
        getDOMStoreTransaction().close();

        if(sendReply) {
            getSender().tell(CloseTransactionReply.INSTANCE.toSerializable(), getSelf());
        }

        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    protected void readData(DOMStoreReadTransaction transaction, ReadData message,
            final boolean returnSerialized) {
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        final YangInstanceIdentifier path = message.getPath();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                transaction.read(path);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<NormalizedNode<?, ?>> optional = future.checkedGet();
                    ReadDataReply readDataReply = new ReadDataReply(optional.orNull());

                    sender.tell((returnSerialized ? readDataReply.toSerializable(clientTxVersion):
                        readDataReply), self);

                } catch (Exception e) {
                    shardStats.incrementFailedReadTransactionsCount();
                    sender.tell(new akka.actor.Status.Failure(e), self);
                }

            }
        }, getContext().dispatcher());
    }

    protected void dataExists(DOMStoreReadTransaction transaction, DataExists message,
        final boolean returnSerialized) {
        final YangInstanceIdentifier path = message.getPath();

        try {
            Boolean exists = transaction.exists(path).checkedGet();
            DataExistsReply dataExistsReply = new DataExistsReply(exists);
            getSender().tell(returnSerialized ? dataExistsReply.toSerializable() :
                dataExistsReply, getSelf());
        } catch (ReadFailedException e) {
            getSender().tell(new akka.actor.Status.Failure(e),getSelf());
        }

    }

    private static class ShardTransactionCreator implements Creator<ShardTransaction> {

        private static final long serialVersionUID = 1L;

        final DOMStoreTransaction transaction;
        final ActorRef shardActor;
        final SchemaContext schemaContext;
        final DatastoreContext datastoreContext;
        final ShardStats shardStats;
        final String transactionID;
        final short txnClientVersion;

        ShardTransactionCreator(DOMStoreTransaction transaction, ActorRef shardActor,
                SchemaContext schemaContext, DatastoreContext datastoreContext,
                ShardStats shardStats, String transactionID, short txnClientVersion) {
            this.transaction = transaction;
            this.shardActor = shardActor;
            this.shardStats = shardStats;
            this.schemaContext = schemaContext;
            this.datastoreContext = datastoreContext;
            this.transactionID = transactionID;
            this.txnClientVersion = txnClientVersion;
        }

        @Override
        public ShardTransaction create() throws Exception {
            ShardTransaction tx;
            if(transaction instanceof DOMStoreReadWriteTransaction) {
                tx = new ShardReadWriteTransaction((DOMStoreReadWriteTransaction)transaction,
                        shardActor, schemaContext, shardStats, transactionID, txnClientVersion);
            } else if(transaction instanceof DOMStoreReadTransaction) {
                tx = new ShardReadTransaction((DOMStoreReadTransaction)transaction, shardActor,
                        schemaContext, shardStats, transactionID, txnClientVersion);
            } else {
                tx = new ShardWriteTransaction((DOMStoreWriteTransaction)transaction,
                        shardActor, schemaContext, shardStats, transactionID, txnClientVersion);
            }

            tx.getContext().setReceiveTimeout(datastoreContext.getShardTransactionIdleTimeout());
            return tx;
        }
    }
}
