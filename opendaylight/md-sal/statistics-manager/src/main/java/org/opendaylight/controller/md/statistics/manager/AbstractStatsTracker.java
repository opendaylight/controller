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

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

abstract class AbstractStatsTracker<I, K> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractStatsTracker.class);
    private final FutureCallback<RpcResult<? extends TransactionAware>> callback =
            new FutureCallback<RpcResult<? extends TransactionAware>>() {
        @Override
        public void onSuccess(RpcResult<? extends TransactionAware> result) {
            if (result.isSuccessful()) {
                final TransactionId id = result.getResult().getTransactionId();
                if (id == null) {
                    final Throwable t = new UnsupportedOperationException("No protocol support");
                    t.fillInStackTrace();
                    onFailure(t);
                } else {
                    context.registerTransaction(id);
                }
            } else {
                logger.debug("Statistics request failed: {}", result.getErrors());

                final Throwable t = new RPCFailedException("Failed to send statistics request", result.getErrors());
                t.fillInStackTrace();
                onFailure(t);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            logger.debug("Failed to send statistics request", t);
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

    protected final <T extends TransactionAware> void requestHelper(Future<RpcResult<T>> future) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future), callback);
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
