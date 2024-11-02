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
import static java.util.Objects.requireNonNullElse;
import static org.apache.pekko.actor.ActorRef.noSender;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.CommitCohort.State;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.CohortRegistryCommand;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.ReusableNormalizedNodePruner;
import org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload;
import org.opendaylight.controller.cluster.datastore.persisted.CloseLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CreateLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.PayloadVersion;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.persisted.SkipTransactionsPayload;
import org.opendaylight.controller.cluster.datastore.utils.DataTreeModificationOutput;
import org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.data.tree.api.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Internal shard state, similar to a DOMStore, but optimized for use in the actor system, e.g. it does not expose
 * public interfaces and assumes it is only ever called from a single thread.
 *
 * <p>This class is not part of the API contract and is subject to change at any time. It is NOT thread-safe.
 */
// non-final for mocking
@VisibleForTesting
public class ShardDataTree extends ShardDataTreeTransactionParent {
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

    private final Deque<CommitCohort> readyTransactions = new ArrayDeque<>();
    private final Deque<CommitCohort> pendingCanCommit = new ArrayDeque<>();
    private final Deque<CommitCohort> pendingPreCommit = new ArrayDeque<>();
    private final Queue<CommitCohort> pendingCommits = new ArrayDeque<>();
    private final Queue<CommitCohort> pendingFinishCommits = new ArrayDeque<>();

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

    private EffectiveModelContext schemaContext;
    private DataSchemaContextTree dataSchemaContext;

    private int currentTransactionBatch;

