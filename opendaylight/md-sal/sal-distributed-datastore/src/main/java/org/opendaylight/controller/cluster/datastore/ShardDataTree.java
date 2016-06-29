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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.CohortRegistryCommand;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateSupplier;
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
    private static final Timeout COMMIT_STEP_TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTree.class);

    private final Map<LocalHistoryIdentifier, ShardDataTreeTransactionChain> transactionChains = new HashMap<>();
    private final DataTreeCohortActorRegistry cohortRegistry = new DataTreeCohortActorRegistry();
    private final Queue<SimpleShardDataTreeCohort> pendingTransactions = new ArrayDeque<>();
    private final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher;
    private final ShardDataChangeListenerPublisher dataChangeListenerPublisher;
    private final TipProducingDataTree dataTree;
    private final String logContext;
    private final Shard shard;

    private SchemaContext schemaContext;

    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType,
            final ShardDataTreeChangeListenerPublisher treeChangeListenerPublisher,
            final ShardDataChangeListenerPublisher dataChangeListenerPublisher, final String logContext) {
        dataTree = InMemoryDataTreeFactory.getInstance().create(treeType);
        updateSchemaContext(schemaContext);

        this.shard = Preconditions.checkNotNull(shard);
        this.treeChangeListenerPublisher = Preconditions.checkNotNull(treeChangeListenerPublisher);
        this.dataChangeListenerPublisher = Preconditions.checkNotNull(dataChangeListenerPublisher);
        this.logContext = Preconditions.checkNotNull(logContext);
    }

    public ShardDataTree(final Shard shard, final SchemaContext schemaContext, final TreeType treeType) {
        this(shard, schemaContext, treeType, new DefaultShardDataTreeChangeListenerPublisher(),
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

    ShardDataTreeTransactionChain ensureTransactionChain(final LocalHistoryIdentifier localHistoryIdentifier) {
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

    // FIXME: This should be removed, it violates encapsulation
    public DataTreeCandidate commit(final DataTreeModification modification) throws DataValidationFailedException {
        modification.ready();
        dataTree.validate(modification);
        DataTreeCandidateTip candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);
        return candidate;
    }

    private void processNextTransaction() {
        while (!pendingTransactions.isEmpty()) {
            final SimpleShardDataTreeCohort cohort = pendingTransactions.peek();
            final DataTreeModification modification = cohort.getDataTreeModification();

            LOG.debug("Validating transaction {}", cohort.getIdentifier());
            Exception cause;
            try {
                dataTree.validate(modification);
                LOG.debug("Transaction {} validated", cohort.getIdentifier());
                cohort.successfulCanCommit();
                return;
            } catch (ConflictingModificationAppliedException e) {
                LOG.warn("Store Tx {}: Conflicting modification for path {}.", cohort.getIdentifier(), e.getPath());
                cause = new OptimisticLockFailedException("Optimistic lock failed.", e);
            } catch (DataValidationFailedException e) {
                LOG.warn("Store Tx {}: Data validation failed for path {}.", cohort.getIdentifier(), e.getPath(), e);

                // For debugging purposes, allow dumping of the modification. Coupled with the above
                // precondition log, it should allow us to understand what went on.
                LOG.debug("Store Tx {}: modifications: {} tree: {}", cohort.getIdentifier(), modification, dataTree);
                cause = new TransactionCommitFailedException("Data did not pass validation.", e);
            } catch (Exception e) {
                LOG.warn("Unexpected failure in validation phase", e);
                cause = e;
            }

            // Failure path: propagate the failure, remove the transaction from the queue and loop to the next one
            pendingTransactions.poll().failedCanCommit(cause);
        };
    }

    void startCanCommit(final SimpleShardDataTreeCohort cohort) {
        pendingTransactions.add(cohort);

        final SimpleShardDataTreeCohort current = pendingTransactions.peek();
        if (!cohort.equals(current)) {
            LOG.debug("Transaction {} scheduled for canCommit step", cohort.getIdentifier());
            return;
        }

        processNextTransaction();
    }

    private void failPrecommit(final Exception cause) {
        pendingTransactions.poll().failedPreCommit(cause);
    }

    void startPreCommit(final SimpleShardDataTreeCohort cohort) {
        final SimpleShardDataTreeCohort current = pendingTransactions.peek();
        Verify.verify(cohort.equals(current), "Attempted to pre-commit %s while %s is pending", cohort, current);

        final DataTreeCandidateTip candidate;
        try {
            candidate = dataTree.prepare(cohort.getDataTreeModification());
        } catch (Exception e) {
            failPrecommit(e);
            return;
        }

        try {
            cohort.userPreCommit(candidate);
        } catch (ExecutionException | TimeoutException e) {
            failPrecommit(e);
            return;
        }

        cohort.successfulPreCommit(candidate);
    }

    private void failCommit(final Exception cause) {
        pendingTransactions.poll().failedCommit(cause);
        // FIXME: move to next transaction?
    }

    private void finishCommit(final SimpleShardDataTreeCohort cohort) {
        final TransactionIdentifier txId = cohort.getIdentifier();
        final DataTreeCandidate candidate = cohort.getCandidate();

        try {
            try {
                dataTree.commit(candidate);
            } catch (IllegalStateException e) {
                // We may get a "store tree and candidate base differ" IllegalStateException from commit under
                // certain edge case scenarios so we'll try to re-apply the candidate from scratch as a last
                // resort. Eg, we're a follower and a tx payload is replicated but the leader goes down before
                // applying it to the state. We then become the leader and a second tx is pre-committed and
                // replicated. When consensus occurs, this will cause the first tx to be applied as a foreign
                // candidate via applyState prior to the second tx. Since the second tx has already been
                // pre-committed, when it gets here to commit it will get an IllegalStateException.

                // FIXME - this is not an ideal way to handle this scenario. This is temporary - a cleaner
                // solution will be forthcoming.

                LOG.debug("Commit failed for transaction {} - retrying as foreign candidate", txId, e);
                applyForeignCandidate(txId, candidate);
            }
        } catch (Exception e) {
            failCommit(e);
            return;
        }

        // FIXME: propagate journal index
        pendingTransactions.poll().successfulCommit(UnsignedLong.ZERO);

        LOG.trace("Transaction {} committed, proceeding to notify", txId);
        notifyListeners(candidate);
    }

    void startCommit(final SimpleShardDataTreeCohort cohort, final DataTreeCandidate candidate) {
        final SimpleShardDataTreeCohort current = pendingTransactions.peek();
        Verify.verify(cohort.equals(current), "Attempted to commit %s while %s is pending", cohort, current);

        if (shard.canSkipPayload()) {
            finishCommit(cohort);
            return;
        }

        final TransactionIdentifier txId = cohort.getIdentifier();
        final Payload payload;
        try {
            payload = CommitTransactionPayload.create(txId, candidate);
        } catch (IOException e) {
            LOG.error("Failed to encode transaction {} candidate {}", txId, candidate, e);
            pendingTransactions.poll().failedCommit(e);
            return;
        }

        // Once completed, we will continue via payloadReplicationComplete
        shard.persistPayload(txId, payload);
    }

    private void payloadReplicationComplete(final TransactionIdentifier txId, final DataTreeCandidateSupplier payload) {
        final SimpleShardDataTreeCohort current = pendingTransactions.peek();
        if (current == null) {
            LOG.warn("No outstanding transactions, ignoring consensus on transaction {}", txId);
            return;
        }

        if (!current.getIdentifier().equals(txId)) {
            LOG.warn("Head of queue is {}, ignoring consensus on transaction {}", current.getIdentifier(), txId);
            return;
        }

        finishCommit(current);
    }

    void payloadReplicationComplete(final Identifier identifier, final DataTreeCandidateSupplier payload) {
        // For now we do not care about anything else but transactions
        Verify.verify(identifier instanceof TransactionIdentifier);
        payloadReplicationComplete((TransactionIdentifier)identifier, payload);
    }

    void applyRecoveryTransaction(final ReadWriteShardDataTreeTransaction transaction) throws DataValidationFailedException {
        // FIXME: purge any outstanding transactions

        final DataTreeModification snapshot = transaction.getSnapshot();
        snapshot.ready();

        dataTree.validate(snapshot);
        dataTree.commit(dataTree.prepare(snapshot));
    }

    void processCohortRegistryCommand(final ActorRef sender, final CohortRegistryCommand message) {
        cohortRegistry.process(sender, message);
    }

    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier txId,
            final DataTreeModification modification) {
        return new SimpleShardDataTreeCohort(this, modification, txId,
            cohortRegistry.createCohort(schemaContext, txId, COMMIT_STEP_TIMEOUT));
    }

    void applyStateFromLeader(final Identifier identifier, final DataTreeCandidateSupplier payload)
            throws DataValidationFailedException, IOException {
        applyForeignCandidate(identifier, payload.getCandidate().getValue());
    }
}
