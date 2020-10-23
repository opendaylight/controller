/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.AbstractActor.ActorContext;
import akka.actor.ActorRef;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A factory for creating ShardTransaction actors.
 *
 * @author Thomas Pantelis
 */
class ShardTransactionActorFactory {
    private static final AtomicLong ACTOR_NAME_COUNTER = new AtomicLong();

    private final ShardDataTree dataTree;
    private final DatastoreContext datastoreContext;
    private final String txnDispatcherPath;
    private final ShardStats shardMBean;
    private final ActorContext actorContext;
    private final ActorRef shardActor;
    private final String shardName;

    ShardTransactionActorFactory(ShardDataTree dataTree, DatastoreContext datastoreContext,
            String txnDispatcherPath, ActorRef shardActor, ActorContext actorContext, ShardStats shardMBean,
            String shardName) {
        this.dataTree = requireNonNull(dataTree);
        this.datastoreContext = requireNonNull(datastoreContext);
        this.txnDispatcherPath = requireNonNull(txnDispatcherPath);
        this.shardMBean = requireNonNull(shardMBean);
        this.actorContext = requireNonNull(actorContext);
        this.shardActor = requireNonNull(shardActor);
        this.shardName = requireNonNull(shardName);
    }

    private String actorNameFor(final TransactionIdentifier txId) {
        final LocalHistoryIdentifier historyId = txId.getHistoryId();
        final ClientIdentifier clientId = historyId.getClientId();
        final FrontendIdentifier frontendId = clientId.getFrontendId();

        final StringBuilder sb = new StringBuilder("shard-");
        sb.append(shardName).append('-')
            .append(frontendId.getMemberName().getName()).append(':')
            .append(frontendId.getClientType().getName()).append('@')
            .append(clientId.getGeneration()).append(':');
        if (historyId.getHistoryId() != 0) {
            sb.append(historyId.getHistoryId()).append('-');
        }

        return sb.append(txId.getTransactionId()).append('_').append(ACTOR_NAME_COUNTER.incrementAndGet()).toString();
    }

    ActorRef newShardTransaction(TransactionType type, TransactionIdentifier transactionID) {
        final AbstractShardDataTreeTransaction<?> transaction;
        switch (type) {
            case READ_ONLY:
                transaction = dataTree.newReadOnlyTransaction(transactionID);
                break;
            case READ_WRITE:
            case WRITE_ONLY:
                transaction = dataTree.newReadWriteTransaction(transactionID);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transaction type " + type);
        }

        return actorContext.actorOf(ShardTransaction.props(type, transaction, shardActor, datastoreContext, shardMBean)
            .withDispatcher(txnDispatcherPath), actorNameFor(transactionID));
    }
}
