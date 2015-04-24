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
 * An {@link AbstractTransactionComponentFactory} which produces independent components.
 */
// TODO: this should be integrated into DistributedDataStore
final class SingleTransactionComponentFactory extends AbstractTransactionComponentFactory<SingleLocalTransactionFactory>
        implements AutoCloseable {

    @GuardedBy("this")
    private final Collection<ShardedTransactionChain> childChains = new ArrayList<>();
    private final ShardInfoListenerRegistration<SingleTransactionComponentFactory> reg;

    private SingleTransactionComponentFactory(final ActorContext actorContext) {
        super(actorContext);
        this.reg = actorContext.registerShardInfoListener(this);
    }

    static SingleTransactionComponentFactory create(final ActorContext actorContext) {
        return new SingleTransactionComponentFactory(actorContext);
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
    protected SingleLocalTransactionFactory factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
        return new SingleLocalTransactionFactory(getActorContext(), shardLeader, dataTree);
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
        final ShardedTransactionChain ret = new ShardedTransactionChain(this);

        synchronized (this) {
            childChains.add(ret);
        }

        return ret;
    }

    synchronized void removeTransactionChain(final ShardedTransactionChain chain) {
        childChains.remove(chain);
    }

    @Override
    public synchronized void onShardInfoUpdated(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        for (ShardedTransactionChain chain : childChains) {
            chain.onShardInfoUpdated(shardName, primaryShardInfo);
        }
        super.onShardInfoUpdated(shardName, primaryShardInfo);
    }

    @Override
    protected DataTree dataTreeForFactory(final SingleLocalTransactionFactory factory) {
        return factory.getDataTree();
    }
}
