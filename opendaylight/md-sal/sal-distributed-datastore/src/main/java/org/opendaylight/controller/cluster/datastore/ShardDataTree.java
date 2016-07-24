/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
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
public class ShardDataTree extends ShardDataTreeTransactionParent {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);

    private final Map<LocalHistoryIdentifier, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher;
    private final ShardDataChangeListenerPublisher dataChangeListenerPublisher;
    private final TipProducingDataTree dataTree;
    private final String logContext;
    private SchemaContext schemaContext;

    public ShardDataTree(final SchemaContext schemaContext, final TreeType treeType,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final ShardDataChangeListenerPublisher dataChangeListenerPublisher, final String logContext) {
        dataTree = InMemoryDataTreeFactory.getInstance().create(treeType);
        updateSchemaContext(schemaContext);

        this.treeChangeListenerPublisher = Preconditions.checkNotNull(treeChangeListenerPublisher);
        this.dataChangeListenerPublisher = Preconditions.checkNotNull(dataChangeListenerPublisher);
        this.logContext = Preconditions.checkNotNull(logContext);
    }

    public ShardDataTree(final SchemaContext schemaContext, final TreeType treeType) {
        this(schemaContext, treeType, new DefaultShardDataTreeChangeListenerPublisher(),
                new DefaultShardDataChangeListenerPublisher(), "");
    }

    public TipProducingDataTree getDataTree() {
        return dataTree;
    }

    SchemaContext getSchemaContext() {
        return schemaContext;
    }

    void updateSchemaContext(final SchemaContext schemaContext) {
        dataTree.setSchemaContext(schemaContext);
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
    }

    private ShardDataTreeTransactionChain ensureTransactionChain(final LocalHistoryIdentifier localHistoryIdentifier) {
        ShardDataTreeTransactionChain chain = transactionChains.get(localHistoryIdentifier);
        if (chain == null) {
            chain = new ShardDataTreeTransactionChain(localHistoryIdentifier, this);
            transactionChains.put(localHistoryIdentifier, chain);
        }

        return chain;
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        if (txId.getHistoryId().getHistoryId() == 0) {
            return new ReadOnlyShardDataTreeTransaction(txId, dataTree.takeSnapshot());
        }

        return ensureTransactionChain(txId.getHistoryId()).newReadOnlyTransaction(txId);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        if (txId.getHistoryId().getHistoryId() == 0) {
            return new ReadWriteShardDataTreeTransaction(ShardDataTree.this, txId, dataTree.takeSnapshot()
                    .newModification());
        }

        return ensureTransactionChain(txId.getHistoryId()).newReadWriteTransaction(txId);
    }

    public void notifyListeners(final DataTreeCandidate candidate) {
        treeChangeListenerPublisher.publishChanges(candidate, logContext);
        dataChangeListenerPublisher.publishChanges(candidate, logContext);
    }

    void notifyOfInitialData(final DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
            NormalizedNode<?, ?>>> listenerReg, final Optional<DataTreeCandidate> currentState) {
        if (currentState.isPresent()) {
            ShardDataChangeListenerPublisher localPublisher = dataChangeListenerPublisher.newInstance();
            localPublisher.registerDataChangeListener(listenerReg.getPath(), listenerReg.getInstance(),
                    listenerReg.getScope());
            localPublisher.publishChanges(currentState.get(), logContext);
        }
    }

    void notifyOfInitialData(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
            final Optional<DataTreeCandidate> currentState) {
        if (currentState.isPresent()) {
            ShardDataTreeChangeListenerPublisher localPublisher = treeChangeListenerPublisher.newInstance();
            localPublisher.registerTreeChangeListener(path, listener);
            localPublisher.publishChanges(currentState.get(), logContext);
        }
    }

    void closeAllTransactionChains() {
        for (ShardDataTreeTransactionChain chain : transactionChains.values()) {
            chain.close();
        }

        transactionChains.clear();
    }

    void closeTransactionChain(final LocalHistoryIdentifier transactionChainId) {
        final ShardDataTreeTransactionChain chain = transactionChains.remove(transactionChainId);
        if (chain != null) {
            chain.close();
        } else {
            LOG.debug("{}: Closing non-existent transaction chain {}", logContext, transactionChainId);
        }
    }

    Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
            Optional<DataTreeCandidate>> registerChangeListener(final YangInstanceIdentifier path,
                    final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener,
                    final DataChangeScope scope) {
        final DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> reg =
                dataChangeListenerPublisher.registerDataChangeListener(path, listener, scope);

        return new SimpleEntry<>(reg, readCurrentData());
    }

    private Optional<DataTreeCandidate> readCurrentData() {
        final Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.EMPTY);
        return currentState.isPresent() ? Optional.of(DataTreeCandidates.fromNormalizedNode(
            YangInstanceIdentifier.EMPTY, currentState.get())) : Optional.<DataTreeCandidate>absent();
    }

    public Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> registerTreeChangeListener(
            final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener) {
        final ListenerRegistration<DOMDataTreeChangeListener> reg = treeChangeListenerPublisher.registerTreeChangeListener(
                path, listener);

        return new SimpleEntry<>(reg, readCurrentData());
    }

    void applyForeignCandidate(final Identifier identifier, final DataTreeCandidate foreign) throws DataValidationFailedException {
        LOG.debug("{}: Applying foreign transaction {}", logContext, identifier);

        final DataTreeModification mod = dataTree.takeSnapshot().newModification();
        DataTreeCandidates.applyToModification(mod, foreign);
        mod.ready();

        LOG.trace("{}: Applying foreign modification {}", logContext, mod);
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
        return new SimpleShardDataTreeCohort(this, snapshot, transaction.getId());
    }

    public Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier path) {
        return dataTree.takeSnapshot().readNode(path);
    }

    public DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }

    public DataTreeModification newModification() {
        return dataTree.takeSnapshot().newModification();
    }

    // FIXME: This should be removed, it violates encapsulation
    public DataTreeCandidate commit(final DataTreeModification modification) throws DataValidationFailedException {
        modification.ready();
        dataTree.validate(modification);
        DataTreeCandidateTip candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);
        return candidate;
    }
}
