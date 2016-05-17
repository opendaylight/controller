/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * An {@link AbstractTransactionContextFactory} which produces TransactionContext instances for single
 * transactions (ie not chained).
 */
final class TransactionContextFactory extends AbstractTransactionContextFactory<LocalTransactionFactoryImpl> {
    private final AtomicLong nextHistory = new AtomicLong(1);

    TransactionContextFactory(final ActorContext actorContext, final ClientIdentifier clientId) {
        super(actorContext, new LocalHistoryIdentifier(clientId, 0));
    }

    @Override
    public void close() {
    }

    @Override
    protected LocalTransactionFactoryImpl factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
        return new LocalTransactionFactoryImpl(getActorContext(), shardLeader, dataTree);
    }

    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(final String shardName, TransactionIdentifier txId) {
        return getActorContext().findPrimaryShardAsync(shardName);
    }

    @Override
    protected <T> void onTransactionReady(final TransactionIdentifier transaction, final Collection<Future<T>> cohortFutures) {
        // Transactions are disconnected, this is a no-op
    }

    DOMStoreTransactionChain createTransactionChain() {
        return new TransactionChainProxy(this, new LocalHistoryIdentifier(getHistoryId().getClientId(),
                nextHistory.getAndIncrement()));
    }

    @Override
    protected void onTransactionContextCreated(final TransactionIdentifier transactionId) {
    }
}
