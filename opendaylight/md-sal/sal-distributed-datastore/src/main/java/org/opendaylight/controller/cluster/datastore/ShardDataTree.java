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
import com.google.common.base.Verify;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeTip;
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
    private static final YangInstanceIdentifier ROOT_PATH = YangInstanceIdentifier.builder().build();
    private static final ShardDataTreeNotificationManager MANAGER = new ShardDataTreeNotificationManager();
    private final Map<String, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final ShardDataTreeChangePublisher treeChangePublisher = new ShardDataTreeChangePublisher();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final TipProducingDataTree dataTree;
    private SchemaContext schemaContext;

    /**
     * Optimistic {@link DataTreeCandidate} preparation. Since our DataTree implementation is a
     * {@link TipProducingDataTree}, each {@link DataTreeCandidate} is also a {@link DataTreeTip}, e.g. another
     * candidate can be prepared on top of it. They still need to be committed in sequence. Here we track the current
     * tip of the data tree, which is the last DataTreeCandidate we have in flight, or the DataTree itself. We need to
     * track all the candidates we have outstanding to support aborting a candidate somewhere in the middle of the
     * sequence -- in which case we retain the preceding candidates, but have to prepare all the subsequent ones.
     */
    private final Queue<DataTreeTip> pendingCandidates = new ArrayDeque<>();
    private DataTreeTip tip;

    public ShardDataTree(final SchemaContext schemaContext, final TreeType treeType) {
        dataTree = InMemoryDataTreeFactory.getInstance().create(treeType);
        updateSchemaContext(schemaContext);
        tip = dataTree;
    }

    /**
     * @deprecated Use {@link #ShardDataTree(SchemaContext, TreeType)} instead.
     */
    @Deprecated
    public ShardDataTree(final SchemaContext schemaContext) {
        this(schemaContext, TreeType.OPERATIONAL);
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

    void notifyOfInitialData(final DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
            NormalizedNode<?, ?>>> listenerReg, final Optional<DataTreeCandidate> currentState) {

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

    public Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier path) {
        return dataTree.takeSnapshot().readNode(path);
    }

    public DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }

    public DataTreeModification newModification() {
        return dataTree.takeSnapshot().newModification();
    }

    public DataTreeCandidate commit(final DataTreeModification modification) throws DataValidationFailedException {
        // Direct modification commit is a utility, which cannot be used while we have transactions in-flight
        Preconditions.checkState(pendingCandidates.isEmpty(), "Cannot modify data tree while %s are pending",
            pendingCandidates);

        modification.ready();
        dataTree.validate(modification);
        DataTreeCandidateTip candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);
        return candidate;
    }

    void validate(final DataTreeModification modification) throws DataValidationFailedException {
        tip.validate(modification);
    }

    DataTreeCandidate prepare(final DataTreeModification modification) {
        // The cast here is okay, it is just yangtools API gives us a DataTreeCandidate. We know this comes
        // from InMemoryDataTree and it produces DataTreeCandidateTips.
        final DataTreeCandidateTip candidate = (DataTreeCandidateTip) tip.prepare(modification);

        // Enqueue in the pending queue and set the tip of the data tree.
        pendingCandidates.add(Verify.verifyNotNull(candidate));
        tip = candidate;

        return candidate;
    }

    void commit(final DataTreeCandidate candidate) {
        Verify.verify(candidate.equals(pendingCandidates.peek()),
            "Attempted to commit candidate %s, which is not the head of %s", candidate, pendingCandidates);

        dataTree.commit(candidate);
        pendingCandidates.poll();

        // All pending candidates have been committed, reset the tip to the data tree
        if (tip.equals(candidate)) {
            tip = dataTree;
        }
    }

    void abort(final DataTreeCandidate candidate) {
        // Track the last non-aborted candidate in the queue, default to the data tree itself
        DataTreeTip newTip = dataTree;

        final Iterator<DataTreeTip> it = pendingCandidates.iterator();
        while (it.hasNext()) {
            final DataTreeTip c = it.next();
            if (!candidate.equals(c)) {
                // Not the candidate to be aborted, retain it for possible use as the new tip
                newTip = c;
                continue;
            }

            // Found the requested candidate. Remove it and all subsequent candidates from the queue.
            it.remove();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }

            // Reset the tip to the last candidate in sequence
            tip = newTip;
            return;
        }

        throw new IllegalArgumentException("Candidate " + candidate + " not found in " + pendingCandidates);
    }
}
