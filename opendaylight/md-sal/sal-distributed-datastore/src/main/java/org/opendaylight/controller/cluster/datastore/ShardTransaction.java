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
import com.google.common.base.Preconditions;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * The ShardTransaction Actor represents a remote transaction
 * <p>
 * The ShardTransaction Actor delegates all actions to DOMDataReadWriteTransaction
 * </p>
 * <p>
 * Handles Messages <br/>
 * ---------------- <br/>
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.ReadData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.CloseTransaction}
 * </p>
 */
public abstract class ShardTransaction extends AbstractUntypedActorWithMetering {
    private final ActorRef shardActor;
    private final ShardStats shardStats;
    private final String transactionID;

    protected ShardTransaction(ActorRef shardActor, ShardStats shardStats, String transactionID) {
        super("shard-tx"); //actor name override used for metering. This does not change the "real" actor name
        this.shardActor = shardActor;
        this.shardStats = shardStats;
        this.transactionID = Preconditions.checkNotNull(transactionID);
    }

    public static Props props(TransactionType type, AbstractShardDataTreeTransaction<?> transaction, ActorRef shardActor,
            DatastoreContext datastoreContext, ShardStats shardStats, String transactionID) {
        return Props.create(new ShardTransactionCreator(type, transaction, shardActor,
           datastoreContext, shardStats, transactionID));
    }

    protected abstract AbstractShardDataTreeTransaction<?> getDOMStoreTransaction();

    protected ActorRef getShardActor() {
        return shardActor;
    }

    protected String getTransactionID() {
        return transactionID;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (CloseTransaction.isSerializedType(message)) {
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

    protected boolean returnCloseTransactionReply() {
        return true;
    }

    private void closeTransaction(boolean sendReply) {
        getDOMStoreTransaction().abort();

        if(sendReply && returnCloseTransactionReply()) {
            getSender().tell(new CloseTransactionReply(), getSelf());
        }

        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    private boolean checkClosed(AbstractShardDataTreeTransaction<?> transaction) {
        final boolean ret = transaction.isClosed();
        if (ret) {
            shardStats.incrementFailedReadTransactionsCount();
            getSender().tell(new akka.actor.Status.Failure(new ReadFailedException("Transaction is closed")), getSelf());
        }
        return ret;
    }

    protected void readData(AbstractShardDataTreeTransaction<?> transaction, ReadData message) {
        if (checkClosed(transaction)) {
            return;
        }

        final YangInstanceIdentifier path = message.getPath();
        Optional<NormalizedNode<?, ?>> optional = transaction.getSnapshot().readNode(path);
        ReadDataReply readDataReply = new ReadDataReply(optional.orNull(), message.getVersion());
        sender().tell(readDataReply.toSerializable(), self());
    }

    protected void dataExists(AbstractShardDataTreeTransaction<?> transaction, DataExists message) {
        if (checkClosed(transaction)) {
            return;
        }

        final YangInstanceIdentifier path = message.getPath();
        boolean exists = transaction.getSnapshot().readNode(path).isPresent();
        getSender().tell(new DataExistsReply(exists, message.getVersion()).toSerializable(), getSelf());
    }

    private static class ShardTransactionCreator implements Creator<ShardTransaction> {

        private static final long serialVersionUID = 1L;

        final AbstractShardDataTreeTransaction<?> transaction;
        final ActorRef shardActor;
        final DatastoreContext datastoreContext;
        final ShardStats shardStats;
        final String transactionID;
        final TransactionType type;

        ShardTransactionCreator(TransactionType type, AbstractShardDataTreeTransaction<?> transaction, ActorRef shardActor,
                DatastoreContext datastoreContext, ShardStats shardStats, String transactionID) {
            this.transaction = Preconditions.checkNotNull(transaction);
            this.shardActor = shardActor;
            this.shardStats = shardStats;
            this.datastoreContext = datastoreContext;
            this.transactionID = Preconditions.checkNotNull(transactionID);
            this.type = type;
        }

        @Override
        public ShardTransaction create() throws Exception {
            final ShardTransaction tx;
            switch (type) {
            case READ_ONLY:
                tx = new ShardReadTransaction(transaction, shardActor,
                    shardStats, transactionID);
                break;
            case READ_WRITE:
                tx = new ShardReadWriteTransaction((ReadWriteShardDataTreeTransaction)transaction,
                    shardActor, shardStats, transactionID);
                break;
            case WRITE_ONLY:
                tx = new ShardWriteTransaction((ReadWriteShardDataTreeTransaction)transaction,
                    shardActor, shardStats, transactionID);
                break;
            default:
                throw new IllegalArgumentException("Unhandled transaction type " + type);
            }

            tx.getContext().setReceiveTimeout(datastoreContext.getShardTransactionIdleTimeout());
            return tx;
        }
    }
}
