/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * The ShardTransactionChain Actor represents a remote TransactionChain
 */
public class ShardTransactionChain extends AbstractUntypedActor {

    private final DOMStoreTransactionChain chain;
    private final SchemaContext schemaContext;

    public ShardTransactionChain(DOMStoreTransactionChain chain, SchemaContext schemaContext) {
        this.chain = chain;
        this.schemaContext = schemaContext;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message instanceof CreateTransaction) {
            CreateTransaction createTransaction = (CreateTransaction) message;
            createTransaction(createTransaction);
        } else if (message instanceof CloseTransactionChain) {
            chain.close();
            getSender().tell(new CloseTransactionChainReply(), getSelf());
        }
    }

    private void createTransaction(CreateTransaction createTransaction) {
        DOMStoreReadWriteTransaction transaction =
            chain.newReadWriteTransaction();
        ActorRef transactionActor = getContext().actorOf(ShardTransaction
            .props(chain, transaction, getContext().parent(), schemaContext), "shard-" + createTransaction.getTransactionId());
        getSender()
            .tell(ShardTransactionMessages.CreateTransactionReply.newBuilder()
                .setTransactionActorPath(transactionActor.path().toString())
                .setTransactionId(createTransaction.getTransactionId())
                .build(),
                getSelf());
    }

    public static Props props(final DOMStoreTransactionChain chain, final SchemaContext schemaContext) {
        return Props.create(new Creator<ShardTransactionChain>() {

            @Override
            public ShardTransactionChain create() throws Exception {
                return new ShardTransactionChain(chain, schemaContext);
            }
        });
    }
}
