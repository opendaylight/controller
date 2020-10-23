/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.japi.Creator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * The ShardTransaction Actor represents a remote transaction that delegates all actions to DOMDataReadWriteTransaction.
 */
public abstract class ShardTransaction extends AbstractUntypedActorWithMetering {
    private final ActorRef shardActor;
    private final ShardStats shardStats;
    private final TransactionIdentifier transactionId;

    protected ShardTransaction(final ActorRef shardActor, final ShardStats shardStats,
            final TransactionIdentifier transactionId) {
        // actor name override used for metering. This does not change the "real" actor name
        super("shard-tx");
        this.shardActor = shardActor;
        this.shardStats = shardStats;
        this.transactionId = requireNonNull(transactionId);
    }

    public static Props props(final TransactionType type, final AbstractShardDataTreeTransaction<?> transaction,
            final ActorRef shardActor, final DatastoreContext datastoreContext, final ShardStats shardStats) {
        return Props.create(ShardTransaction.class,
            new ShardTransactionCreator(type, transaction, shardActor, datastoreContext, shardStats));
    }

    protected abstract AbstractShardDataTreeTransaction<?> getDOMStoreTransaction();

    protected ActorRef getShardActor() {
        return shardActor;
    }

    protected final TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    @Override
    public void handleReceive(final Object message) {
        if (CloseTransaction.isSerializedType(message)) {
            closeTransaction(true);
        } else if (message instanceof ReceiveTimeout) {
            LOG.debug("Got ReceiveTimeout for inactivity - closing transaction {}", transactionId);
            closeTransaction(false);
        } else {
            unknownMessage(message);
        }
    }

    protected boolean returnCloseTransactionReply() {
        return true;
    }

    private void closeTransaction(final boolean sendReply) {
        getDOMStoreTransaction().abortFromTransactionActor();

        if (sendReply && returnCloseTransactionReply()) {
            getSender().tell(new CloseTransactionReply(), getSelf());
        }

        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    private boolean checkClosed(final AbstractShardDataTreeTransaction<?> transaction) {
        final boolean ret = transaction.isClosed();
        if (ret) {
            shardStats.incrementFailedReadTransactionsCount();
            getSender().tell(new akka.actor.Status.Failure(new ReadFailedException("Transaction is closed")),
                    getSelf());
        }
        return ret;
    }

    protected void readData(final AbstractShardDataTreeTransaction<?> transaction, final ReadData message) {
        if (checkClosed(transaction)) {
            return;
        }

        final YangInstanceIdentifier path = message.getPath();
        ReadDataReply readDataReply = new ReadDataReply(transaction.getSnapshot().readNode(path).orElse(null),
            message.getVersion());
        sender().tell(readDataReply.toSerializable(), self());
    }

    protected void dataExists(final AbstractShardDataTreeTransaction<?> transaction, final DataExists message) {
        if (checkClosed(transaction)) {
            return;
        }

        final YangInstanceIdentifier path = message.getPath();
        boolean exists = transaction.getSnapshot().readNode(path).isPresent();
        getSender().tell(new DataExistsReply(exists, message.getVersion()).toSerializable(), getSelf());
    }

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Some fields are not Serializable but we don't "
            + "create remote instances of this actor and thus don't need it to be Serializable.")
    private static class ShardTransactionCreator implements Creator<ShardTransaction> {

        private static final long serialVersionUID = 1L;

        final AbstractShardDataTreeTransaction<?> transaction;
        final ActorRef shardActor;
        final DatastoreContext datastoreContext;
        final ShardStats shardStats;
        final TransactionType type;

        ShardTransactionCreator(final TransactionType type, final AbstractShardDataTreeTransaction<?> transaction,
                final ActorRef shardActor, final DatastoreContext datastoreContext, final ShardStats shardStats) {
            this.transaction = requireNonNull(transaction);
            this.shardActor = shardActor;
            this.shardStats = shardStats;
            this.datastoreContext = datastoreContext;
            this.type = type;
        }

        @Override
        public ShardTransaction create() {
            final ShardTransaction tx;
            switch (type) {
                case READ_ONLY:
                    tx = new ShardReadTransaction(transaction, shardActor, shardStats);
                    break;
                case READ_WRITE:
                    tx = new ShardReadWriteTransaction((ReadWriteShardDataTreeTransaction)transaction, shardActor,
                            shardStats);
                    break;
                case WRITE_ONLY:
                    tx = new ShardWriteTransaction((ReadWriteShardDataTreeTransaction)transaction, shardActor,
                            shardStats);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled transaction type " + type);
            }

            tx.getContext().setReceiveTimeout(datastoreContext.getShardTransactionIdleTimeout());
            return tx;
        }
    }
}
