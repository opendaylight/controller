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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;

/**
 * A factory for creating ShardTransaction actors.
 *
 * @author Thomas Pantelis
 */
class ShardTransactionActorFactory {

    private final ShardDataTree dataTree;
    private final DatastoreContext datastoreContext;
    private final String txnDispatcherPath;
    private final ShardStats shardMBean;
    private final UntypedActorContext actorContext;
    private final ActorRef shardActor;

    ShardTransactionActorFactory(ShardDataTree dataTree, DatastoreContext datastoreContext,
            String txnDispatcherPath, ActorRef shardActor, UntypedActorContext actorContext, ShardStats shardMBean) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.datastoreContext = datastoreContext;
        this.txnDispatcherPath = txnDispatcherPath;
        this.shardMBean = shardMBean;
        this.actorContext = actorContext;
        this.shardActor = shardActor;
    }

    private static String actorNameFor(final TransactionIdentifier<?> txId) {
        final LocalHistoryIdentifier<?> historyId = txId.getHistoryId();
        final ClientIdentifier<?> clientId = historyId.getClienId();
        final FrontendIdentifier<?> frontendId = clientId.getFrontendId();

        final StringBuilder sb = new StringBuilder("shard-");
        sb.append(frontendId.getMemberName().getName()).append(':');
        sb.append(frontendId.getClientType().toSimpleString()).append('@');
        sb.append(clientId.getGeneration()).append(':');
        if (historyId.getHistoryId() != 0) {
            sb.append(historyId.getHistoryId()).append('-');
        }

        return sb.append(txId.getTransactionId()).toString();
    }

    ActorRef newShardTransaction(TransactionType type, TransactionIdentifier<?> transactionID) {
        final AbstractShardDataTreeTransaction<?> transaction;
        switch (type) {
        case READ_ONLY:
            transaction = dataTree.newReadOnlyTransaction(transactionID);
            shardMBean.incrementReadOnlyTransactionCount();
            break;
        case READ_WRITE:
            transaction = dataTree.newReadWriteTransaction(transactionID);
            shardMBean.incrementReadWriteTransactionCount();
            break;
        case WRITE_ONLY:
            transaction = dataTree.newReadWriteTransaction(transactionID);
            shardMBean.incrementWriteOnlyTransactionCount();
            break;
        default:
            throw new IllegalArgumentException("Unsupported transaction type " + type);
        }

        return actorContext.actorOf(ShardTransaction.props(type, transaction, shardActor, datastoreContext, shardMBean)
            .withDispatcher(txnDispatcherPath), actorNameFor(transactionID));
    }
}
