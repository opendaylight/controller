/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.NormalizedNodePruner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.ResolveDataChangeEventsTask;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
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
public final class ShardDataTree extends ShardDataTreeTransactionParent {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);
    private static final ShardDataTreeNotificationManager MANAGER = new ShardDataTreeNotificationManager();
    private final Map<String, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final ShardDataTreeChangePublisher treeChangePublisher = new ShardDataTreeChangePublisher();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final TipProducingDataTree dataTree;
    private Set<URI> validNamespaces;
    private ShardDataTreeTransactionFactory transactionFactory = new RecoveryShardDataTreeTransactionFactory();

    ShardDataTree(final SchemaContext schemaContext) {
        dataTree = InMemoryDataTreeFactory.getInstance().create();
        updateSchemaContext(schemaContext);

    }

    TipProducingDataTree getDataTree() {
        return dataTree;
    }

    void updateSchemaContext(final SchemaContext schemaContext) {
        Preconditions.checkNotNull(schemaContext);
        dataTree.setSchemaContext(schemaContext);
        validNamespaces = NormalizedNodePruner.namespaces(schemaContext);
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
            return transactionFactory.newReadOnlyTransaction(txId, chainId);
        }

        return ensureTransactionChain(chainId).newReadOnlyTransaction(txId);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final String txId, final String chainId) {
        if (Strings.isNullOrEmpty(chainId)) {
            return transactionFactory.newReadWriteTransaction(txId, chainId);
        }

        return ensureTransactionChain(chainId).newReadWriteTransaction(txId);
    }

    void notifyListeners(final DataTreeCandidate candidate) {
        LOG.debug("Notifying listeners on candidate {}", candidate);

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
            LOG.debug("Closing non-existent transaction chain {}", transactionChainId);
        }
    }

    Entry<ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>, DOMImmutableDataChangeEvent> registerChangeListener(
            final YangInstanceIdentifier path,
            final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, final DataChangeScope scope) {
        final ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> reg =
                listenerTree.registerDataChangeListener(path, listener, scope);

        final Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(path);
        final DOMImmutableDataChangeEvent event;
        if (currentState.isPresent()) {
            final NormalizedNode<?, ?> data = currentState.get();
            event = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE).setAfter(data).addCreated(path, data).build();
        } else {
            event = null;
        }

        return new SimpleEntry<>(reg, event);
    }

    Entry<ListenerRegistration<DOMDataTreeChangeListener>, DataTreeCandidate> registerTreeChangeListener(final YangInstanceIdentifier path,
            final DOMDataTreeChangeListener listener) {
        final ListenerRegistration<DOMDataTreeChangeListener> reg = treeChangePublisher.registerTreeChangeListener(path, listener);

        final Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(path);
        final DataTreeCandidate event;
        if (currentState.isPresent()) {
            event = DataTreeCandidates.fromNormalizedNode(path, currentState.get());
        } else {
            event = null;
        }
        return new SimpleEntry<>(reg, event);
    }

    void applyForeignCandidate(final String identifier, final DataTreeCandidate foreign) throws DataValidationFailedException {
        LOG.debug("Applying foreign transaction {}", identifier);

        final DataTreeModification mod = dataTree.takeSnapshot().newModification();
        DataTreeCandidates.applyToModification(mod, foreign);
        mod.ready();

        LOG.trace("Applying foreign modification {}", mod);
        dataTree.validate(mod);
        final DataTreeCandidate candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);
        notifyListeners(candidate);
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction) {
        // Intentional no-op
    }

    @Override
    ShardDataTreeCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        final DataTreeModification snapshot = transaction.getSnapshot();
        snapshot.ready();
        return new SimpleShardDataTreeCohort(this, snapshot);
    }

    void recoveryDone(){
        transactionFactory = new RegularShardDataTreeTransactionFactory();
    }

    @VisibleForTesting
    ShardDataTreeTransactionFactory getTransactionFactory(){
        return transactionFactory;
    }

    @VisibleForTesting
    static interface ShardDataTreeTransactionFactory {
        ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final String txId, final String chainId);

        ReadWriteShardDataTreeTransaction newReadWriteTransaction(final String txId, final String chainId);
    }

    @VisibleForTesting
    class RecoveryShardDataTreeTransactionFactory implements ShardDataTreeTransactionFactory {

        @Override
        public ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(String txId, String chainId) {
            return new ReadOnlyShardDataTreeTransaction(txId,
                    new ShardDataTreeSnapshot(dataTree.takeSnapshot(), validNamespaces));
        }

        @Override
        public ReadWriteShardDataTreeTransaction newReadWriteTransaction(String txId, String chainId) {
            return new ReadWriteShardDataTreeTransaction(ShardDataTree.this, txId,
                    new ShardDataTreeSnapshot(dataTree.takeSnapshot(), validNamespaces).newModification());
        }
    }

    @VisibleForTesting
    class RegularShardDataTreeTransactionFactory implements ShardDataTreeTransactionFactory {

        @Override
        public ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(String txId, String chainId) {
            return new ReadOnlyShardDataTreeTransaction(txId, dataTree.takeSnapshot());

        }

        @Override
        public ReadWriteShardDataTreeTransaction newReadWriteTransaction(String txId, String chainId) {
            return new ReadWriteShardDataTreeTransaction(ShardDataTree.this, txId, dataTree.takeSnapshot()
                    .newModification());
        }
    }
}
