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
import com.google.common.base.Strings;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.controller.md.sal.dom.store.impl.ResolveDataChangeEventsTask;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
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
    private static final YangInstanceIdentifier ROOT_PATH = YangInstanceIdentifier.builder().build();
    private static final ShardDataTreeNotificationManager MANAGER = new ShardDataTreeNotificationManager();
    private final Map<String, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final ShardDataTreeChangePublisher treeChangePublisher = new ShardDataTreeChangePublisher();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final TipProducingDataTree dataTree;
    private SchemaContext schemaContext;

    public ShardDataTree(final SchemaContext schemaContext) {
        dataTree = InMemoryDataTreeFactory.getInstance().create();
        updateSchemaContext(schemaContext);

    }

    public TipProducingDataTree getDataTree() {
        return dataTree;
    }

    SchemaContext getSchemaContext() {
        return schemaContext;
    }

    void updateSchemaContext(final SchemaContext schemaContext) {
        Preconditions.checkNotNull(schemaContext);
        this.schemaContext = schemaContext;
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
            return new ReadWriteShardDataTreeTransaction(ShardDataTree.this, txId, dataTree.takeSnapshot()
                    .newModification());
        }

        return ensureTransactionChain(chainId).newReadWriteTransaction(txId);
    }

    public void notifyListeners(final DataTreeCandidate candidate) {
        LOG.debug("Notifying listeners on candidate {}", candidate);

        // DataTreeChanges first, as they are more light-weight
        treeChangePublisher.publishChanges(candidate);

        // DataChanges second, as they are heavier
        ResolveDataChangeEventsTask.create(candidate, listenerTree).resolve(MANAGER);
    }

    void notifyOfInitialData(DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
            NormalizedNode<?, ?>>> listenerReg, Optional<DataTreeCandidate> currentState) {

        if(currentState.isPresent()) {
            ListenerTree localListenerTree = ListenerTree.create();
            localListenerTree.registerDataChangeListener(listenerReg.getPath(), listenerReg.getInstance(),
                    listenerReg.getScope());

            ResolveDataChangeEventsTask.create(currentState.get(), localListenerTree).resolve(MANAGER);
        }
    }

    void notifyOfInitialData(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
            final Optional<DataTreeCandidate> currentState) {
        if(currentState.isPresent()) {
            ShardDataTreeChangePublisher localTreeChangePublisher = new ShardDataTreeChangePublisher();
            localTreeChangePublisher.registerTreeChangeListener(path, listener);
            localTreeChangePublisher.publishChanges(currentState.get());
        }
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

    Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
            Optional<DataTreeCandidate>> registerChangeListener(final YangInstanceIdentifier path,
                    final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener,
                    final DataChangeScope scope) {
        final DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> reg =
                listenerTree.registerDataChangeListener(path, listener, scope);

        return new SimpleEntry<>(reg, readCurrentData());
    }

    private Optional<DataTreeCandidate> readCurrentData() {
        final Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(ROOT_PATH);
        return currentState.isPresent() ? Optional.of(DataTreeCandidates.fromNormalizedNode(
                ROOT_PATH, currentState.get())) : Optional.<DataTreeCandidate>absent();
    }

    public Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> registerTreeChangeListener(
            final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener) {
        final ListenerRegistration<DOMDataTreeChangeListener> reg = treeChangePublisher.registerTreeChangeListener(
                path, listener);

        return new SimpleEntry<>(reg, readCurrentData());
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
        return new SimpleShardDataTreeCohort(this, snapshot, transaction.getId());
    }

    public Optional<NormalizedNode<?, ?>> readNode(YangInstanceIdentifier path) {
        return dataTree.takeSnapshot().readNode(path);
    }

    public DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }

    public DataTreeModification newModification() {
        return dataTree.takeSnapshot().newModification();
    }

    public DataTreeCandidate commit(DataTreeModification modification) throws DataValidationFailedException {
        modification.ready();
        dataTree.validate(modification);
        DataTreeCandidateTip candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);
        return candidate;
    }
}
