/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * The ShardTransactionChain Actor represents a remote TransactionChain
 */
public class ShardTransactionChain extends AbstractUntypedActor {

    private final ShardDataTreeTransactionChain chain;
    private final DatastoreContext datastoreContext;
    private final ShardStats shardStats;

    public ShardTransactionChain(ShardDataTreeTransactionChain chain, DatastoreContext datastoreContext,
            ShardStats shardStats) {
        this.chain = Preconditions.checkNotNull(chain);
        this.datastoreContext = datastoreContext;
        this.shardStats = shardStats;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message.getClass().equals(CreateTransaction.SERIALIZABLE_CLASS)) {
            CreateTransaction createTransaction = CreateTransaction.fromSerializable( message);
            createTransaction(createTransaction);
        } else if (message.getClass().equals(CloseTransactionChain.SERIALIZABLE_CLASS)) {
            chain.close();
            getSender().tell(CloseTransactionChainReply.INSTANCE.toSerializable(), getSelf());
        }else{
            unknownMessage(message);
        }
    }

    private ActorRef getShardActor(){
        return getContext().parent();
    }

    private ActorRef createTypedTransactionActor(CreateTransaction createTransaction) {
        String transactionName = "shard-" + createTransaction.getTransactionId();

        final TransactionType type = TransactionType.fromInt(createTransaction.getTransactionType());
        final AbstractShardDataTreeTransaction<?> transaction;
        switch (type) {
        case READ_ONLY:
            transaction = chain.newReadOnlyTransaction(transactionName);
            break;
        case READ_WRITE:
        case WRITE_ONLY:
            transaction = chain.newReadWriteTransaction(transactionName);
            break;
        default:
            throw new IllegalArgumentException("Unhandled transaction type " + type);
        }

        return getContext().actorOf(
            ShardTransaction.props(type, transaction, getShardActor(),
                    datastoreContext, shardStats, createTransaction.getTransactionId(),
                    createTransaction.getVersion()), transactionName);
    }

    private void createTransaction(CreateTransaction createTransaction) {

        ActorRef transactionActor = createTypedTransactionActor(createTransaction);
        getSender().tell(new CreateTransactionReply(transactionActor.path().toString(),
                createTransaction.getTransactionId()).toSerializable(), getSelf());
    }

    public static Props props(ShardDataTreeTransactionChain chain, SchemaContext schemaContext,
        DatastoreContext datastoreContext, ShardStats shardStats) {
        return Props.create(new ShardTransactionChainCreator(chain, datastoreContext, shardStats));
    }

    private static class ShardTransactionChainCreator implements Creator<ShardTransactionChain> {
        private static final long serialVersionUID = 1L;

        final ShardDataTreeTransactionChain chain;
        final DatastoreContext datastoreContext;
        final ShardStats shardStats;

        ShardTransactionChainCreator(ShardDataTreeTransactionChain chain, DatastoreContext datastoreContext,
                ShardStats shardStats) {
            this.chain = chain;
            this.datastoreContext = datastoreContext;
            this.shardStats = shardStats;
        }

        @Override
        public ShardTransactionChain create() throws Exception {
            return new ShardTransactionChain(chain, datastoreContext, shardStats);
        }
    }
}
