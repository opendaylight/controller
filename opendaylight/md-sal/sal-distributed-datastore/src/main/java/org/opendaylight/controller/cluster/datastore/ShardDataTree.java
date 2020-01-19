/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.CohortRegistryCommand;
import org.opendaylight.controller.cluster.datastore.ShardDataTreeCohort.State;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.ReusableNormalizedNodePruner;
import org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload;
import org.opendaylight.controller.cluster.datastore.persisted.CloseLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CreateLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.datastore.utils.DataTreeModificationOutput;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Internal shard state, similar to a DOMStore, but optimized for use in the actor system, e.g. it does not expose
 * public interfaces and assumes it is only ever called from a single thread.
 *
 * <p>
 * This class is not part of the API contract and is subject to change at any time. It is NOT thread-safe.
 */
public class ShardDataTree extends ShardDataTreeTransactionParent {
    private static final class CommitEntry {
        final SimpleShardDataTreeCohort cohort;
        long lastAccess;

        CommitEntry(final SimpleShardDataTreeCohort cohort, final long now) {
            this.cohort = requireNonNull(cohort);
            lastAccess = now;
        }

        @Override
        public String toString() {
            return "CommitEntry [tx=" + cohort.getIdentifier() + ", state=" + cohort.getState() + "]";
        }
    }

