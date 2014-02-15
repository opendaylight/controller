/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.statistics.manager.MultipartMessageManager.StatsRequestType;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

abstract class AbstractStatsTracker<I, K> {
    private static final Function<RpcResult<? extends TransactionAware>, TransactionId> FUNCTION =
            new Function<RpcResult<? extends TransactionAware>, TransactionId>() {
        @Override
        public TransactionId apply(RpcResult<? extends TransactionAware> input) {
            return input.getResult().getTransactionId();
        }
    };

    private final Map<K, Long> trackedItems = new HashMap<>();
    private final FlowCapableContext context;
    private final long lifetimeNanos;

    protected AbstractStatsTracker(final FlowCapableContext context, final long lifetimeNanos) {
        this.context = Preconditions.checkNotNull(context);
        this.lifetimeNanos = lifetimeNanos;
    }

    protected final InstanceIdentifierBuilder<Node> getNodeIdentifierBuilder() {
        return InstanceIdentifier.builder(getNodeIdentifier());
    }

    protected final NodeRef getNodeRef() {
        return context.getNodeRef();
    }

    protected final InstanceIdentifier<Node> getNodeIdentifier() {
        return context.getNodeIdentifier();
    }

    protected final <T extends TransactionAware> void requestHelper(Future<RpcResult<T>> future, StatsRequestType type) {
        context.registerTransaction(Futures.transform(JdkFutureAdapters.listenInPoolThread(future), FUNCTION), type);
    }

    protected final DataModificationTransaction startTransaction() {
        return context.startDataModification();
    }

    protected abstract void cleanupSingleStat(DataModificationTransaction trans, K item);
    protected abstract K updateSingleStat(DataModificationTransaction trans, I item);

    public final synchronized void updateStats(List<I> list) {
        final Long expiryTime = System.nanoTime() + lifetimeNanos;
        final DataModificationTransaction trans = startTransaction();

        for (final I item : list) {
            trackedItems.put(updateSingleStat(trans, item), expiryTime);
        }

        trans.commit();
    }

    public final synchronized void cleanup(final DataModificationTransaction trans, long now) {
        for (Iterator<Entry<K, Long>> it = trackedItems.entrySet().iterator();it.hasNext();){
            Entry<K, Long> e = it.next();
            if (now > e.getValue()) {
                cleanupSingleStat(trans, e.getKey());
                it.remove();
            }
        }
    }
}
