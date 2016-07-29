/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.CohortRegistryCommand;
import org.opendaylight.controller.cluster.datastore.ShardDataTreeCohort.State;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Internal shard state, similar to a DOMStore, but optimized for use in the actor system,
 * e.g. it does not expose public interfaces and assumes it is only ever called from a
 * single thread.
 *
 * This class is not part of the API contract and is subject to change at any time.
 */
@NotThreadSafe
public class ShardDataTree extends ShardDataTreeTransactionParent {
    private static final class CommitEntry {
        final SimpleShardDataTreeCohort cohort;
        long lastAccess;

        CommitEntry(final SimpleShardDataTreeCohort cohort, final long now) {
            this.cohort = Preconditions.checkNotNull(cohort);
            lastAccess = now;
        }
    }

    private static final Timeout COMMIT_STEP_TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);

    private final Map<LocalHistoryIdentifier, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final DataTreeCohortActorRegistry cohortRegistry = new DataTreeCohortActorRegistry();
    private final Queue<CommitEntry> pendingTransactions = new ArrayDeque<>();
    private final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher;
    private final ShardDataChangeListenerPublisher dataChangeListenerPublisher;
    private final Collection<ShardDataTreeMetadata<?>> metadata;
    private final TipProducingDataTree dataTree;
    private final String logContext;
    private final Shard shard;
    private Runnable runOnPendingTransactionsComplete;

    private SchemaContext schemaContext;

    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TipProducingDataTree dataTree,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final ShardDataChangeListenerPublisher dataChangeListenerPublisher, final String logContext,
            final ShardDataTreeMetadata<?>... metadata) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        updateSchemaContext(schemaContext);

        this.shard = Preconditions.checkNotNull(shard);
        this.treeChangeListenerPublisher = Preconditions.checkNotNull(treeChangeListenerPublisher);
        this.dataChangeListenerPublisher = Preconditions.checkNotNull(dataChangeListenerPublisher);
        this.logContext = Preconditions.checkNotNull(logContext);
        this.metadata = ImmutableList.copyOf(metadata);
    }

    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final ShardDataChangeListenerPublisher dataChangeListenerPublisher, final String logContext) {
        this(shard, schemaContext, InMemoryDataTreeFactory.getInstance().create(treeType),
                treeChangeListenerPublisher, dataChangeListenerPublisher, logContext);
    }

    @VisibleForTesting
    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType) {
        this(shard, schemaContext, treeType, new DefaultShardDataTreeChangeListenerPublisher(),
                new DefaultShardDataChangeListenerPublisher(), "");
    }

    String logContext() {
        return logContext;
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

    /**
     * Take a snapshot of current state for later recovery.
     *
     * @return A state snapshot
     */
    @Nonnull ShardDataTreeSnapshot takeStateSnapshot() {
        final NormalizedNode<?, ?> rootNode = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.EMPTY).get();
        final Builder<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metaBuilder =
                ImmutableMap.builder();

        for (final ShardDataTreeMetadata<?> m : metadata) {
            final ShardDataTreeSnapshotMetadata<?> meta = m.toStapshot();
            if (meta != null) {
                metaBuilder.put(meta.getType(), meta);
            }
        }

        return new MetadataShardDataTreeSnapshot(rootNode, metaBuilder.build());
    }

    private void applySnapshot(final @Nonnull ShardDataTreeSnapshot snapshot,
            final UnaryOperator<DataTreeModification> wrapper) throws DataValidationFailedException {
        final Stopwatch elapsed = Stopwatch.createStarted();

        if (!pendingTransactions.isEmpty()) {
            LOG.warn("{}: applying state snapshot with pending transactions", logContext);
        }

        final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> snapshotMeta;
        if (snapshot instanceof MetadataShardDataTreeSnapshot) {
            snapshotMeta = ((MetadataShardDataTreeSnapshot) snapshot).getMetadata();
        } else {
            snapshotMeta = ImmutableMap.of();
        }

        for (final ShardDataTreeMetadata<?> m : metadata) {
            final ShardDataTreeSnapshotMetadata<?> s = snapshotMeta.get(m.getSupportedType());
            if (s != null) {
                m.applySnapshot(s);
            } else {
                m.reset();
            }
        }

        final DataTreeModification mod = wrapper.apply(dataTree.takeSnapshot().newModification());
        // delete everything first
        mod.delete(YangInstanceIdentifier.EMPTY);

        final java.util.Optional<NormalizedNode<?, ?>> maybeNode = snapshot.getRootNode();
        if (maybeNode.isPresent()) {
            // Add everything from the remote node back
            mod.write(YangInstanceIdentifier.EMPTY, maybeNode.get());
        }
        mod.ready();

        final DataTreeModification unwrapped = unwrap(mod);
        dataTree.validate(unwrapped);
        dataTree.commit(dataTree.prepare(unwrapped));
        LOG.debug("{}: state snapshot applied in %s", logContext, elapsed);
    }

    private PruningDataTreeModification wrapWithPruning(final DataTreeModification delegate) {
        return new PruningDataTreeModification(delegate, dataTree, schemaContext);
    }

    private static DataTreeModification unwrap(final DataTreeModification modification) {
        if (modification instanceof PruningDataTreeModification) {
            return ((PruningDataTreeModification)modification).delegate();
        }
        return modification;
    }

    /**
     * Apply a snapshot coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param snapshot Snapshot that needs to be applied
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applyRecoverySnapshot(final @Nonnull ShardDataTreeSnapshot snapshot) throws DataValidationFailedException {
        applySnapshot(snapshot, this::wrapWithPruning);
    }


    /**
     * Apply a snapshot coming from the leader. This method assumes the leader and follower SchemaContexts match and
     * does not perform any pruning.
     *
     * @param snapshot Snapshot that needs to be applied
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applySnapshot(final @Nonnull ShardDataTreeSnapshot snapshot) throws DataValidationFailedException {
        applySnapshot(snapshot, UnaryOperator.identity());
    }

    private void applyRecoveryCandidate(final DataTreeCandidate candidate) throws DataValidationFailedException {
        final PruningDataTreeModification mod = wrapWithPruning(dataTree.takeSnapshot().newModification());
        DataTreeCandidates.applyToModification(mod, candidate);
        mod.ready();

        final DataTreeModification unwrapped = mod.delegate();
        LOG.trace("{}: Applying recovery modification {}", logContext, unwrapped);

        dataTree.validate(unwrapped);
        dataTree.commit(dataTree.prepare(unwrapped));
    }

    /**
     * Apply a payload coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param payload Payload
     * @throws IOException when the snapshot fails to deserialize
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applyRecoveryPayload(final @Nonnull Payload payload) throws IOException, DataValidationFailedException {
        if (payload instanceof CommitTransactionPayload) {
            final Entry<TransactionIdentifier, DataTreeCandidate> e = ((CommitTransactionPayload) payload).getCandidate();
            applyRecoveryCandidate(e.getValue());
            allMetadataCommittedTransaction(e.getKey());
        } else if (payload instanceof DataTreeCandidatePayload) {
            applyRecoveryCandidate(((DataTreeCandidatePayload) payload).getCandidate());
        } else {
            LOG.warn("{}: ignoring unhandled payload {}", logContext, payload);
        }
    }

    @VisibleForTesting
    void applyReplicatedCandidate(final Identifier identifier, final DataTreeCandidate foreign)
            throws DataValidationFailedException {
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

    /**
     * Apply a payload coming from the leader, which could actually be us. This method assumes the leader and follower
     * SchemaContexts match and does not perform any pruning.
     *
     * @param identifier Payload identifier as returned from RaftActor
     * @param payload Payload
     * @throws IOException when the snapshot fails to deserialize
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applyReplicatedPayload(final Identifier identifier, final Payload payload) throws IOException,
            DataValidationFailedException {
        /*
         * This is a bit more involved than it needs to be due to to the fact we do not want to be touching the payload
         * if we are the leader and it has originated with us.
         *
         * The identifier will only ever be non-null when we were the leader which achieved consensus. Unfortunately,
         * though, this may not be the case anymore, as we are being called some time afterwards and we may not be
         * acting in that capacity anymore.
         *
         * In any case, we know that this is an entry coming from replication, hence we can be sure we will not observe
         * pre-Boron state -- which limits the number of options here.
         */
        if (payload instanceof CommitTransactionPayload) {
            if (identifier == null) {
                final Entry<TransactionIdentifier, DataTreeCandidate> e = ((CommitTransactionPayload) payload).getCandidate();
                applyReplicatedCandidate(e.getKey(), e.getValue());
                allMetadataCommittedTransaction(e.getKey());
            } else {
                Verify.verify(identifier instanceof TransactionIdentifier);
                payloadReplicationComplete((TransactionIdentifier) identifier);
            }
        } else {
            LOG.warn("{}: ignoring unhandled identifier {} payload {}", logContext, identifier, payload);
        }
    }

    private void payloadReplicationComplete(final TransactionIdentifier txId) {
        final CommitEntry current = pendingTransactions.peek();
        if (current == null) {
            LOG.warn("{}: No outstanding transactions, ignoring consensus on transaction {}", logContext, txId);
            return;
        }

        if (!current.cohort.getIdentifier().equals(txId)) {
            LOG.warn("{}: Head of queue is {}, ignoring consensus on transaction {}", logContext,
                current.cohort.getIdentifier(), txId);
            return;
        }

        finishCommit(current.cohort);
    }

    private void allMetadataCommittedTransaction(final TransactionIdentifier txId) {
        for (final ShardDataTreeMetadata<?> m : metadata) {
            m.onTransactionCommitted(txId);
        }
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
            final ShardDataChangeListenerPublisher localPublisher = dataChangeListenerPublisher.newInstance();
            localPublisher.registerDataChangeListener(listenerReg.getPath(), listenerReg.getInstance(),
                    listenerReg.getScope());
            localPublisher.publishChanges(currentState.get(), logContext);
        }
    }

    void notifyOfInitialData(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
            final Optional<DataTreeCandidate> currentState) {
        if (currentState.isPresent()) {
            final ShardDataTreeChangeListenerPublisher localPublisher = treeChangeListenerPublisher.newInstance();
            localPublisher.registerTreeChangeListener(path, listener);
            localPublisher.publishChanges(currentState.get(), logContext);
        }
    }

    void closeAllTransactionChains() {
        for (final ShardDataTreeTransactionChain chain : transactionChains.values()) {
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

    int getQueueSize() {
        return pendingTransactions.size();
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction) {
        // Intentional no-op
    }

    @Override
    ShardDataTreeCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        final DataTreeModification snapshot = transaction.getSnapshot();
        snapshot.ready();

        return createReadyCohort(transaction.getId(), snapshot);
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

    /**
     * @deprecated This method violates DataTree containment and will be removed.
     */
    @VisibleForTesting
    @Deprecated
    public DataTreeCandidate commit(final DataTreeModification modification) throws DataValidationFailedException {
        modification.ready();
        dataTree.validate(modification);
        final DataTreeCandidate candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);
        return candidate;
    }

    public Collection<ShardDataTreeCohort> getAndClearPendingTransactions() {
        final Collection<ShardDataTreeCohort> ret = new ArrayList<>(pendingTransactions.size());
        for(final CommitEntry entry: pendingTransactions) {
            ret.add(entry.cohort);
        }

        pendingTransactions.clear();
        return ret;
    }

    private void processNextTransaction() {
        while (!pendingTransactions.isEmpty()) {
            final CommitEntry entry = pendingTransactions.peek();
            final SimpleShardDataTreeCohort cohort = entry.cohort;
            final DataTreeModification modification = cohort.getDataTreeModification();

            if(cohort.getState() != State.CAN_COMMIT_PENDING) {
                break;
            }

            LOG.debug("{}: Validating transaction {}", logContext, cohort.getIdentifier());
            final Exception cause;
            try {
                dataTree.validate(modification);
                LOG.debug("{}: Transaction {} validated", logContext, cohort.getIdentifier());
                cohort.successfulCanCommit();
                entry.lastAccess = shard.ticker().read();
                return;
            } catch (final ConflictingModificationAppliedException e) {
                LOG.warn("{}: Store Tx {}: Conflicting modification for path {}.", logContext, cohort.getIdentifier(),
                    e.getPath());
                cause = new OptimisticLockFailedException("Optimistic lock failed.", e);
            } catch (final DataValidationFailedException e) {
                LOG.warn("{}: Store Tx {}: Data validation failed for path {}.", logContext, cohort.getIdentifier(),
                    e.getPath(), e);

                // For debugging purposes, allow dumping of the modification. Coupled with the above
                // precondition log, it should allow us to understand what went on.
                LOG.debug("{}: Store Tx {}: modifications: {} tree: {}", cohort.getIdentifier(), modification, dataTree);
                cause = new TransactionCommitFailedException("Data did not pass validation.", e);
            } catch (final Exception e) {
                LOG.warn("{}: Unexpected failure in validation phase", logContext, e);
                cause = e;
            }

            // Failure path: propagate the failure, remove the transaction from the queue and loop to the next one
            pendingTransactions.poll().cohort.failedCanCommit(cause);
        }

        maybeRunOperationOnPendingTransactionsComplete();
    }

    void startCanCommit(final SimpleShardDataTreeCohort cohort) {
        final SimpleShardDataTreeCohort current = pendingTransactions.peek().cohort;
        if (!cohort.equals(current)) {
            LOG.debug("{}: Transaction {} scheduled for canCommit step", logContext, cohort.getIdentifier());
            return;
        }

        processNextTransaction();
    }

    private void failPreCommit(final Exception cause) {
        shard.getShardMBean().incrementFailedTransactionsCount();
        pendingTransactions.poll().cohort.failedPreCommit(cause);
        processNextTransaction();
    }

    void startPreCommit(final SimpleShardDataTreeCohort cohort) {
        final CommitEntry entry = pendingTransactions.peek();
        Preconditions.checkState(entry != null, "Attempted to pre-commit of %s when no transactions pending", cohort);

        final SimpleShardDataTreeCohort current = entry.cohort;
        Verify.verify(cohort.equals(current), "Attempted to pre-commit %s while %s is pending", cohort, current);
        final DataTreeCandidateTip candidate;
        try {
            candidate = dataTree.prepare(cohort.getDataTreeModification());
        } catch (final Exception e) {
            failPreCommit(e);
            return;
        }

        try {
            cohort.userPreCommit(candidate);
        } catch (ExecutionException | TimeoutException e) {
            failPreCommit(e);
            return;
        }

        entry.lastAccess = shard.ticker().read();
        cohort.successfulPreCommit(candidate);
    }

    private void failCommit(final Exception cause) {
        shard.getShardMBean().incrementFailedTransactionsCount();
        pendingTransactions.poll().cohort.failedCommit(cause);
        processNextTransaction();
    }

    private void finishCommit(final SimpleShardDataTreeCohort cohort) {
        final TransactionIdentifier txId = cohort.getIdentifier();
        final DataTreeCandidate candidate = cohort.getCandidate();

        LOG.debug("{}: Resuming commit of transaction {}", logContext, txId);

        try {
            dataTree.commit(candidate);
        } catch (final Exception e) {
            LOG.error("{}: Failed to commit transaction {}", logContext, txId, e);
            failCommit(e);
            return;
        }

        shard.getShardMBean().incrementCommittedTransactionCount();
        shard.getShardMBean().setLastCommittedTransactionTime(System.currentTimeMillis());

        // FIXME: propagate journal index
        pendingTransactions.poll().cohort.successfulCommit(UnsignedLong.ZERO);

        LOG.trace("{}: Transaction {} committed, proceeding to notify", logContext, txId);
        notifyListeners(candidate);

        processNextTransaction();
    }

    void startCommit(final SimpleShardDataTreeCohort cohort, final DataTreeCandidate candidate) {
        final CommitEntry entry = pendingTransactions.peek();
        Preconditions.checkState(entry != null, "Attempted to start commit of %s when no transactions pending", cohort);

        final SimpleShardDataTreeCohort current = entry.cohort;
        Verify.verify(cohort.equals(current), "Attempted to commit %s while %s is pending", cohort, current);

        if (shard.canSkipPayload() || candidate.getRootNode().getModificationType() == ModificationType.UNMODIFIED) {
            LOG.debug("{}: No replication required, proceeding to finish commit", logContext);
            finishCommit(cohort);
            return;
        }

        final TransactionIdentifier txId = cohort.getIdentifier();
        final Payload payload;
        try {
            payload = CommitTransactionPayload.create(txId, candidate);
        } catch (final IOException e) {
            LOG.error("{}: Failed to encode transaction {} candidate {}", logContext, txId, candidate, e);
            pendingTransactions.poll().cohort.failedCommit(e);
            return;
        }

        // Once completed, we will continue via payloadReplicationComplete
        entry.lastAccess = shard.ticker().read();
        shard.persistPayload(txId, payload);
        LOG.debug("{}: Transaction {} submitted to persistence", logContext, txId);
    }

    void processCohortRegistryCommand(final ActorRef sender, final CohortRegistryCommand message) {
        cohortRegistry.process(sender, message);
    }

    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier txId,
            final DataTreeModification modification) {
        final SimpleShardDataTreeCohort cohort = new SimpleShardDataTreeCohort(this, modification, txId,
                cohortRegistry.createCohort(schemaContext, txId, COMMIT_STEP_TIMEOUT));
        pendingTransactions.add(new CommitEntry(cohort, shard.ticker().read()));
        return cohort;
    }

    void checkForExpiredTransactions(final long transactionCommitTimeoutMillis) {
        final long timeout = TimeUnit.MILLISECONDS.toNanos(transactionCommitTimeoutMillis);
        final long now = shard.ticker().read();
        final CommitEntry currentTx = pendingTransactions.peek();
        if (currentTx != null && currentTx.lastAccess + timeout < now) {
            LOG.warn("{}: Current transaction {} has timed out after {} ms in state {}", logContext,
                    currentTx.cohort.getIdentifier(), transactionCommitTimeoutMillis, currentTx.cohort.getState());
            boolean processNext = true;
            switch (currentTx.cohort.getState()) {
                case CAN_COMMIT_PENDING:
                    pendingTransactions.poll().cohort.failedCanCommit(new TimeoutException());
                    break;
                case CAN_COMMIT_COMPLETE:
                    pendingTransactions.poll().cohort.reportFailure(new TimeoutException());
                    break;
                case PRE_COMMIT_PENDING:
                    pendingTransactions.poll().cohort.failedPreCommit(new TimeoutException());
                    break;
                case PRE_COMMIT_COMPLETE:
                    // FIXME: this is a legacy behavior problem. Three-phase commit protocol specifies that after we
                    //        are ready we should commit the transaction, not abort it. Our current software stack does
                    //        not allow us to do that consistently, because we persist at the time of commit, hence
                    //        we can end up in a state where we have pre-committed a transaction, then a leader failover
                    //        occurred ... the new leader does not see the pre-committed transaction and does not have
                    //        a running timer. To fix this we really need two persistence events.
                    //
                    //        The first one, done at pre-commit time will hold the transaction payload. When consensus
                    //        is reached, we exit the pre-commit phase and start the pre-commit timer. Followers do not
                    //        apply the state in this event.
                    //
                    //        The second one, done at commit (or abort) time holds only the transaction identifier and
                    //        signals to followers that the state should (or should not) be applied.
                    //
                    //        In order to make the pre-commit timer working across failovers, though, we need
                    //        a per-shard cluster-wide monotonic time, so a follower becoming the leader can accurately
                    //        restart the timer.
                    pendingTransactions.poll().cohort.reportFailure(new TimeoutException());
                    break;
                case COMMIT_PENDING:
                    LOG.warn("{}: Transaction {} is still committing, cannot abort", logContext,
                        currentTx.cohort.getIdentifier());
                    currentTx.lastAccess = now;
                    processNext = false;
                    return;
                case ABORTED:
                case COMMITTED:
                case FAILED:
                case READY:
                default:
                    pendingTransactions.poll();
            }

            if (processNext) {
                processNextTransaction();
            }
        }
    }

    void startAbort(final SimpleShardDataTreeCohort cohort) {
        final Iterator<CommitEntry> it = pendingTransactions.iterator();
        if (!it.hasNext()) {
            LOG.debug("{}: no open transaction while attempting to abort {}", logContext, cohort.getIdentifier());
            return;
        }

        // First entry is special, as it may already be committing
        final CommitEntry first = it.next();
        if (cohort.equals(first.cohort)) {
            if (cohort.getState() != State.COMMIT_PENDING) {
                LOG.debug("{}: aborted head of queue {} in state {}", logContext, cohort.getIdentifier(),
                    cohort.getIdentifier());
                pendingTransactions.poll();
                processNextTransaction();
            } else {
                LOG.warn("{}: transaction {} is committing, skipping abort", logContext, cohort.getIdentifier());
            }

            return;
        }

        while (it.hasNext()) {
            final CommitEntry e = it.next();
            if (cohort.equals(e.cohort)) {
                LOG.debug("{}: aborting queued transaction {}", logContext, cohort.getIdentifier());
                it.remove();
                return;
            }
        }

        LOG.debug("{}: aborted transaction {} not found in the queue", logContext, cohort.getIdentifier());
    }

    void setRunOnPendingTransactionsComplete(final Runnable operation) {
        runOnPendingTransactionsComplete = operation;
        maybeRunOperationOnPendingTransactionsComplete();
    }

    private void maybeRunOperationOnPendingTransactionsComplete() {
      if (runOnPendingTransactionsComplete != null && pendingTransactions.isEmpty()) {
          LOG.debug("{}: Pending transactions complete - running operation {}", logContext,
                  runOnPendingTransactionsComplete);

          runOnPendingTransactionsComplete.run();
          runOnPendingTransactionsComplete = null;
      }
  }
}