    ShardDataTree(final Shard shard, final EffectiveModelContext schemaContext, final DataTree dataTree,
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

    ShardDataTree(final Shard shard, final EffectiveModelContext schemaContext, final TreeType treeType,
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
    public ShardDataTree(final Shard shard, final EffectiveModelContext schemaContext, final TreeType treeType) {
        this(shard, schemaContext, treeType, YangInstanceIdentifier.of(),
                new DefaultShardDataTreeChangeListenerPublisher(""), "");
    }

    final String logContext() {
        return logContext;
    }

    final long readTime() {
        return shard.ticker().read();
    }

    final DataTree getDataTree() {
        return dataTree;
    }

    @VisibleForTesting
    final EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    final void updateSchemaContext(final @NonNull EffectiveModelContext newSchemaContext) {
        dataTree.setEffectiveModelContext(newSchemaContext);
        schemaContext = newSchemaContext;
        dataSchemaContext = DataSchemaContextTree.from(newSchemaContext);
    }

    final void resetTransactionBatch() {
        currentTransactionBatch = 0;
    }

    /**
     * Take a snapshot of current state for later recovery.
     *
     * @return A state snapshot
     */
    @NonNull ShardDataTreeSnapshot takeStateSnapshot() {
        final var rootNode = takeSnapshot().readNode(YangInstanceIdentifier.of()).orElseThrow();
        final var metaBuilder =
            ImmutableMap.<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>>builder();

        for (var meta : metadata) {
            final var snapshot = meta.toSnapshot();
            if (snapshot != null) {
                metaBuilder.put(snapshot.getType(), snapshot);
            }
        }

        return new MetadataShardDataTreeSnapshot(rootNode, metaBuilder.build());
    }

    private boolean anyPendingTransactions() {
        return !readyTransactions.isEmpty() || !pendingCanCommit.isEmpty() || !pendingPreCommit.isEmpty()
            || !pendingCommits.isEmpty() || !pendingFinishCommits.isEmpty();
    }

    private void applySnapshot(final @NonNull ShardDataTreeSnapshot snapshot,
            final UnaryOperator<DataTreeModification> wrapper) throws DataValidationFailedException {
        final Stopwatch elapsed = Stopwatch.createStarted();

        if (anyPendingTransactions()) {
            LOG.warn("{}: applying state snapshot with pending transactions", logContext);
        }

        final var snapshotMeta = snapshot instanceof MetadataShardDataTreeSnapshot ms ? ms.getMetadata()
            : Map.<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>>of();

        for (var m : metadata) {
            final var s = snapshotMeta.get(m.getSupportedType());
            if (s != null) {
                m.applySnapshot(s);
            } else {
                m.reset();
            }
        }

        final DataTreeModification unwrapped = newModification();
        final DataTreeModification mod = wrapper.apply(unwrapped);
        // delete everything first
        mod.delete(YangInstanceIdentifier.of());

        snapshot.getRootNode().ifPresent(rootNode -> {
            // Add everything from the remote node back
            mod.write(YangInstanceIdentifier.of(), rootNode);
        });

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
    final void applySnapshot(final @NonNull ShardDataTreeSnapshot snapshot) throws DataValidationFailedException {
        // TODO: we should be taking ShardSnapshotState here and performing forward-compatibility translation
        applySnapshot(snapshot, UnaryOperator.identity());
    }

    /**
     * Apply a snapshot coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param snapshot Snapshot that needs to be applied
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    final void applyRecoverySnapshot(final @NonNull ShardSnapshotState snapshot) throws DataValidationFailedException {
        // TODO: we should be able to reuse the pruner, provided we are not reentrant
        final ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forDataSchemaContext(
            dataSchemaContext);
        if (snapshot.needsMigration()) {
            final ReusableNormalizedNodePruner uintPruner = pruner.withUintAdaption();
            applySnapshot(snapshot.getSnapshot(),
                delegate -> new PruningDataTreeModification.Proactive(delegate, dataTree, uintPruner));
        } else {
            applySnapshot(snapshot.getSnapshot(),
                delegate -> new PruningDataTreeModification.Reactive(delegate, dataTree, pruner));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void applyRecoveryCandidate(final CommitTransactionPayload payload) throws IOException {
        final var entry = payload.acquireCandidate();
        final var unwrapped = newModification();
        final var pruningMod = createPruningModification(unwrapped,
            NormalizedNodeStreamVersion.MAGNESIUM.compareTo(entry.streamVersion()) > 0);

        DataTreeCandidates.applyToModification(pruningMod, entry.candidate());
        pruningMod.ready();
        LOG.trace("{}: Applying recovery modification {}", logContext, unwrapped);

        try {
            dataTree.validate(unwrapped);
            dataTree.commit(dataTree.prepare(unwrapped));
        } catch (Exception e) {
            final var file = new File(System.getProperty("karaf.data", "."),
                    "failed-recovery-payload-" + logContext + ".out");
            DataTreeModificationOutput.toFile(file, unwrapped);
            throw new IllegalStateException(
                "%s: Failed to apply recovery payload. Modification data was written to file %s".formatted(
                    logContext, file),
                e);
        }

        allMetadataCommittedTransaction(entry.transactionId());
    }

    private PruningDataTreeModification createPruningModification(final DataTreeModification unwrapped,
            final boolean uintAdapting) {
        // TODO: we should be able to reuse the pruner, provided we are not reentrant
        final var pruner = ReusableNormalizedNodePruner.forDataSchemaContext(dataSchemaContext);
        return uintAdapting ? new PruningDataTreeModification.Proactive(unwrapped, dataTree, pruner.withUintAdaption())
                : new PruningDataTreeModification.Reactive(unwrapped, dataTree, pruner);
    }

    /**
     * Apply a payload coming from recovery. This method does not assume the SchemaContexts match and performs data
     * pruning in an attempt to adjust the state to our current SchemaContext.
     *
     * @param payload Payload
     * @throws IOException when the snapshot fails to deserialize
     * @throws DataValidationFailedException when the snapshot fails to apply
     */
    final void applyRecoveryPayload(final @NonNull Payload payload) throws IOException {
        switch (payload) {
            case CommitTransactionPayload commit -> applyRecoveryCandidate(commit);
            case AbortTransactionPayload abort -> allMetadataAbortedTransaction(abort.getIdentifier());
            case PurgeTransactionPayload purge -> allMetadataPurgedTransaction(purge.getIdentifier());
            case CreateLocalHistoryPayload create -> allMetadataCreatedLocalHistory(create.getIdentifier());
            case CloseLocalHistoryPayload close -> allMetadataClosedLocalHistory(close.getIdentifier());
            case PurgeLocalHistoryPayload purge -> allMetadataPurgedLocalHistory(purge.getIdentifier());
            case SkipTransactionsPayload skip -> allMetadataSkipTransactions(skip);
            default -> LOG.debug("{}: ignoring unhandled payload {}", logContext, payload);
        }
    }

    private void applyReplicatedCandidate(final CommitTransactionPayload payload)
            throws DataValidationFailedException, IOException {
        final var payloadCandidate = payload.acquireCandidate();
        final var transactionId = payloadCandidate.transactionId();
        LOG.debug("{}: Applying foreign transaction {}", logContext, transactionId);

        final var mod = newModification();
        // TODO: check version here, which will enable us to perform forward-compatibility transformations
        DataTreeCandidates.applyToModification(mod, payloadCandidate.candidate());
        mod.ready();

        LOG.trace("{}: Applying foreign modification {}", logContext, mod);
        dataTree.validate(mod);
        final var candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        allMetadataCommittedTransaction(transactionId);
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
    final void applyReplicatedPayload(final Identifier identifier, final Payload payload) throws IOException,
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
        switch (payload) {
            case CommitTransactionPayload commit -> {
                if (identifier == null) {
                    applyReplicatedCandidate(commit);
                } else {
                    verify(identifier instanceof TransactionIdentifier);
                    // if we did not track this transaction before, it means that it came from another leader and we are
                    // in the process of commiting it while in PreLeader state. That means that it hasnt yet been
                    // committed to the local DataTree and would be lost if it was only applied via
                    // payloadReplicationComplete().
                    if (!payloadReplicationComplete((TransactionIdentifier) identifier)) {
                        applyReplicatedCandidate(commit);
                    }
                }

                // make sure acquireCandidate() is the last call touching the payload data as we want it to be GC-ed.
                checkRootOverwrite(commit.acquireCandidate().candidate());
            }
            case AbortTransactionPayload abort -> {
                if (identifier != null) {
                    payloadReplicationComplete(abort);
                }
                allMetadataAbortedTransaction(abort.getIdentifier());
            }
            case PurgeTransactionPayload purge -> {
                if (identifier != null) {
                    payloadReplicationComplete(purge);
                }
                allMetadataPurgedTransaction(purge.getIdentifier());
            }
            case CloseLocalHistoryPayload close -> {
                if (identifier != null) {
                    payloadReplicationComplete(close);
                }
                allMetadataClosedLocalHistory(close.getIdentifier());
            }
            case CreateLocalHistoryPayload create -> {
                if (identifier != null) {
                    payloadReplicationComplete(create);
                }
                allMetadataCreatedLocalHistory(create.getIdentifier());
            }
            case PurgeLocalHistoryPayload purge -> {
                if (identifier != null) {
                    payloadReplicationComplete(purge);
                }
                allMetadataPurgedLocalHistory(purge.getIdentifier());
            }
            case SkipTransactionsPayload skip -> {
                if (identifier != null) {
                    payloadReplicationComplete(skip);
                }
                allMetadataSkipTransactions(skip);
            }
            default -> LOG.warn("{}: ignoring unhandled identifier {} payload {}", logContext, identifier, payload);
        }
    }

    private void checkRootOverwrite(final DataTreeCandidate candidate) {
        final DatastoreContext datastoreContext = shard.getDatastoreContext();
        if (!datastoreContext.isSnapshotOnRootOverwrite()) {
            return;
        }

        if (!datastoreContext.isPersistent()) {
            // FIXME: why don't we want a snapshot in non-persistent state?
            return;
        }

        // top level container ie "/"
        if (candidate.getRootPath().isEmpty() && candidate.getRootNode().modificationType() == ModificationType.WRITE) {
            LOG.debug("{}: shard root overwritten, enqueuing snapshot", logContext);
            shard.self().tell(new InitiateCaptureSnapshot(), noSender());
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

    private boolean payloadReplicationComplete(final TransactionIdentifier txId) {
        final var current = pendingFinishCommits.peek();
        if (current == null) {
            LOG.warn("{}: No outstanding transactions, ignoring consensus on transaction {}", logContext, txId);
            allMetadataCommittedTransaction(txId);
            return false;
        }

        final var cohortTxId = current.transactionId();
        if (!cohortTxId.equals(txId)) {
            LOG.debug("{}: Head of pendingFinishCommits queue is {}, ignoring consensus on transaction {}", logContext,
                cohortTxId, txId);
            allMetadataCommittedTransaction(txId);
            return false;
        }

        finishCommit(current);
        return true;
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

    private void allMetadataSkipTransactions(final SkipTransactionsPayload payload) {
        final var historyId = payload.getIdentifier();
        final var txIds = payload.getTransactionIds();
        for (ShardDataTreeMetadata<?> m : metadata) {
            m.onTransactionsSkipped(historyId, txIds);
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
    final ShardDataTreeTransactionChain recreateTransactionChain(final LocalHistoryIdentifier historyId,
            final boolean closed) {
        final ShardDataTreeTransactionChain ret = new ShardDataTreeTransactionChain(historyId, this);
        final ShardDataTreeTransactionChain existing = transactionChains.putIfAbsent(historyId, ret);
        checkState(existing == null, "Attempted to recreate chain %s, but %s already exists", historyId, existing);
        return ret;
    }

    final ShardDataTreeTransactionChain ensureTransactionChain(final LocalHistoryIdentifier historyId,
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

    final @NonNull ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        getStats().incrementReadOnlyTransactionCount();

        final var historyId = txId.getHistoryId();
        return historyId.getHistoryId() == 0 ? newStandaloneReadOnlyTransaction(txId)
            : ensureTransactionChain(historyId, null).newReadOnlyTransaction(txId);
    }

    final @NonNull ReadOnlyShardDataTreeTransaction newStandaloneReadOnlyTransaction(final TransactionIdentifier txId) {
        return new ReadOnlyShardDataTreeTransaction(this, txId, takeSnapshot());
    }

    final @NonNull ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        getStats().incrementReadWriteTransactionCount();

        final var historyId = txId.getHistoryId();
        return historyId.getHistoryId() == 0 ? newStandaloneReadWriteTransaction(txId)
            : ensureTransactionChain(historyId, null).newReadWriteTransaction(txId);
    }

    final @NonNull ReadWriteShardDataTreeTransaction newStandaloneReadWriteTransaction(
            final TransactionIdentifier txId) {
        return new ReadWriteShardDataTreeTransaction(this, txId, newModification());
    }

    @VisibleForTesting
    final void notifyListeners(final DataTreeCandidate candidate) {
        treeChangeListenerPublisher.publishChanges(candidate);
    }

    /**
     * Immediately purge all state relevant to leader. This includes all transaction chains and any scheduled
     * replication callbacks.
     */
    final void purgeLeaderState() {
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
    final void closeTransactionChain(final LocalHistoryIdentifier id, final @Nullable Runnable callback) {
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
    final void closeTransactionChain(final LocalHistoryIdentifier id) {
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
    final void purgeTransactionChain(final LocalHistoryIdentifier id, final @Nullable Runnable callback) {
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

    final void skipTransactions(final LocalHistoryIdentifier id, final ImmutableUnsignedLongSet transactionIds,
            final Runnable callback) {
        final ShardDataTreeTransactionChain chain = transactionChains.get(id);
        if (chain == null) {
            LOG.debug("{}: Skipping on non-existent transaction chain {}", logContext, id);
            if (callback != null) {
                callback.run();
            }
            return;
        }

        replicatePayload(id, SkipTransactionsPayload.create(id, transactionIds,
            shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    final Optional<DataTreeCandidate> readCurrentData() {
        return readNode(YangInstanceIdentifier.of())
            .map(state -> DataTreeCandidates.fromNormalizedNode(YangInstanceIdentifier.of(), state));
    }

    final void registerTreeChangeListener(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
            final Optional<DataTreeCandidate> initialState, final Consumer<Registration> onRegistration) {
        treeChangeListenerPublisher.registerTreeChangeListener(path, listener, initialState, onRegistration);
    }

    final int getQueueSize() {
        return readyTransactions.size() + pendingCanCommit.size() + pendingPreCommit.size() + pendingCommits.size()
            + pendingFinishCommits.size();
    }

    final void enqueueReadyTransaction(final @NonNull CommitCohort cohort) {
        cohort.setLastAccess(readTime());
        pendingCanCommit.add(cohort);
    }

    @Override
    final void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        final TransactionIdentifier id = transaction.getIdentifier();
        LOG.debug("{}: aborting transaction {}", logContext, id);
        replicatePayload(id, AbortTransactionPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    @Override
    final void abortFromTransactionActor(final AbstractShardDataTreeTransaction<?> transaction) {
        // No-op for free-standing transactions
    }

    @Override
    final SimpleCommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction,
            final Optional<SortedSet<String>> participatingShardNames) {
        final var userCohorts = finishTransaction(transaction);
        final var cohort = new SimpleCommitCohort(this, transaction, userCohorts, participatingShardNames);
        enqueueReadyTransaction(cohort);
        return cohort;
    }

    final CompositeDataTreeCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        final var snapshot = transaction.getSnapshot();
        final var txId = transaction.getIdentifier();
        LOG.debug("{}: readying transaction {}", logContext, txId);
        snapshot.ready();
        LOG.debug("{}: transaction {} ready", logContext, txId);
        return newUserCohorts(txId);
    }

    final CompositeDataTreeCohort newUserCohorts(final TransactionIdentifier txId) {
        return cohortRegistry.createCohort(schemaContext, txId, shard::executeInSelf, COMMIT_STEP_TIMEOUT);
    }

    final void purgeTransaction(final TransactionIdentifier id, final Runnable callback) {
        LOG.debug("{}: purging transaction {}", logContext, id);
        replicatePayload(id, PurgeTransactionPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    @VisibleForTesting
    public final Optional<NormalizedNode> readNode(final YangInstanceIdentifier path) {
        return takeSnapshot().readNode(path);
    }

    final DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }

    @VisibleForTesting
    final DataTreeModification newModification() {
        return takeSnapshot().newModification();
    }

    final Collection<CommitCohort> getAndClearPendingTransactions() {
        final var ret = new ArrayList<CommitCohort>(getQueueSize());

        for (var entry : pendingFinishCommits) {
            ret.add(entry);
        }
        for (var entry : pendingCommits) {
            ret.add(entry);
        }
        for (var entry : pendingPreCommit) {
            ret.add(entry);
        }
        for (var entry : pendingCanCommit) {
            ret.add(entry);
        }
        for (var entry : readyTransactions) {
            ret.add(entry);
        }

        pendingFinishCommits.clear();
        pendingCommits.clear();
        pendingPreCommit.clear();
        pendingCanCommit.clear();
        readyTransactions.clear();
        tip = dataTree;
        return ret;
    }

    /**
     * Called some time after {@link #processNextPendingTransaction()} decides to stop processing.
     */
    final void resumeNextPendingTransaction() {
        LOG.debug("{}: attempting to resume transaction processing", logContext);
        processNextPending();
    }

    private void processNextPendingTransaction() {
        ++currentTransactionBatch;
        if (currentTransactionBatch > MAX_TRANSACTION_BATCH) {
            LOG.debug("{}: Already processed {}, scheduling continuation", logContext, currentTransactionBatch);
            shard.scheduleNextPendingTransaction();
            return;
        }

        final var entry = findFirstEntry(pendingTransactions, State.CAN_COMMIT_PENDING);
        try {
            if (entry != null) {
                canCommitEntry(entry);
            }
        } finally {
            maybeRunOperationOnPendingTransactionsComplete();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void canCommitEntry(final CommitCohort cohort) {
        final var modification = cohort.getDataTreeModification();

        LOG.debug("{}: Validating transaction {}", logContext, cohort.transactionId());
        final Exception cause;
        try {
            tip.validate(modification);
            LOG.debug("{}: Transaction {} validated", logContext, cohort.transactionId());
            cohort.successfulCanCommit();

            return;
        } catch (ConflictingModificationAppliedException e) {
            LOG.warn("{}: Store Tx {}: Conflicting modification for path {}.", logContext, cohort.transactionId(),
                e.getPath());
            cause = new OptimisticLockFailedException("Optimistic lock failed for path " + e.getPath(), e);
        } catch (DataValidationFailedException e) {
            LOG.warn("{}: Store Tx {}: Data validation failed for path {}.", logContext, cohort.transactionId(),
                e.getPath(), e);

            // For debugging purposes, allow dumping of the modification. Coupled with the above
            // precondition log, it should allow us to understand what went on.
            LOG.debug("{}: Store Tx {}: modifications: {}", logContext, cohort.transactionId(), modification);
            LOG.trace("{}: Current tree: {}", logContext, dataTree);
            cause = new TransactionCommitFailedException("Data did not pass validation for path " + e.getPath(), e);
        } catch (Exception e) {
            LOG.warn("{}: Unexpected failure in validation phase", logContext, e);
            cause = e;
        } finally {
            cohort.setLastAccess(readTime());
        }

        // Failure path: propagate the failure, remove the transaction from the queue and loop to the next one
        cohort.failedCanCommit(cause);
    }

    private void processNextPending() {
        processNextPendingCommit();
        processNextPendingTransaction();
    }


    private @Nullable CommitCohort findFirstEntry(final Queue<CommitCohort> queue, final State allowedState) {
        while (true) {
            final var entry = queue.peek();
            if (entry == null) {
                // Empty queue
                return null;
            }

            if (entry.isFailed()) {
                LOG.debug("{}: Removing failed transaction {}", logContext, entry.transactionId());
                queue.remove();
                continue;
            }

            return entry.getState() == allowedState ? entry : null;
        }
    }

    private void processNextPendingCommit() {
        final var entry = findFirstEntry(pendingCommits, State.COMMIT_PENDING);
        try {
            if (entry != null) {
                startCommit(entry, entry.getCandidate());
            }
        } finally {
            maybeRunOperationOnPendingTransactionsComplete();
        }
    }

    private boolean peekNextPendingCommit() {
        final var first = pendingCommits.peek();
        return first != null && first.getState() == State.COMMIT_PENDING;
    }

    // non-final for mocking
    void startCanCommit(final CommitCohort cohort) {
        final var head = pendingTransactions.peek();
        if (head == null) {
            LOG.warn("{}: No transactions enqueued while attempting to start canCommit on {}", logContext, cohort);
            return;
        }
        if (!cohort.equals(head)) {
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
                LOG.debug("{}: Tx {} is scheduled for canCommit step", logContext, cohort.transactionId());
                return;
            }

            LOG.debug("{}: Evaluating tx {} for canCommit -  preceding participating shard names {}",
                    logContext, cohort.transactionId(), precedingShardNames);
            final var iter = pendingTransactions.iterator();
            int index = -1;
            int moveToIndex = -1;
            while (iter.hasNext()) {
                final var entry = iter.next();
                ++index;

                if (cohort.equals(entry)) {
                    if (moveToIndex < 0) {
                        LOG.debug("{}: Not moving tx {} - cannot proceed with canCommit",
                                logContext, cohort.transactionId());
                        return;
                    }

                    LOG.debug("{}: Moving {} to index {} in the pendingTransactions queue",
                            logContext, cohort.transactionId(), moveToIndex);
                    iter.remove();
                    insertEntry(pendingTransactions, entry, moveToIndex);

                    if (!cohort.equals(pendingTransactions.peek())) {
                        LOG.debug("{}: Tx {} is not at the head of the queue - cannot proceed with canCommit",
                                logContext, cohort.transactionId());
                        return;
                    }

                    LOG.debug("{}: Tx {} is now at the head of the queue - proceeding with canCommit",
                            logContext, cohort.transactionId());
                    break;
                }

                if (entry.getState() != State.READY) {
                    LOG.debug("{}: Skipping pending transaction {} in state {}",
                            logContext, entry.transactionId(), entry.getState());
                    continue;
                }

                final var pendingPrecedingShardNames = extractPrecedingShardNames(entry.getParticipatingShardNames());
                if (precedingShardNames.equals(pendingPrecedingShardNames)) {
                    if (moveToIndex < 0) {
                        LOG.debug("{}: Preceding shard names {} for pending tx {} match - saving moveToIndex {}",
                                logContext, pendingPrecedingShardNames, entry.transactionId(), index);
                        moveToIndex = index;
                    } else {
                        LOG.debug(
                            "{}: Preceding shard names {} for pending tx {} match but moveToIndex already set to {}",
                            logContext, pendingPrecedingShardNames, entry.transactionId(), moveToIndex);
                    }
                } else {
                    LOG.debug("{}: Preceding shard names {} for pending tx {} differ - skipping",
                        logContext, pendingPrecedingShardNames, entry.transactionId());
                }
            }
        }

        processNextPendingTransaction();
    }

    private static void insertEntry(final Deque<CommitCohort> queue, final CommitCohort entry, final int atIndex) {
        if (atIndex == 0) {
            queue.addFirst(entry);
            return;
        }

        LOG.trace("Inserting into Deque at index {}", atIndex);

        final var tempStack = new ArrayDeque<CommitCohort>(atIndex);
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

    // non-final for mocking
    @SuppressWarnings("checkstyle:IllegalCatch")
    void startPreCommit(final CommitCohort cohort) {
        final var start = pendingPreCommit.isEmpty();
        pendingPreCommit.addLast(cohort);
        if (start) {
            doStartPreCommit(cohort);
        } else {
            LOG.debug("{}: deferring prepare of {}", logContext, cohort.transactionId());
            return;
        }
    }

    private void doStartPreCommit(final CommitCohort cohort) {
        final var txId = cohort.transactionId();
        LOG.debug("{}: Preparing transaction {}", logContext, txId);

        final DataTreeCandidateTip candidate;
        try {
            candidate = tip.prepare(cohort.getDataTreeModification());
            LOG.debug("{}: Transaction {} candidate ready", logContext, txId);
        } catch (DataValidationFailedException | RuntimeException e) {
            failPreCommit(e);
            return;
        }

        cohort.userPreCommit(candidate, new FutureCallback<>() {
            @Override
            public void onSuccess(final DataTreeCandidateTip result) {
                onPreCommitComplete(result);
            }

            @Override
            public void onFailure(final Throwable failure) {
                failPreCommit(failure);
            }
        });
    }

    private void failPreCommit(final Throwable cause) {
        // Note: preCommit() is sequential, hence the remove() is safe here
        pendingPreCommit.remove().failedPreCommit(cause);
        onPreCommitComplete();
    }

    private void onPreCommitComplete(final DataTreeCandidateTip candidate) {
        final var cohort = pendingPreCommit.remove();
        cohort.setLastAccess(readTime());

        // Set the tip of the data tree.
        tip = verifyNotNull(candidate);
        pendingCommits.add(cohort);
        LOG.debug("{}: Transaction {} prepared", logContext, cohort.transactionId());

        cohort.successfulPreCommit(candidate);
        onPreCommitComplete();
    }

    private void onPreCommitComplete() {
        final var nextPreCommit = pendingPreCommit.peek();
        if (nextPreCommit != null) {
            // FIXME: permit for next transaction
            doStartPreCommit(nextPreCommit);
        } else {
            processNextPendingTransaction();
        }
    }

    private void failCommit(final Exception cause) {
        pendingFinishCommits.remove().failedCommit(cause);
        processNextPending();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void finishCommit(final CommitCohort cohort) {
        final var txId = cohort.transactionId();
        final var candidate = cohort.getCandidate();

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

        // FIXME: propagate journal index
        pendingFinishCommits.poll().successfulCommit(UnsignedLong.ZERO, () -> {
            LOG.trace("{}: Transaction {} committed, proceeding to notify", logContext, txId);
            notifyListeners(candidate);

            processNextPending();
        });
    }

    // non-final for mocking
    void startCommit(final CommitCohort cohort, final DataTreeCandidate candidate) {
        final var current = pendingCommits.peek();
        checkState(current != null, "Attempted to start commit of %s when no transactions pending", cohort);

        if (!cohort.equals(current)) {
            LOG.debug("{}: Transaction {} scheduled for commit step", logContext, cohort.transactionId());
            return;
        }

        LOG.debug("{}: Starting commit for transaction {}", logContext, current.transactionId());

        final TransactionIdentifier txId = cohort.transactionId();
        final Payload payload;
        try {
            payload = CommitTransactionPayload.create(txId, candidate, PayloadVersion.current(),
                    shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity());
        } catch (IOException e) {
            LOG.error("{}: Failed to encode transaction {} candidate {}", logContext, txId, candidate, e);
            pendingCommits.poll().failedCommit(e);
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
        pendingFinishCommits.add(pendingCommits.remove());

        // See if the next transaction is pending commit (ie in the COMMIT_PENDING state) so it can be batched with
        // this transaction for replication.
        boolean replicationBatchHint = peekNextPendingCommit();

        // Once completed, we will continue via payloadReplicationComplete
        shard.persistPayload(txId, payload, replicationBatchHint);

        current.setLastAccess(readTime());

        LOG.debug("{}: Transaction {} submitted to persistence", logContext, txId);

        // Process the next transaction pending commit, if any. If there is one it will be batched with this
        // transaction for replication.
        processNextPendingCommit();
    }

    final Collection<ActorRef> getCohortActors() {
        return cohortRegistry.getCohortActors();
    }

    final void processCohortRegistryCommand(final ActorRef sender, final CohortRegistryCommand message) {
        cohortRegistry.process(sender, message);
    }

    @Override
    final SimpleCommitCohort createFailedCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Exception failure) {
        final var cohort = new SimpleCommitCohort(this, mod, txId, failure);
        enqueueReadyTransaction(cohort);
        return cohort;
    }

    @Override
    final SimpleCommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Optional<SortedSet<String>> participatingShardNames) {
        final var cohort = new SimpleCommitCohort(this, mod, txId, newUserCohorts(txId),
            participatingShardNames);
        enqueueReadyTransaction(cohort);
        return cohort;
    }

    @SuppressFBWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES", justification = "See inline comments below.")
    final void checkForExpiredTransactions(final long transactionCommitTimeoutMillis,
            final Function<CommitCohort, OptionalLong> accessTimeUpdater) {
        final long timeout = TimeUnit.MILLISECONDS.toNanos(transactionCommitTimeoutMillis);
        final long now = readTime();

        final var currentQueue = !pendingFinishCommits.isEmpty() ? pendingFinishCommits :
            !pendingCommits.isEmpty() ? pendingCommits : pendingTransactions;
        final var currentTx = currentQueue.peek();
        if (currentTx == null) {
            // Empty queue, no-op
            return;
        }

        long delta = now - currentTx.lastAccess();
        if (delta < timeout) {
            // Not expired yet, bail
            return;
        }

        final var updateOpt = accessTimeUpdater.apply(currentTx);
        if (updateOpt.isPresent()) {
            final long newAccess =  updateOpt.orElseThrow();
            final long newDelta = now - newAccess;
            if (newDelta < delta) {
                LOG.debug("{}: Updated current transaction {} access time", logContext, currentTx.transactionId());
                currentTx.setLastAccess(newAccess);
                delta = newDelta;
            }

            if (delta < timeout) {
                // Not expired yet, bail
                return;
            }
        }

        final long deltaMillis = TimeUnit.NANOSECONDS.toMillis(delta);
        final var state = currentTx.getState();

        LOG.warn("{}: Current transaction {} has timed out after {} ms in state {}", logContext,
            currentTx.transactionId(), deltaMillis, state);
        boolean processNext = true;
        final TimeoutException cohortFailure = new TimeoutException("Backend timeout in state " + state + " after "
                + deltaMillis + "ms");

        switch (state) {
            case CAN_COMMIT_PENDING:
                currentQueue.remove().failedCanCommit(cohortFailure);
                break;
            case CAN_COMMIT_COMPLETE:
                // The suppression of the FindBugs "DB_DUPLICATE_SWITCH_CLAUSES" warning pertains to this clause
                // whose code is duplicated with PRE_COMMIT_COMPLETE. The clauses aren't combined in case the code
                // in PRE_COMMIT_COMPLETE is changed.
                currentQueue.remove().reportFailure(cohortFailure);
                break;
            case PRE_COMMIT_PENDING:
                currentQueue.remove().failedPreCommit(cohortFailure);
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
                currentQueue.remove().reportFailure(cohortFailure);
                break;
            case COMMIT_PENDING:
                LOG.warn("{}: Transaction {} is still committing, cannot abort", logContext, currentTx.transactionId());
                currentTx.setLastAccess(now);
                processNext = false;
                return;
            case READY:
                currentQueue.remove().reportFailure(cohortFailure);
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

    // non-final for mocking
    boolean startAbort(final CommitCohort cohort) {
        final var it = Iterables.concat(pendingFinishCommits, pendingCommits, pendingTransactions).iterator();
        if (!it.hasNext()) {
            LOG.debug("{}: no open transaction while attempting to abort {}", logContext, cohort.transactionId());
            return true;
        }

        // First entry is special, as it may already be committing
        final var first = it.next();
        if (cohort.equals(first)) {
            if (cohort.getState() != State.COMMIT_PENDING) {
                LOG.debug("{}: aborting head of queue {} in state {}", logContext, cohort.transactionId(),
                    cohort.transactionId());

                it.remove();
                if (cohort.getCandidate() != null) {
                    rebaseTransactions(it, dataTree);
                }

                processNextPending();
                return true;
            }

            LOG.warn("{}: transaction {} is committing, skipping abort", logContext, cohort.transactionId());
            return false;
        }

        var newTip = requireNonNullElse(first.getCandidate(), dataTree);
        while (it.hasNext()) {
            final var entry = it.next();
            if (cohort.equals(entry)) {
                LOG.debug("{}: aborting queued transaction {}", logContext, cohort.transactionId());
                getStats().incrementAbortTransactionsCount();

                it.remove();
                if (cohort.getCandidate() != null) {
                    rebaseTransactions(it, newTip);
                }

                return true;
            }

            newTip = requireNonNullElse(entry.getCandidate(), newTip);
        }

        LOG.debug("{}: aborted transaction {} not found in the queue", logContext, cohort.transactionId());
        return true;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void rebaseTransactions(final Iterator<CommitCohort> iter, final @NonNull DataTreeTip newTip) {
        tip = requireNonNull(newTip);
        while (iter.hasNext()) {
            final var cohort = iter.next();
            if (cohort.getState() == State.CAN_COMMIT_COMPLETE) {
                LOG.debug("{}: Revalidating queued transaction {}", logContext, cohort.transactionId());

                try {
                    tip.validate(cohort.getDataTreeModification());
                } catch (DataValidationFailedException | RuntimeException e) {
                    LOG.debug("{}: Failed to revalidate queued transaction {}", logContext, cohort.transactionId(), e);
                    cohort.reportFailure(e);
                }
            } else if (cohort.getState() == State.PRE_COMMIT_COMPLETE) {
                LOG.debug("{}: Repreparing queued transaction {}", logContext, cohort.transactionId());

                try {
                    tip.validate(cohort.getDataTreeModification());
                    DataTreeCandidateTip candidate = tip.prepare(cohort.getDataTreeModification());

                    cohort.setNewCandidate(candidate);
                    tip = candidate;
                } catch (RuntimeException | DataValidationFailedException e) {
                    LOG.debug("{}: Failed to reprepare queued transaction {}", logContext, cohort.transactionId(), e);
                    cohort.reportFailure(e);
                }
            }
        }
    }

    final void setRunOnPendingTransactionsComplete(final Runnable operation) {
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

    // Non-final for mocking
    ShardStats getStats() {
        return shard.shardStats();
    }

    final void retire(final ClientIdentifier clientId) {
        final var it = Iterables.concat(pendingFinishCommits, pendingCommits, pendingPreCommit, pendingCanCommit,
            readyTransactions).iterator();
        while (it.hasNext()) {
            final var cohort = it.next();
            final var transactionId = cohort.transactionId();
            if (clientId.equals(transactionId.getHistoryId().getClientId())) {
                if (cohort.getState() != State.COMMIT_PENDING) {
                    LOG.debug("{}: Retiring transaction {}", logContext, transactionId);
                    it.remove();
                } else {
                    LOG.debug("{}: Transaction {} already committing, not retiring it", logContext, transactionId);
                }
            }
        }
    }

    final void removeTransactionChain(final LocalHistoryIdentifier id) {
        if (transactionChains.remove(id) != null) {
            LOG.debug("{}: Removed transaction chain {}", logContext, id);
        }
    }
}
