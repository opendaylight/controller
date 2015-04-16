/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.ResolveDataChangeEventsTask;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal shard state, similar to a DOMStore, but optimized for use in the actor system,
 * e.g. it does not expose public interfaces and assumes it is only ever called from a
 * single thread.
 *
 * This class is not part of the API contract and is subject to change at any time.
 */
@NotThreadSafe
@VisibleForTesting
public final class ShardDataTree extends ShardDataTreeTransactionParent {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);
    private static final ShardDataTreeNotificationManager MANAGER = new ShardDataTreeNotificationManager();
    private final Map<String, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final ShardDataTreeChangePublisher treeChangePublisher = new ShardDataTreeChangePublisher();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final TipProducingDataTree dataTree;

    ShardDataTree(final SchemaContext schemaContext) {
        dataTree = InMemoryDataTreeFactory.getInstance().create();
        if (schemaContext != null) {
            dataTree.setSchemaContext(schemaContext);
        }
    }

    TipProducingDataTree getDataTree() {
        return dataTree;
    }

    void updateSchemaContext(final SchemaContext schemaContext) {
        dataTree.setSchemaContext(schemaContext);
    }

    private ShardDataTreeTransactionChain ensureTransactionChain(final String chainId) {
        ShardDataTreeTransactionChain chain = transactionChains.get(chainId);
        if (chain == null) {
            chain = new ShardDataTreeTransactionChain(chainId, this);
            transactionChains.put(chainId, chain);
        }

        return chain;
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final String txId, final String chainId) {
        if (Strings.isNullOrEmpty(chainId)) {
            return new ReadOnlyShardDataTreeTransaction(txId, dataTree.takeSnapshot());
        }

        return ensureTransactionChain(chainId).newReadOnlyTransaction(txId);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final String txId, final String chainId) {
        if (Strings.isNullOrEmpty(chainId)) {
            return new ReadWriteShardDataTreeTransaction(this, txId, dataTree.takeSnapshot().newModification());
        }

        return ensureTransactionChain(chainId).newReadWriteTransaction(txId);
    }

    void notifyListeners(final DataTreeCandidateTip candidate) {
        // DataTreeChanges first, as they are more light-weight
        treeChangePublisher.publishChanges(candidate);

        // DataChanges second, as they are heavier
        ResolveDataChangeEventsTask.create(candidate, listenerTree).resolve(MANAGER);
    }

    void closeAllTransactionChains() {
        for (ShardDataTreeTransactionChain chain : transactionChains.values()) {
            chain.close();
        }

        transactionChains.clear();
    }

    void closeTransactionChain(final String transactionChainId) {
        final ShardDataTreeTransactionChain chain = transactionChains.remove(transactionChainId);
        if (chain != null) {
            chain.close();
        } else {
            LOG.warn("Closing non-existent transaction chain {}", transactionChainId);
        }
    }

    ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> registerChangeListener(
            final YangInstanceIdentifier path,
            final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, final DataChangeScope scope) {
        return listenerTree.registerDataChangeListener(path, listener, scope);
    }

    ListenerRegistration<DOMDataTreeChangeListener> registerTreeChangeListener(final YangInstanceIdentifier path,
            final DOMDataTreeChangeListener listener) {
        return treeChangePublisher.registerTreeChangeListener(path, listener);
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction) {
        // Intentional no-op
    }

    @Override
    DOMStoreThreePhaseCommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        final DataTreeModification snapshot = transaction.getSnapshot();
        snapshot.ready();
        return new ShardDataTreeCohort(this, snapshot);
    }
}
