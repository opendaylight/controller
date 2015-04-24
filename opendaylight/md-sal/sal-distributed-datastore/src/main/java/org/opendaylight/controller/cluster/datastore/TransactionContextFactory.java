/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ShardInfoListenerRegistration;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * An {@link AbstractTransactionContextFactory} which produces TransactionContext instances for single
 * transactions (ie not chained).
 */
final class TransactionContextFactory extends AbstractTransactionContextFactory<LocalTransactionFactoryImpl> {

    @GuardedBy("childChains")
    private final Collection<TransactionChainProxy> childChains = new ArrayList<>();

    private final ShardInfoListenerRegistration<TransactionContextFactory> reg;

    private TransactionContextFactory(final ActorContext actorContext) {
        super(actorContext);
        this.reg = actorContext.registerShardInfoListener(this);
    }

    static TransactionContextFactory create(final ActorContext actorContext) {
        return new TransactionContextFactory(actorContext);
    }

    @Override
    public void close() {
        reg.close();
    }

    @Override
    protected TransactionIdentifier nextIdentifier() {
        return TransactionIdentifier.create(getMemberName(), TX_COUNTER.getAndIncrement(), null);
    }

    @Override
    protected LocalTransactionFactoryImpl factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
        return new LocalTransactionFactoryImpl(getActorContext(), shardLeader, dataTree);
    }

    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(final String shardName) {
        return getActorContext().findPrimaryShardAsync(shardName);
    }

    @Override
    protected <T> void onTransactionReady(final TransactionIdentifier transaction, final Collection<Future<T>> cohortFutures) {
        // Transactions are disconnected, this is a no-op
    }

    DOMStoreTransactionChain createTransactionChain() {
        final TransactionChainProxy ret = new TransactionChainProxy(this);

        synchronized (childChains) {
            childChains.add(ret);
        }

        return ret;
    }

    void removeTransactionChain(final TransactionChainProxy chain) {
        synchronized (childChains) {
            childChains.remove(chain);
        }
    }

    @Override
    public void onShardInfoUpdated(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        synchronized (childChains) {
            for (TransactionChainProxy chain : childChains) {
                chain.onShardInfoUpdated(shardName, primaryShardInfo);
            }
            super.onShardInfoUpdated(shardName, primaryShardInfo);
        }
    }

    @Override
    protected DataTree dataTreeForFactory(final LocalTransactionFactoryImpl factory) {
        return factory.getDataTree();
    }
}