    private static final Timeout COMMIT_STEP_TIMEOUT = new Timeout(FiniteDuration.create(5, TimeUnit.SECONDS));
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);

    /**
     * Process this many transactions in a single batched run. If we exceed this limit, we need to schedule later
     * execution to finish up the batch. This is necessary in case of a long list of transactions which progress
     * immediately through their preCommit phase -- if that happens, their completion eats up stack frames and could
     * result in StackOverflowError.
     */
    private static final int MAX_TRANSACTION_BATCH = 100;

    private final Map<LocalHistoryIdentifier, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final DataTreeCohortActorRegistry cohortRegistry = new DataTreeCohortActorRegistry();
    private final Deque<CommitEntry> pendingTransactions = new ArrayDeque<>();
    private final Queue<CommitEntry> pendingCommits = new ArrayDeque<>();
    private final Queue<CommitEntry> pendingFinishCommits = new ArrayDeque<>();

    /**
     * Callbacks that need to be invoked once a payload is replicated.
     */
    private final Map<Payload, Runnable> replicationCallbacks = new HashMap<>();

    private final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher;
    private final Collection<ShardDataTreeMetadata<?>> metadata;
    private final DataTree dataTree;
    private final String logContext;
    private final Shard shard;
    private Runnable runOnPendingTransactionsComplete;

    /**
     * Optimistic {@link DataTreeCandidate} preparation. Since our DataTree implementation is a
     * {@link DataTree}, each {@link DataTreeCandidate} is also a {@link DataTreeTip}, e.g. another
     * candidate can be prepared on top of it. They still need to be committed in sequence. Here we track the current
     * tip of the data tree, which is the last DataTreeCandidate we have in flight, or the DataTree itself.
     */
    private DataTreeTip tip;

    private SchemaContext schemaContext;
    private DataSchemaContextTree dataSchemaContext;

    private int currentTransactionBatch;

    ShardDataTree(final Shard shard, final SchemaContext schemaContext, final DataTree dataTree,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final String logContext,
            final ShardDataTreeMetadata<?>... metadata) {
        this.dataTree = requireNonNull(dataTree);
        updateSchemaContext(schemaContext);

        this.shard = requireNonNull(shard);
        this.treeChangeListenerPublisher = requireNonNull(treeChangeListenerPublisher);
        this.logContext = requireNonNull(logContext);
        this.metadata = ImmutableList.copyOf(metadata);
        tip = dataTree;
    }

    ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType,
            final YangInstanceIdentifier root,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final String logContext,
            final ShardDataTreeMetadata<?>... metadata) {
        this(shard, schemaContext, createDataTree(treeType, root), treeChangeListenerPublisher, logContext, metadata);
    }

    private static DataTree createDataTree(final TreeType treeType, final YangInstanceIdentifier root) {
        final DataTreeConfiguration baseConfig = DataTreeConfiguration.getDefault(treeType);
        return new InMemoryDataTreeFactory().create(new DataTreeConfiguration.Builder(baseConfig.getTreeType())
                .setMandatoryNodesValidation(baseConfig.isMandatoryNodesValidationEnabled())
                .setUniqueIndexes(baseConfig.isUniqueIndexEnabled())
                .setRootPath(root)
                .build());
    }

    @VisibleForTesting
    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType) {
        this(shard, schemaContext, treeType, YangInstanceIdentifier.empty(),
                new DefaultShardDataTreeChangeListenerPublisher(""), "");
    }

    final String logContext() {
        return logContext;
    }

    final long readTime() {
        return shard.ticker().read();
    }

    public DataTree getDataTree() {
        return dataTree;
    }

    SchemaContext getSchemaContext() {
        return schemaContext;
    }

    void updateSchemaContext(final SchemaContext newSchemaContext) {
        dataTree.setSchemaContext(newSchemaContext);
        this.schemaContext = requireNonNull(newSchemaContext);
        this.dataSchemaContext = DataSchemaContextTree.from(newSchemaContext);
    }

    void resetTransactionBatch() {
        currentTransactionBatch = 0;
    }

    /**
     * Take a snapshot of current state for later recovery.
     *
     * @return A state snapshot
     */
    @NonNull ShardDataTreeSnapshot takeStateSnapshot() {
        final NormalizedNode<?, ?> rootNode = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.empty()).get();
        final Builder<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metaBuilder =
                ImmutableMap.builder();

        for (ShardDataTreeMetadata<?> m : metadata) {
            final ShardDataTreeSnapshotMetadata<?> meta = m.toSnapshot();
            if (meta != null) {
                metaBuilder.put(meta.getType(), meta);
            }
        }

        return new MetadataShardDataTreeSnapshot(rootNode, metaBuilder.build());
    }

    private boolean anyPendingTransactions() {
        return !pendingTransactions.isEmpty() || !pendingCommits.isEmpty() || !pendingFinishCommits.isEmpty();
    }

    private void applySnapshot(final @NonNull ShardDataTreeSnapshot snapshot,
            final UnaryOperator<DataTreeModification> wrapper) throws DataValidationFailedException {
        final Stopwatch elapsed = Stopwatch.createStarted();

        if (anyPendingTransactions()) {
            LOG.warn("{}: applying state snapshot with pending transactions", logContext);
        }

        final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> snapshotMeta;
        if (snapshot instanceof MetadataShardDataTreeSnapshot) {
            snapshotMeta = ((MetadataShardDataTreeSnapshot) snapshot).getMetadata();
        } else {
            snapshotMeta = ImmutableMap.of();
        }

        for (ShardDataTreeMetadata<?> m : metadata) {
            final ShardDataTreeSnapshotMetadata<?> s = snapshotMeta.get(m.getSupportedType());
            if (s != null) {
                m.applySnapshot(s);
            } else {
                m.reset();
            }
        }

        final DataTreeModification unwrapped = dataTree.takeSnapshot().newModification();
        final DataTreeModification mod = wrapper.apply(unwrapped);
        // delete everything first
        mod.delete(YangInstanceIdentifier.empty());

        final Optional<NormalizedNode<?, ?>> maybeNode = snapshot.getRootNode();
        if (maybeNode.isPresent()) {
            // Add everything from the remote node back
            mod.write(YangInstanceIdentifier.empty(), maybeNode.get());
        }
        mod.ready();

        dataTree.validate(unwrapped);
        DataTreeCandidateTip candidate = dataTree.prepare(unwrapped);
        dataTree.commit(candidate);
        notifyListeners(candidate);

        LOG.debug("{}: state snapshot applied in {}", logContext, elapsed);
    }

    /**
     * Apply a snapshot coming from the leader. This method assumes the leader and follower SchemaContexts match and
     * does not perform any pruning.
     *
     * @param snapshot Snapshot that needs to be applied
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applySnapshot(final @NonNull ShardDataTreeSnapshot snapshot) throws DataValidationFailedException {
        applySnapshot(snapshot, UnaryOperator.identity());
    }

    private PruningDataTreeModification wrapWithPruning(final DataTreeModification delegate) {
        return new PruningDataTreeModification(delegate, dataTree,
            // TODO: we should be able to reuse the pruner, provided we are not reentrant
            ReusableNormalizedNodePruner.forDataSchemaContext(dataSchemaContext));
    }

    /**
     * Apply a snapshot coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param snapshot Snapshot that needs to be applied
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applyRecoverySnapshot(final @NonNull ShardDataTreeSnapshot snapshot) throws DataValidationFailedException {
        applySnapshot(snapshot, this::wrapWithPruning);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void applyRecoveryCandidate(final CommitTransactionPayload payload) throws IOException {
        final Entry<TransactionIdentifier, DataTreeCandidate> entry = payload.getCandidate();
        final DataTreeModification unwrapped = dataTree.takeSnapshot().newModification();
        final PruningDataTreeModification mod = wrapWithPruning(unwrapped);
        DataTreeCandidates.applyToModification(mod, entry.getValue());
        mod.ready();

        LOG.trace("{}: Applying recovery modification {}", logContext, unwrapped);

        try {
            dataTree.validate(unwrapped);
            dataTree.commit(dataTree.prepare(unwrapped));
        } catch (Exception e) {
            File file = new File(System.getProperty("karaf.data", "."),
                    "failed-recovery-payload-" + logContext + ".out");
            DataTreeModificationOutput.toFile(file, unwrapped);
            throw new IllegalStateException(String.format(
                    "%s: Failed to apply recovery payload. Modification data was written to file %s",
                    logContext, file), e);
        }

        allMetadataCommittedTransaction(entry.getKey());
    }

    /**
     * Apply a payload coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param payload Payload
     * @throws IOException when the snapshot fails to deserialize
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    void applyRecoveryPayload(final @NonNull Payload payload) throws IOException {
        if (payload instanceof CommitTransactionPayload) {
            applyRecoveryCandidate((CommitTransactionPayload) payload);
        } else if (payload instanceof AbortTransactionPayload) {
            allMetadataAbortedTransaction(((AbortTransactionPayload) payload).getIdentifier());
        } else if (payload instanceof PurgeTransactionPayload) {
            allMetadataPurgedTransaction(((PurgeTransactionPayload) payload).getIdentifier());
        } else if (payload instanceof CreateLocalHistoryPayload) {
            allMetadataCreatedLocalHistory(((CreateLocalHistoryPayload) payload).getIdentifier());
        } else if (payload instanceof CloseLocalHistoryPayload) {
            allMetadataClosedLocalHistory(((CloseLocalHistoryPayload) payload).getIdentifier());
        } else if (payload instanceof PurgeLocalHistoryPayload) {
            allMetadataPurgedLocalHistory(((PurgeLocalHistoryPayload) payload).getIdentifier());
        } else {
            LOG.debug("{}: ignoring unhandled payload {}", logContext, payload);
        }
    }

    private void applyReplicatedCandidate(final CommitTransactionPayload payload)
            throws DataValidationFailedException, IOException {
        final Entry<TransactionIdentifier, DataTreeCandidate> entry = payload.getCandidate();
        final TransactionIdentifier identifier = entry.getKey();
        LOG.debug("{}: Applying foreign transaction {}", logContext, identifier);

        final DataTreeModification mod = dataTree.takeSnapshot().newModification();
        DataTreeCandidates.applyToModification(mod, entry.getValue());
        mod.ready();

        LOG.trace("{}: Applying foreign modification {}", logContext, mod);
        dataTree.validate(mod);
        final DataTreeCandidate candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        allMetadataCommittedTransaction(identifier);
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
                applyReplicatedCandidate((CommitTransactionPayload) payload);
            } else {
                verify(identifier instanceof TransactionIdentifier);
                payloadReplicationComplete((TransactionIdentifier) identifier);
            }
        } else if (payload instanceof AbortTransactionPayload) {
            if (identifier != null) {
                payloadReplicationComplete((AbortTransactionPayload) payload);
            }
            allMetadataAbortedTransaction(((AbortTransactionPayload) payload).getIdentifier());
        } else if (payload instanceof PurgeTransactionPayload) {
            if (identifier != null) {
                payloadReplicationComplete((PurgeTransactionPayload) payload);
            }
            allMetadataPurgedTransaction(((PurgeTransactionPayload) payload).getIdentifier());
        } else if (payload instanceof CloseLocalHistoryPayload) {
            if (identifier != null) {
                payloadReplicationComplete((CloseLocalHistoryPayload) payload);
            }
            allMetadataClosedLocalHistory(((CloseLocalHistoryPayload) payload).getIdentifier());
        } else if (payload instanceof CreateLocalHistoryPayload) {
            if (identifier != null) {
                payloadReplicationComplete((CreateLocalHistoryPayload)payload);
            }
            allMetadataCreatedLocalHistory(((CreateLocalHistoryPayload) payload).getIdentifier());
        } else if (payload instanceof PurgeLocalHistoryPayload) {
            if (identifier != null) {
                payloadReplicationComplete((PurgeLocalHistoryPayload)payload);
            }
            allMetadataPurgedLocalHistory(((PurgeLocalHistoryPayload) payload).getIdentifier());
        } else {
            LOG.warn("{}: ignoring unhandled identifier {} payload {}", logContext, identifier, payload);
        }
    }

    private void replicatePayload(final Identifier id, final Payload payload, final @Nullable Runnable callback) {
        if (callback != null) {
            replicationCallbacks.put(payload, callback);
        }
        shard.persistPayload(id, payload, true);
    }

    private void payloadReplicationComplete(final AbstractIdentifiablePayload<?> payload) {
        final Runnable callback = replicationCallbacks.remove(payload);
        if (callback != null) {
            LOG.debug("{}: replication of {} completed, invoking {}", logContext, payload.getIdentifier(), callback);
            callback.run();
        } else {
            LOG.debug("{}: replication of {} has no callback", logContext, payload.getIdentifier());
        }
    }

    private void payloadReplicationComplete(final TransactionIdentifier txId) {
        final CommitEntry current = pendingFinishCommits.peek();
        if (current == null) {
            LOG.warn("{}: No outstanding transactions, ignoring consensus on transaction {}", logContext, txId);
            allMetadataCommittedTransaction(txId);
            return;
        }

        if (!current.cohort.getIdentifier().equals(txId)) {
            LOG.debug("{}: Head of pendingFinishCommits queue is {}, ignoring consensus on transaction {}", logContext,
                current.cohort.getIdentifier(), txId);
            allMetadataCommittedTransaction(txId);
            return;
        }

        finishCommit(current.cohort);
    }

    private void allMetadataAbortedTransaction(final TransactionIdentifier txId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onTransactionAborted(txId);
        }
    }

    private void allMetadataCommittedTransaction(final TransactionIdentifier txId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onTransactionCommitted(txId);
        }
    }

    private void allMetadataPurgedTransaction(final TransactionIdentifier txId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onTransactionPurged(txId);
        }
    }

    private void allMetadataCreatedLocalHistory(final LocalHistoryIdentifier historyId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onHistoryCreated(historyId);
        }
    }

    private void allMetadataClosedLocalHistory(final LocalHistoryIdentifier historyId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onHistoryClosed(historyId);
        }
    }

    private void allMetadataPurgedLocalHistory(final LocalHistoryIdentifier historyId) {
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onHistoryPurged(historyId);
        }
    }

    /**
     * Create a transaction chain for specified history. Unlike {@link #ensureTransactionChain(LocalHistoryIdentifier)},
     * this method is used for re-establishing state when we are taking over
     *
     * @param historyId Local history identifier
     * @param closed True if the chain should be created in closed state (i.e. pending purge)
     * @return Transaction chain handle
     */
    ShardDataTreeTransactionChain recreateTransactionChain(final LocalHistoryIdentifier historyId,
            final boolean closed) {
        final ShardDataTreeTransactionChain ret = new ShardDataTreeTransactionChain(historyId, this);
        final ShardDataTreeTransactionChain existing = transactionChains.putIfAbsent(historyId, ret);
        checkState(existing == null, "Attempted to recreate chain %s, but %s already exists", historyId, existing);
        return ret;
    }

    ShardDataTreeTransactionChain ensureTransactionChain(final LocalHistoryIdentifier historyId,
            final @Nullable Runnable callback) {
        ShardDataTreeTransactionChain chain = transactionChains.get(historyId);
        if (chain == null) {
            chain = new ShardDataTreeTransactionChain(historyId, this);
            transactionChains.put(historyId, chain);
            replicatePayload(historyId, CreateLocalHistoryPayload.create(
                    historyId, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
        } else if (callback != null) {
            callback.run();
        }

        return chain;
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        shard.getShardMBean().incrementReadOnlyTransactionCount();

        if (txId.getHistoryId().getHistoryId() == 0) {
            return new ReadOnlyShardDataTreeTransaction(this, txId, dataTree.takeSnapshot());
        }

        return ensureTransactionChain(txId.getHistoryId(), null).newReadOnlyTransaction(txId);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        shard.getShardMBean().incrementReadWriteTransactionCount();

        if (txId.getHistoryId().getHistoryId() == 0) {
            return new ReadWriteShardDataTreeTransaction(ShardDataTree.this, txId, dataTree.takeSnapshot()
                    .newModification());
        }

        return ensureTransactionChain(txId.getHistoryId(), null).newReadWriteTransaction(txId);
    }

    @VisibleForTesting
    public void notifyListeners(final DataTreeCandidate candidate) {
        treeChangeListenerPublisher.publishChanges(candidate);
    }

    /**
     * Immediately purge all state relevant to leader. This includes all transaction chains and any scheduled
     * replication callbacks.
     */
    void purgeLeaderState() {
        for (ShardDataTreeTransactionChain chain : transactionChains.values()) {
            chain.close();
        }

        transactionChains.clear();
        replicationCallbacks.clear();
    }

    /**
     * Close a single transaction chain.
     *
     * @param id History identifier
     * @param callback Callback to invoke upon completion, may be null
     */
    void closeTransactionChain(final LocalHistoryIdentifier id, final @Nullable Runnable callback) {
        if (commonCloseTransactionChain(id, callback)) {
            replicatePayload(id, CloseLocalHistoryPayload.create(id,
                shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
        }
    }

    /**
     * Close a single transaction chain which is received through ask-based protocol. It does not keep a commit record.
     *
     * @param id History identifier
     */
    void closeTransactionChain(final LocalHistoryIdentifier id) {
        commonCloseTransactionChain(id, null);
    }

    private boolean commonCloseTransactionChain(final LocalHistoryIdentifier id, final @Nullable Runnable callback) {
        final ShardDataTreeTransactionChain chain = transactionChains.get(id);
        if (chain == null) {
            LOG.debug("{}: Closing non-existent transaction chain {}", logContext, id);
            if (callback != null) {
                callback.run();
            }
            return false;
        }

        chain.close();
        return true;
    }

    /**
     * Purge a single transaction chain.
     *
     * @param id History identifier
     * @param callback Callback to invoke upon completion, may be null
     */
    void purgeTransactionChain(final LocalHistoryIdentifier id, final @Nullable Runnable callback) {
        final ShardDataTreeTransactionChain chain = transactionChains.remove(id);
        if (chain == null) {
            LOG.debug("{}: Purging non-existent transaction chain {}", logContext, id);
            if (callback != null) {
                callback.run();
            }
            return;
        }

        replicatePayload(id, PurgeLocalHistoryPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    Optional<DataTreeCandidate> readCurrentData() {
        return dataTree.takeSnapshot().readNode(YangInstanceIdentifier.empty())
                .map(state -> DataTreeCandidates.fromNormalizedNode(YangInstanceIdentifier.empty(), state));
    }

    public void registerTreeChangeListener(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
            final Optional<DataTreeCandidate> initialState,
            final Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration) {
        treeChangeListenerPublisher.registerTreeChangeListener(path, listener, initialState, onRegistration);
    }

    int getQueueSize() {
        return pendingTransactions.size() + pendingCommits.size() + pendingFinishCommits.size();
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        final TransactionIdentifier id = transaction.getIdentifier();
        LOG.debug("{}: aborting transaction {}", logContext, id);
        replicatePayload(id, AbortTransactionPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    @Override
    void abortFromTransactionActor(final AbstractShardDataTreeTransaction<?> transaction) {
        // No-op for free-standing transactions

    }

    @Override
    ShardDataTreeCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction,
            final Optional<SortedSet<String>> participatingShardNames) {
        final DataTreeModification snapshot = transaction.getSnapshot();
        final TransactionIdentifier id = transaction.getIdentifier();
        LOG.debug("{}: readying transaction {}", logContext, id);
        snapshot.ready();
        LOG.debug("{}: transaction {} ready", logContext, id);

        return createReadyCohort(transaction.getIdentifier(), snapshot, participatingShardNames);
    }

    void purgeTransaction(final TransactionIdentifier id, final Runnable callback) {
        LOG.debug("{}: purging transaction {}", logContext, id);
        replicatePayload(id, PurgeTransactionPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    public Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier path) {
        return dataTree.takeSnapshot().readNode(path);
    }

    DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }

    @VisibleForTesting
    public DataTreeModification newModification() {
        return dataTree.takeSnapshot().newModification();
    }

    public Collection<ShardDataTreeCohort> getAndClearPendingTransactions() {
        Collection<ShardDataTreeCohort> ret = new ArrayList<>(getQueueSize());

        for (CommitEntry entry: pendingFinishCommits) {
            ret.add(entry.cohort);
        }

        for (CommitEntry entry: pendingCommits) {
            ret.add(entry.cohort);
        }

        for (CommitEntry entry: pendingTransactions) {
            ret.add(entry.cohort);
        }

        pendingFinishCommits.clear();
        pendingCommits.clear();
        pendingTransactions.clear();
        tip = dataTree;
        return ret;
    }

    /**
     * Called some time after {@link #processNextPendingTransaction()} decides to stop processing.
     */
    void resumeNextPendingTransaction() {
        LOG.debug("{}: attempting to resume transaction processing", logContext);
        processNextPending();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void processNextPendingTransaction() {
        ++currentTransactionBatch;
        if (currentTransactionBatch > MAX_TRANSACTION_BATCH) {
            LOG.debug("{}: Already processed {}, scheduling continuation", logContext, currentTransactionBatch);
            shard.scheduleNextPendingTransaction();
            return;
        }

        processNextPending(pendingTransactions, State.CAN_COMMIT_PENDING, entry -> {
            final SimpleShardDataTreeCohort cohort = entry.cohort;
            final DataTreeModification modification = cohort.getDataTreeModification();

            LOG.debug("{}: Validating transaction {}", logContext, cohort.getIdentifier());
            Exception cause;
            try {
                tip.validate(modification);
                LOG.debug("{}: Transaction {} validated", logContext, cohort.getIdentifier());
                cohort.successfulCanCommit();
                entry.lastAccess = readTime();
                return;
            } catch (ConflictingModificationAppliedException e) {
                LOG.warn("{}: Store Tx {}: Conflicting modification for path {}.", logContext, cohort.getIdentifier(),
                    e.getPath());
                cause = new OptimisticLockFailedException("Optimistic lock failed for path " + e.getPath(), e);
            } catch (DataValidationFailedException e) {
                LOG.warn("{}: Store Tx {}: Data validation failed for path {}.", logContext, cohort.getIdentifier(),
                    e.getPath(), e);

                // For debugging purposes, allow dumping of the modification. Coupled with the above
                // precondition log, it should allow us to understand what went on.
                LOG.debug("{}: Store Tx {}: modifications: {}", logContext, cohort.getIdentifier(), modification);
                LOG.trace("{}: Current tree: {}", logContext, dataTree);
                cause = new TransactionCommitFailedException("Data did not pass validation for path " + e.getPath(), e);
            } catch (Exception e) {
                LOG.warn("{}: Unexpected failure in validation phase", logContext, e);
                cause = e;
            }

            // Failure path: propagate the failure, remove the transaction from the queue and loop to the next one
            pendingTransactions.poll().cohort.failedCanCommit(cause);
        });
    }

    private void processNextPending() {
        processNextPendingCommit();
        processNextPendingTransaction();
    }

    private void processNextPending(final Queue<CommitEntry> queue, final State allowedState,
            final Consumer<CommitEntry> processor) {
        while (!queue.isEmpty()) {
            final CommitEntry entry = queue.peek();
            final SimpleShardDataTreeCohort cohort = entry.cohort;

            if (cohort.isFailed()) {
                LOG.debug("{}: Removing failed transaction {}", logContext, cohort.getIdentifier());
                queue.remove();
                continue;
            }

            if (cohort.getState() == allowedState) {
                processor.accept(entry);
            }

            break;
        }

        maybeRunOperationOnPendingTransactionsComplete();
    }

    private void processNextPendingCommit() {
        processNextPending(pendingCommits, State.COMMIT_PENDING,
            entry -> startCommit(entry.cohort, entry.cohort.getCandidate()));
    }

    private boolean peekNextPendingCommit() {
        final CommitEntry first = pendingCommits.peek();
        return first != null && first.cohort.getState() == State.COMMIT_PENDING;
    }

    void startCanCommit(final SimpleShardDataTreeCohort cohort) {
        final CommitEntry head = pendingTransactions.peek();
        if (head == null) {
            LOG.warn("{}: No transactions enqueued while attempting to start canCommit on {}", logContext, cohort);
            return;
        }
        if (!cohort.equals(head.cohort)) {
            // The tx isn't at the head of the queue so we can't start canCommit at this point. Here we check if this
            // tx should be moved ahead of other tx's in the READY state in the pendingTransactions queue. If this tx
            // has other participating shards, it could deadlock with other tx's accessing the same shards
            // depending on the order the tx's are readied on each shard
            // (see https://jira.opendaylight.org/browse/CONTROLLER-1836). Therefore, if the preceding participating
            // shard names for a preceding pending tx, call it A, in the queue matches that of this tx, then this tx
            // is allowed to be moved ahead of tx A in the queue so it is processed first to avoid potential deadlock
            // if tx A is behind this tx in the pendingTransactions queue for a preceding shard. In other words, since
            // canCommmit for this tx was requested before tx A, honor that request. If this tx is moved to the head of
            // the queue as a result, then proceed with canCommit.

            Collection<String> precedingShardNames = extractPrecedingShardNames(cohort.getParticipatingShardNames());
            if (precedingShardNames.isEmpty()) {
                LOG.debug("{}: Tx {} is scheduled for canCommit step", logContext, cohort.getIdentifier());
                return;
            }

            LOG.debug("{}: Evaluating tx {} for canCommit -  preceding participating shard names {}",
                    logContext, cohort.getIdentifier(), precedingShardNames);
            final Iterator<CommitEntry> iter = pendingTransactions.iterator();
            int index = -1;
            int moveToIndex = -1;
            while (iter.hasNext()) {
                final CommitEntry entry = iter.next();
                ++index;

                if (cohort.equals(entry.cohort)) {
                    if (moveToIndex < 0) {
                        LOG.debug("{}: Not moving tx {} - cannot proceed with canCommit",
                                logContext, cohort.getIdentifier());
                        return;
                    }

                    LOG.debug("{}: Moving {} to index {} in the pendingTransactions queue",
                            logContext, cohort.getIdentifier(), moveToIndex);
                    iter.remove();
                    insertEntry(pendingTransactions, entry, moveToIndex);

                    if (!cohort.equals(pendingTransactions.peek().cohort)) {
                        LOG.debug("{}: Tx {} is not at the head of the queue - cannot proceed with canCommit",
                                logContext, cohort.getIdentifier());
                        return;
                    }

                    LOG.debug("{}: Tx {} is now at the head of the queue - proceeding with canCommit",
                            logContext, cohort.getIdentifier());
                    break;
                }

                if (entry.cohort.getState() != State.READY) {
                    LOG.debug("{}: Skipping pending transaction {} in state {}",
                            logContext, entry.cohort.getIdentifier(), entry.cohort.getState());
                    continue;
                }

                final Collection<String> pendingPrecedingShardNames = extractPrecedingShardNames(
                        entry.cohort.getParticipatingShardNames());

                if (precedingShardNames.equals(pendingPrecedingShardNames)) {
                    if (moveToIndex < 0) {
                        LOG.debug("{}: Preceding shard names {} for pending tx {} match - saving moveToIndex {}",
                                logContext, pendingPrecedingShardNames, entry.cohort.getIdentifier(), index);
                        moveToIndex = index;
                    } else {
                        LOG.debug(
                            "{}: Preceding shard names {} for pending tx {} match but moveToIndex already set to {}",
                            logContext, pendingPrecedingShardNames, entry.cohort.getIdentifier(), moveToIndex);
                    }
                } else {
                    LOG.debug("{}: Preceding shard names {} for pending tx {} differ - skipping",
                        logContext, pendingPrecedingShardNames, entry.cohort.getIdentifier());
                }
            }
        }

        processNextPendingTransaction();
    }

    private static void insertEntry(final Deque<CommitEntry> queue, final CommitEntry entry, final int atIndex) {
        if (atIndex == 0) {
            queue.addFirst(entry);
            return;
        }

        LOG.trace("Inserting into Deque at index {}", atIndex);

        Deque<CommitEntry> tempStack = new ArrayDeque<>(atIndex);
        for (int i = 0; i < atIndex; i++) {
            tempStack.push(queue.poll());
        }

        queue.addFirst(entry);

        tempStack.forEach(queue::addFirst);
    }

    private Collection<String> extractPrecedingShardNames(final Optional<SortedSet<String>> participatingShardNames) {
        return participatingShardNames.map((Function<SortedSet<String>, Collection<String>>)
            set -> set.headSet(shard.getShardName())).orElse(Collections.<String>emptyList());
    }

    private void failPreCommit(final Throwable cause) {
        shard.getShardMBean().incrementFailedTransactionsCount();
        pendingTransactions.poll().cohort.failedPreCommit(cause);
        processNextPendingTransaction();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void startPreCommit(final SimpleShardDataTreeCohort cohort) {
        final CommitEntry entry = pendingTransactions.peek();
        checkState(entry != null, "Attempted to pre-commit of %s when no transactions pending", cohort);

        final SimpleShardDataTreeCohort current = entry.cohort;
        verify(cohort.equals(current), "Attempted to pre-commit %s while %s is pending", cohort, current);

        final TransactionIdentifier currentId = current.getIdentifier();
        LOG.debug("{}: Preparing transaction {}", logContext, currentId);

        final DataTreeCandidateTip candidate;
        try {
            candidate = tip.prepare(cohort.getDataTreeModification());
            LOG.debug("{}: Transaction {} candidate ready", logContext, currentId);
        } catch (DataValidationFailedException | RuntimeException e) {
            failPreCommit(e);
            return;
        }

        cohort.userPreCommit(candidate, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void noop) {
                // Set the tip of the data tree.
                tip = verifyNotNull(candidate);

                entry.lastAccess = readTime();

                pendingTransactions.remove();
                pendingCommits.add(entry);

                LOG.debug("{}: Transaction {} prepared", logContext, currentId);

                cohort.successfulPreCommit(candidate);

                processNextPendingTransaction();
            }

            @Override
            public void onFailure(final Throwable failure) {
                failPreCommit(failure);
            }
        });
    }

    private void failCommit(final Exception cause) {
        shard.getShardMBean().incrementFailedTransactionsCount();
        pendingFinishCommits.poll().cohort.failedCommit(cause);
        processNextPending();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void finishCommit(final SimpleShardDataTreeCohort cohort) {
        final TransactionIdentifier txId = cohort.getIdentifier();
        final DataTreeCandidate candidate = cohort.getCandidate();

        LOG.debug("{}: Resuming commit of transaction {}", logContext, txId);

        if (tip == candidate) {
            // All pending candidates have been committed, reset the tip to the data tree.
            tip = dataTree;
        }

        try {
            dataTree.commit(candidate);
        } catch (Exception e) {
            LOG.error("{}: Failed to commit transaction {}", logContext, txId, e);
            failCommit(e);
            return;
        }

        allMetadataCommittedTransaction(txId);
        shard.getShardMBean().incrementCommittedTransactionCount();
        shard.getShardMBean().setLastCommittedTransactionTime(System.currentTimeMillis());

        // FIXME: propagate journal index
        pendingFinishCommits.poll().cohort.successfulCommit(UnsignedLong.ZERO, () -> {
            LOG.trace("{}: Transaction {} committed, proceeding to notify", logContext, txId);
            notifyListeners(candidate);

            processNextPending();
        });
    }

    void startCommit(final SimpleShardDataTreeCohort cohort, final DataTreeCandidate candidate) {
        final CommitEntry entry = pendingCommits.peek();
        checkState(entry != null, "Attempted to start commit of %s when no transactions pending", cohort);

        final SimpleShardDataTreeCohort current = entry.cohort;
        if (!cohort.equals(current)) {
            LOG.debug("{}: Transaction {} scheduled for commit step", logContext, cohort.getIdentifier());
            return;
        }

        LOG.debug("{}: Starting commit for transaction {}", logContext, current.getIdentifier());

        final TransactionIdentifier txId = cohort.getIdentifier();
        final Payload payload;
        try {
            payload = CommitTransactionPayload.create(txId, candidate,
                    shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity());
        } catch (IOException e) {
            LOG.error("{}: Failed to encode transaction {} candidate {}", logContext, txId, candidate, e);
            pendingCommits.poll().cohort.failedCommit(e);
            processNextPending();
            return;
        }

        // We process next transactions pending canCommit before we call persistPayload to possibly progress subsequent
        // transactions to the COMMIT_PENDING state so the payloads can be batched for replication. This is done for
        // single-shard transactions that immediately transition from canCommit to preCommit to commit. Note that
        // if the next pending transaction is progressed to COMMIT_PENDING and this method (startCommit) is called,
        // the next transaction will not attempt to replicate b/c the current transaction is still at the head of the
        // pendingCommits queue.
        processNextPendingTransaction();

        // After processing next pending transactions, we can now remove the current transaction from pendingCommits.
        // Note this must be done before the call to peekNextPendingCommit below so we check the next transaction
        // in order to properly determine the batchHint flag for the call to persistPayload.
        pendingCommits.remove();
        pendingFinishCommits.add(entry);

        // See if the next transaction is pending commit (ie in the COMMIT_PENDING state) so it can be batched with
        // this transaction for replication.
        boolean replicationBatchHint = peekNextPendingCommit();

        // Once completed, we will continue via payloadReplicationComplete
        shard.persistPayload(txId, payload, replicationBatchHint);

        entry.lastAccess = shard.ticker().read();

        LOG.debug("{}: Transaction {} submitted to persistence", logContext, txId);

        // Process the next transaction pending commit, if any. If there is one it will be batched with this
        // transaction for replication.
        processNextPendingCommit();
    }

    Collection<ActorRef> getCohortActors() {
        return cohortRegistry.getCohortActors();
    }

    void processCohortRegistryCommand(final ActorRef sender, final CohortRegistryCommand message) {
        cohortRegistry.process(sender, message);
    }

    @Override
    ShardDataTreeCohort createFailedCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Exception failure) {
        final SimpleShardDataTreeCohort cohort = new SimpleShardDataTreeCohort(this, mod, txId, failure);
        pendingTransactions.add(new CommitEntry(cohort, readTime()));
        return cohort;
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Optional<SortedSet<String>> participatingShardNames) {
        SimpleShardDataTreeCohort cohort = new SimpleShardDataTreeCohort(this, mod, txId,
                cohortRegistry.createCohort(schemaContext, txId, shard::executeInSelf,
                        COMMIT_STEP_TIMEOUT), participatingShardNames);
        pendingTransactions.add(new CommitEntry(cohort, readTime()));
        return cohort;
    }

    // Exposed for ShardCommitCoordinator so it does not have deal with local histories (it does not care), this mimics
    // the newReadWriteTransaction()
    ShardDataTreeCohort newReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Optional<SortedSet<String>> participatingShardNames) {
        if (txId.getHistoryId().getHistoryId() == 0) {
            return createReadyCohort(txId, mod, participatingShardNames);
        }

        return ensureTransactionChain(txId.getHistoryId(), null).createReadyCohort(txId, mod, participatingShardNames);
    }

    @SuppressFBWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES", justification = "See inline comments below.")
    void checkForExpiredTransactions(final long transactionCommitTimeoutMillis,
            final Function<SimpleShardDataTreeCohort, OptionalLong> accessTimeUpdater) {
        final long timeout = TimeUnit.MILLISECONDS.toNanos(transactionCommitTimeoutMillis);
        final long now = readTime();

        final Queue<CommitEntry> currentQueue = !pendingFinishCommits.isEmpty() ? pendingFinishCommits :
            !pendingCommits.isEmpty() ? pendingCommits : pendingTransactions;
        final CommitEntry currentTx = currentQueue.peek();
        if (currentTx == null) {
            // Empty queue, no-op
            return;
        }

        long delta = now - currentTx.lastAccess;
        if (delta < timeout) {
            // Not expired yet, bail
            return;
        }

        final OptionalLong updateOpt = accessTimeUpdater.apply(currentTx.cohort);
        if (updateOpt.isPresent()) {
            final long newAccess =  updateOpt.getAsLong();
            final long newDelta = now - newAccess;
            if (newDelta < delta) {
                LOG.debug("{}: Updated current transaction {} access time", logContext,
                    currentTx.cohort.getIdentifier());
                currentTx.lastAccess = newAccess;
                delta = newDelta;
            }

            if (delta < timeout) {
                // Not expired yet, bail
                return;
            }
        }

        final long deltaMillis = TimeUnit.NANOSECONDS.toMillis(delta);
        final State state = currentTx.cohort.getState();

        LOG.warn("{}: Current transaction {} has timed out after {} ms in state {}", logContext,
            currentTx.cohort.getIdentifier(), deltaMillis, state);
        boolean processNext = true;
        final TimeoutException cohortFailure = new TimeoutException("Backend timeout in state " + state + " after "
                + deltaMillis + "ms");

        switch (state) {
            case CAN_COMMIT_PENDING:
                currentQueue.remove().cohort.failedCanCommit(cohortFailure);
                break;
            case CAN_COMMIT_COMPLETE:
                // The suppression of the FindBugs "DB_DUPLICATE_SWITCH_CLAUSES" warning pertains to this clause
                // whose code is duplicated with PRE_COMMIT_COMPLETE. The clauses aren't combined in case the code
                // in PRE_COMMIT_COMPLETE is changed.
                currentQueue.remove().cohort.reportFailure(cohortFailure);
                break;
            case PRE_COMMIT_PENDING:
                currentQueue.remove().cohort.failedPreCommit(cohortFailure);
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
                currentQueue.remove().cohort.reportFailure(cohortFailure);
                break;
            case COMMIT_PENDING:
                LOG.warn("{}: Transaction {} is still committing, cannot abort", logContext,
                    currentTx.cohort.getIdentifier());
                currentTx.lastAccess = now;
                processNext = false;
                return;
            case READY:
                currentQueue.remove().cohort.reportFailure(cohortFailure);
                break;
            case ABORTED:
            case COMMITTED:
            case FAILED:
            default:
                currentQueue.remove();
        }

        if (processNext) {
            processNextPending();
        }
    }

    boolean startAbort(final SimpleShardDataTreeCohort cohort) {
        final Iterator<CommitEntry> it = Iterables.concat(pendingFinishCommits, pendingCommits,
                pendingTransactions).iterator();
        if (!it.hasNext()) {
            LOG.debug("{}: no open transaction while attempting to abort {}", logContext, cohort.getIdentifier());
            return true;
        }

        // First entry is special, as it may already be committing
        final CommitEntry first = it.next();
        if (cohort.equals(first.cohort)) {
            if (cohort.getState() != State.COMMIT_PENDING) {
                LOG.debug("{}: aborting head of queue {} in state {}", logContext, cohort.getIdentifier(),
                    cohort.getIdentifier());

                it.remove();
                if (cohort.getCandidate() != null) {
                    rebaseTransactions(it, dataTree);
                }

                processNextPending();
                return true;
            }

            LOG.warn("{}: transaction {} is committing, skipping abort", logContext, cohort.getIdentifier());
            return false;
        }

        DataTreeTip newTip = MoreObjects.firstNonNull(first.cohort.getCandidate(), dataTree);
        while (it.hasNext()) {
            final CommitEntry e = it.next();
            if (cohort.equals(e.cohort)) {
                LOG.debug("{}: aborting queued transaction {}", logContext, cohort.getIdentifier());

                it.remove();
                if (cohort.getCandidate() != null) {
                    rebaseTransactions(it, newTip);
                }

                return true;
            } else {
                newTip = MoreObjects.firstNonNull(e.cohort.getCandidate(), newTip);
            }
        }

        LOG.debug("{}: aborted transaction {} not found in the queue", logContext, cohort.getIdentifier());
        return true;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void rebaseTransactions(final Iterator<CommitEntry> iter, final @NonNull DataTreeTip newTip) {
        tip = requireNonNull(newTip);
        while (iter.hasNext()) {
            final SimpleShardDataTreeCohort cohort = iter.next().cohort;
            if (cohort.getState() == State.CAN_COMMIT_COMPLETE) {
                LOG.debug("{}: Revalidating queued transaction {}", logContext, cohort.getIdentifier());

                try {
                    tip.validate(cohort.getDataTreeModification());
                } catch (DataValidationFailedException | RuntimeException e) {
                    LOG.debug("{}: Failed to revalidate queued transaction {}", logContext, cohort.getIdentifier(), e);
                    cohort.reportFailure(e);
                }
            } else if (cohort.getState() == State.PRE_COMMIT_COMPLETE) {
                LOG.debug("{}: Repreparing queued transaction {}", logContext, cohort.getIdentifier());

                try {
                    tip.validate(cohort.getDataTreeModification());
                    DataTreeCandidateTip candidate = tip.prepare(cohort.getDataTreeModification());

                    cohort.setNewCandidate(candidate);
                    tip = candidate;
                } catch (RuntimeException | DataValidationFailedException e) {
                    LOG.debug("{}: Failed to reprepare queued transaction {}", logContext, cohort.getIdentifier(), e);
                    cohort.reportFailure(e);
                }
            }
        }
    }

    void setRunOnPendingTransactionsComplete(final Runnable operation) {
        runOnPendingTransactionsComplete = operation;
        maybeRunOperationOnPendingTransactionsComplete();
    }

    private void maybeRunOperationOnPendingTransactionsComplete() {
        if (runOnPendingTransactionsComplete != null && !anyPendingTransactions()) {
            LOG.debug("{}: Pending transactions complete - running operation {}", logContext,
                    runOnPendingTransactionsComplete);

            runOnPendingTransactionsComplete.run();
            runOnPendingTransactionsComplete = null;
        }
    }

    ShardStats getStats() {
        return shard.getShardMBean();
    }

    Iterator<SimpleShardDataTreeCohort> cohortIterator() {
        return Iterables.transform(Iterables.concat(pendingFinishCommits, pendingCommits, pendingTransactions),
            e -> e.cohort).iterator();
    }

    void removeTransactionChain(final LocalHistoryIdentifier id) {
        if (transactionChains.remove(id) != null) {
            LOG.debug("{}: Removed transaction chain {}", logContext, id);
        }
    }
}
