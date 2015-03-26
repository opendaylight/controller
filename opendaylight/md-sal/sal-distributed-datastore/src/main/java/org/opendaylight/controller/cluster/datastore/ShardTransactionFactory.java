/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.UntypedActorContext;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;

/**
 * A factory for creating ShardTransaction actors.
 *
 * @author Thomas Pantelis
 */
class ShardTransactionActorFactory {

    private final DOMTransactionFactory domTransactionFactory;
    private final DatastoreContext datastoreContext;
    private final String txnDispatcherPath;
    private final ShardStats shardMBean;
    private final UntypedActorContext actorContext;
    private final ActorRef shardActor;

    ShardTransactionActorFactory(DOMTransactionFactory domTransactionFactory, DatastoreContext datastoreContext,
            String txnDispatcherPath, ActorRef shardActor, UntypedActorContext actorContext, ShardStats shardMBean) {
        this.domTransactionFactory = domTransactionFactory;
        this.datastoreContext = datastoreContext;
        this.txnDispatcherPath = txnDispatcherPath;
        this.shardMBean = shardMBean;
        this.actorContext = actorContext;
        this.shardActor = shardActor;
    }

    ActorRef newShardTransaction(TransactionProxy.TransactionType type, ShardTransactionIdentifier transactionID,
            String transactionChainID, short clientVersion) {

        DOMStoreTransaction transaction = domTransactionFactory.newTransaction(type, transactionID.toString(),
                transactionChainID);

        return actorContext.actorOf(ShardTransaction.props(transaction, shardActor, datastoreContext, shardMBean,
                transactionID.getRemoteTransactionId(), clientVersion).withDispatcher(txnDispatcherPath),
                transactionID.toString());
    }
}
