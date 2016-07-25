/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.VersionedExternalizableMessage;
import org.opendaylight.controller.cluster.datastore.utils.AbstractBatchedModificationsCursor;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;

/**
 * Coordinates commits for a shard ensuring only one concurrent 3-phase commit.
 *
 * @author Thomas Pantelis
 */
final class ShardCommitCoordinator {

    // Interface hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    public interface CohortDecorator {
        ShardDataTreeCohort decorate(Identifier transactionID, ShardDataTreeCohort actual);
    }

    private final Map<Identifier, CohortEntry> cohortCache = new HashMap<>();

    private final ShardDataTree dataTree;

    private final Queue<CohortEntry> readyEntries = new ArrayDeque<>();
    private int queueCapacity;

    private final Logger log;

    private final String name;

    private final long cacheExpiryTimeoutInMillis;

    // This is a hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    private CohortDecorator cohortDecorator;

    private ReadyTransactionReply readyTransactionReply;

    private Runnable runOnPendingTransactionsComplete;

    ShardCommitCoordinator(final ShardDataTree dataTree, final long cacheExpiryTimeoutInMillis, final int queueCapacity, final Logger log,
            final String name) {

        this.queueCapacity = queueCapacity;
        this.log = log;
        this.name = name;
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.cacheExpiryTimeoutInMillis = cacheExpiryTimeoutInMillis;
    }

    int getCohortCacheSize() {
        return cohortCache.size();
    }

    void setQueueCapacity(final int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    private String persistenceId() {
        return dataTree.logContext();
    }

    private ReadyTransactionReply readyTransactionReply(final ActorRef cohort) {
        if (readyTransactionReply == null) {
            readyTransactionReply = new ReadyTransactionReply(Serialization.serializedActorPath(cohort));
        }

        return readyTransactionReply;
    }

    private boolean queueCohortEntry(final CohortEntry cohortEntry, final ActorRef sender, final Shard shard) {
        final int readySize = readyEntries.size();
        if (readySize < queueCapacity) {
            readyEntries.offer(cohortEntry);
            log.debug("{}: Enqueued transaction {}, queue size {}", name, cohortEntry.getTransactionID(), readySize);
            return true;
        }

        // Slow path
        log.error("{}: Maximum commit queue capacity {} reached, cannot enqueue transaction {}", name, queueCapacity,
            cohortEntry.getTransactionID());

        cohortCache.remove(cohortEntry.getTransactionID());
        sender.tell(new Failure(new RuntimeException(String.format(
            "%s: Could not enqueue transaction %s - the maximum commit queue capacity %s has been reached.", name,
            cohortEntry.getTransactionID(), queueCapacity))), shard.self());
        return false;
    }

    /**
     * This method is called to ready a transaction that was prepared by ShardTransaction actor. It caches
     * the prepared cohort entry for the given transactions ID in preparation for the subsequent 3-phase commit.
     *
     * @param ready the ForwardedReadyTransaction message to process
     * @param sender the sender of the message
     * @param shard the transaction's shard actor
     * @param schema
     */
    void handleForwardedReadyTransaction(final ForwardedReadyTransaction ready, final ActorRef sender,
            final Shard shard) {
        log.debug("{}: Readying transaction {}, client version {}", name,
                ready.getTransactionID(), ready.getTxnClientVersion());

        final ShardDataTreeCohort cohort = ready.getTransaction().ready();
        final CohortEntry cohortEntry = CohortEntry.createReady(cohort, ready.getTxnClientVersion());
        cohortCache.put(cohortEntry.getTransactionID(), cohortEntry);

        if (!queueCohortEntry(cohortEntry, sender, shard)) {
            return;
        }

        if (ready.isDoImmediateCommit()) {
            cohortEntry.setDoImmediateCommit(true);
            cohortEntry.setReplySender(sender);
            cohortEntry.setShard(shard);
            handleCanCommit(cohortEntry);
        } else {
            // The caller does not want immediate commit - the 3-phase commit will be coordinated by the
            // front-end so send back a ReadyTransactionReply with our actor path.
            sender.tell(readyTransactionReply(shard.self()), shard.self());
        }
    }

    /**
     * This method handles a BatchedModifications message for a transaction being prepared directly on the
     * Shard actor instead of via a ShardTransaction actor. If there's no currently cached
     * DOMStoreWriteTransaction, one is created. The batched modifications are applied to the write Tx. If
     * the BatchedModifications is ready to commit then a DOMStoreThreePhaseCommitCohort is created.
     *
     * @param batched the BatchedModifications message to process
     * @param sender the sender of the message
     */
    void handleBatchedModifications(final BatchedModifications batched, final ActorRef sender, final Shard shard) {
        CohortEntry cohortEntry = cohortCache.get(batched.getTransactionID());
        if (cohortEntry == null) {
            cohortEntry = CohortEntry.createOpen(dataTree.newReadWriteTransaction(batched.getTransactionID()),
                batched.getVersion());
            cohortCache.put(cohortEntry.getTransactionID(), cohortEntry);
        }

        if (log.isDebugEnabled()) {
            log.debug("{}: Applying {} batched modifications for Tx {}", name,
                    batched.getModifications().size(), batched.getTransactionID());
        }

        cohortEntry.applyModifications(batched.getModifications());

        if (batched.isReady()) {
            if (cohortEntry.getLastBatchedModificationsException() != null) {
                cohortCache.remove(cohortEntry.getTransactionID());
                throw cohortEntry.getLastBatchedModificationsException();
            }

            if (cohortEntry.getTotalBatchedModificationsReceived() != batched.getTotalMessagesSent()) {
                cohortCache.remove(cohortEntry.getTransactionID());
                throw new IllegalStateException(String.format(
                        "The total number of batched messages received %d does not match the number sent %d",
                        cohortEntry.getTotalBatchedModificationsReceived(), batched.getTotalMessagesSent()));
            }

            if (!queueCohortEntry(cohortEntry, sender, shard)) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("{}: Readying Tx {}, client version {}", name,
                        batched.getTransactionID(), batched.getVersion());
            }

            cohortEntry.ready(cohortDecorator, batched.isDoCommitOnReady());

            if (batched.isDoCommitOnReady()) {
                cohortEntry.setReplySender(sender);
                cohortEntry.setShard(shard);
                handleCanCommit(cohortEntry);
            } else {
                sender.tell(readyTransactionReply(shard.self()), shard.self());
            }
        } else {
            sender.tell(new BatchedModificationsReply(batched.getModifications().size()), shard.self());
        }
    }

    /**
     * This method handles {@link ReadyLocalTransaction} message. All transaction modifications have
     * been prepared beforehand by the sender and we just need to drive them through into the
     * dataTree.
     *
     * @param message the ReadyLocalTransaction message to process
     * @param sender the sender of the message
     * @param shard the transaction's shard actor
     */
    void handleReadyLocalTransaction(final ReadyLocalTransaction message, final ActorRef sender, final Shard shard) {
        final ShardDataTreeCohort cohort = dataTree.createReadyCohort(message.getTransactionID(),
            message.getModification());
        final CohortEntry cohortEntry = CohortEntry.createReady(cohort, DataStoreVersions.CURRENT_VERSION);
        cohortCache.put(cohortEntry.getTransactionID(), cohortEntry);
        cohortEntry.setDoImmediateCommit(message.isDoCommitOnReady());

        if (!queueCohortEntry(cohortEntry, sender, shard)) {
            return;
        }

        log.debug("{}: Applying local modifications for Tx {}", name, message.getTransactionID());

        if (message.isDoCommitOnReady()) {
            cohortEntry.setReplySender(sender);
            cohortEntry.setShard(shard);
            handleCanCommit(cohortEntry);
        } else {
            sender.tell(readyTransactionReply(shard.self()), shard.self());
        }
    }

    Collection<BatchedModifications> createForwardedBatchedModifications(final BatchedModifications from,
            final int maxModificationsPerBatch) {
        CohortEntry cohortEntry = getAndRemoveCohortEntry(from.getTransactionID());
        if (cohortEntry == null || cohortEntry.getTransaction() == null) {
            return Collections.singletonList(from);
        }

        cohortEntry.applyModifications(from.getModifications());

        final LinkedList<BatchedModifications> newModifications = new LinkedList<>();
        cohortEntry.getTransaction().getSnapshot().applyToCursor(new AbstractBatchedModificationsCursor() {
            @Override
            protected BatchedModifications getModifications() {
                if (newModifications.isEmpty() ||
                        newModifications.getLast().getModifications().size() >= maxModificationsPerBatch) {
                    newModifications.add(new BatchedModifications(from.getTransactionID(), from.getVersion()));
                }

                return newModifications.getLast();
            }
        });

        BatchedModifications last = newModifications.getLast();
        last.setDoCommitOnReady(from.isDoCommitOnReady());
        last.setReady(from.isReady());
        last.setTotalMessagesSent(newModifications.size());
        return newModifications;
    }

    private void handleCanCommit(final CohortEntry cohortEntry) {
        cohortEntry.canCommit(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                log.debug("{}: canCommit for {}: success", name, cohortEntry.getTransactionID());

                if (cohortEntry.isDoImmediateCommit()) {
                    doCommit(cohortEntry);
                } else {
                    cohortEntry.getReplySender().tell(
                        CanCommitTransactionReply.yes(cohortEntry.getClientVersion()).toSerializable(),
                        cohortEntry.getShard().self());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                log.debug("{}: An exception occurred during canCommit", name, t);
                currentTransactionComplete(cohortEntry.getTransactionID(), true);
                cohortEntry.getReplySender().tell(new Failure(t), cohortEntry.getShard().self());
            }
        });
    }

    /**
     * This method handles the canCommit phase for a transaction.
     *
     * @param transactionID the ID of the transaction to canCommit
     * @param sender the actor to which to send the response
     * @param shard the transaction's shard actor
     */
    void handleCanCommit(final Identifier transactionID, final ActorRef sender, final Shard shard) {
        // Lookup the cohort entry that was cached previously (or should have been) by
        // transactionReady (via the ForwardedReadyTransaction message).
        final CohortEntry cohortEntry = cohortCache.get(transactionID);
        if (cohortEntry == null) {
            // Either canCommit was invoked before ready(shouldn't happen)  or a long time passed
            // between canCommit and ready and the entry was expired from the cache.
            IllegalStateException ex = new IllegalStateException(
                    String.format("%s: No cohort entry found for transaction %s", name, transactionID));
            log.error(ex.getMessage());
            sender.tell(new Failure(ex), shard.self());
            return;
        }

        cohortEntry.setReplySender(sender);
        cohortEntry.setShard(shard);

        handleCanCommit(cohortEntry);
    }

    private void doCommit(final CohortEntry cohortEntry) {
        log.debug("{}: Committing transaction {}", name, cohortEntry.getTransactionID());

        // We perform the preCommit phase here atomically with the commit phase. This is an
        // optimization to eliminate the overhead of an extra preCommit message. We lose front-end
        // coordination of preCommit across shards in case of failure but preCommit should not
        // normally fail since we ensure only one concurrent 3-phase commit.
        cohortEntry.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate candidate) {
                finishCommit(cohortEntry.getReplySender(), cohortEntry);
                cohortEntry.updateLastAccessTime();
            }

            @Override
            public void onFailure(final Throwable t) {
                log.error("{} An exception occurred while preCommitting transaction {}", name,
                    cohortEntry.getTransactionID(), t);
                cohortEntry.getShard().getShardMBean().incrementFailedTransactionsCount();
                cohortEntry.getReplySender().tell(new Failure(t), cohortEntry.getShard().self());
                currentTransactionComplete(cohortEntry.getTransactionID(), true);
            }
        });
    }

    private void finishCommit(@Nonnull final ActorRef sender, @Nonnull final CohortEntry cohortEntry) {
        log.debug("{}: Finishing commit for transaction {}", persistenceId(), cohortEntry.getTransactionID());

        cohortEntry.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                final TransactionIdentifier txId = cohortEntry.getTransactionID();
                log.debug("{}: Transaction {} committed as {}, sending response to {}", persistenceId(), txId, result,
                    sender);

                sender.tell(CommitTransactionReply.instance(cohortEntry.getClientVersion()).toSerializable(),
                    cohortEntry.self());
                currentTransactionComplete(txId, true);
            }

            @Override
            public void onFailure(final Throwable t) {
                final TransactionIdentifier txId = cohortEntry.getTransactionID();
                log.error("{}, An exception occurred while committing transaction {}", persistenceId(), txId, t);
                sender.tell(new Failure(t), cohortEntry.self());
                currentTransactionComplete(txId, true);
            }
        });
    }

    /**
     * This method handles the preCommit and commit phases for a transaction.
     *
     * @param transactionID the ID of the transaction to commit
     * @param sender the actor to which to send the response
     * @param shard the transaction's shard actor
     */
    void handleCommit(final Identifier transactionID, final ActorRef sender, final Shard shard) {
        // Get the current in-progress cohort entry in the commitCoordinator if it corresponds to
        // this transaction.
        final CohortEntry cohortEntry = getCohortEntryIfCurrent(transactionID);
        if (cohortEntry == null) {
            // We're not the current Tx - the Tx was likely expired b/c it took too long in
            // between the canCommit and commit messages.
            IllegalStateException ex = new IllegalStateException(
                    String.format("%s: Cannot commit transaction %s - it is not the current transaction",
                            name, transactionID));
            log.error(ex.getMessage());
            sender.tell(new Failure(ex), shard.self());
            shard.getShardMBean().incrementFailedTransactionsCount();
        } else {
            cohortEntry.setReplySender(sender);
            doCommit(cohortEntry);
        }
    }

    void handleAbort(final Identifier transactionID, final ActorRef sender, final Shard shard) {
        CohortEntry cohortEntry = getCohortEntryIfCurrent(transactionID);
        if (cohortEntry != null) {
            // We don't remove the cached cohort entry here (ie pass false) in case the Tx was
            // aborted during replication in which case we may still commit locally if replication
            // succeeds.
            currentTransactionComplete(transactionID, false);
        } else {
            cohortEntry = getAndRemoveCohortEntry(transactionID);
        }

        if (cohortEntry == null) {
            return;
        }

        log.debug("{}: Aborting transaction {}", name, transactionID);

        final ActorRef self = shard.getSelf();
        try {
            cohortEntry.abort();

            shard.getShardMBean().incrementAbortTransactionsCount();

            if (sender != null) {
                sender.tell(AbortTransactionReply.instance(cohortEntry.getClientVersion()).toSerializable(), self);
            }
        } catch (Exception e) {
            log.error("{}: An exception happened during abort", name, e);

            if (sender != null) {
                sender.tell(new Failure(e), self);
            }
        }
    }

    void checkForExpiredTransactions(final long timeout, final Shard shard) {
        CohortEntry cohortEntry = getCurrentCohortEntry();
        if (cohortEntry != null) {
            if (cohortEntry.isExpired(timeout)) {
                log.warn("{}: Current transaction {} has timed out after {} ms - aborting",
                        name, cohortEntry.getTransactionID(), timeout);

                handleAbort(cohortEntry.getTransactionID(), null, shard);
            }
        }

        cleanupExpiredCohortEntries();
    }

    void abortPendingTransactions(final String reason, final Shard shard) {
        final Collection<CohortEntry> cohortEntries = getAndClearPendingCohortEntries();

        log.debug("{}: Aborting {} pending queued transactions", name, cohortEntries.size());

        final Failure failure = new Failure(new RuntimeException(reason));
        for (CohortEntry cohortEntry: cohortEntries) {
            if (cohortEntry.getReplySender() != null) {
                cohortEntry.getReplySender().tell(failure, shard.self());
            }
        }
    }

    private Collection<CohortEntry> getAndClearPendingCohortEntries() {
        for (CohortEntry cohortEntry : readyEntries) {
            cohortCache.remove(cohortEntry.getTransactionID());
        }

        final Collection<CohortEntry> ret = ImmutableList.copyOf(readyEntries);
        readyEntries.clear();
        return ret;
    }

    Collection<?> convertPendingTransactionsToMessages(final int maxModificationsPerBatch) {
        final Collection<VersionedExternalizableMessage> messages = new ArrayList<>();
        for (CohortEntry cohortEntry : getAndClearPendingCohortEntries()) {
            if (cohortEntry.isExpired(cacheExpiryTimeoutInMillis) || cohortEntry.isAborted()) {
                continue;
            }

            final Deque<BatchedModifications> newMessages = new ArrayDeque<>();
            cohortEntry.getDataTreeModification().applyToCursor(new AbstractBatchedModificationsCursor() {
                @Override
                protected BatchedModifications getModifications() {
                    final BatchedModifications lastBatch = newMessages.peekLast();

                    if (lastBatch != null && lastBatch.getModifications().size() >= maxModificationsPerBatch) {
                        return lastBatch;
                    }

                    // Allocate a new message
                    final BatchedModifications ret = new BatchedModifications(cohortEntry.getTransactionID(),
                        cohortEntry.getClientVersion());
                    newMessages.add(ret);
                    return ret;
                }
            });

            final BatchedModifications last = newMessages.peekLast();
            if (last != null) {
                final boolean immediate = cohortEntry.isDoImmediateCommit();
                last.setDoCommitOnReady(immediate);
                last.setReady(true);
                last.setTotalMessagesSent(newMessages.size());

                messages.addAll(newMessages);

                if (!immediate) {
                    switch (cohortEntry.getState()) {
                        case CAN_COMMITTED:
                            messages.add(new CanCommitTransaction(cohortEntry.getTransactionID(),
                                cohortEntry.getClientVersion()));
                            break;
                        case PRE_COMMITTED:
                            messages.add(new CommitTransaction(cohortEntry.getTransactionID(),
                                cohortEntry.getClientVersion()));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return messages;
    }

    /**
     * Returns the cohort entry for the Tx commit currently in progress if the given transaction ID
     * matches the current entry.
     *
     * @param transactionID the ID of the transaction
     * @return the current CohortEntry or null if the given transaction ID does not match the
     *         current entry.
     */
    CohortEntry getCohortEntryIfCurrent(final Identifier transactionID) {
        final CohortEntry currentCohortEntry = readyEntries.peek();
        if (isCurrentTransaction(transactionID)) {
            return currentCohortEntry;
        }

        return null;
    }

    CohortEntry getCurrentCohortEntry() {
        return readyEntries.peek();
    }

    CohortEntry getAndRemoveCohortEntry(final Identifier transactionID) {
        return cohortCache.remove(transactionID);
    }

    boolean isCurrentTransaction(final Identifier transactionID) {
        final CohortEntry currentCohortEntry = readyEntries.peek();
        return currentCohortEntry != null && currentCohortEntry.getTransactionID().equals(transactionID);
    }

    /**
     * This method is called when a transaction is complete, successful or not. If the given
     * given transaction ID matches the current in-progress transaction, the next cohort entry,
     * if any, is dequeued and processed.
     *
     * @param transactionID the ID of the completed transaction
     * @param removeCohortEntry if true the CohortEntry for the transaction is also removed from
     *        the cache.
     */
    void currentTransactionComplete(final Identifier transactionID, final boolean removeCohortEntry) {
        if (removeCohortEntry) {
            cohortCache.remove(transactionID);
        }

        if (isCurrentTransaction(transactionID)) {
            readyEntries.poll();

            log.debug("{}: currentTransactionComplete: {}", name, transactionID);

            maybeProcessNextCohortEntry();
        }
    }

    private void maybeProcessNextCohortEntry() {
        // Check if there's a next cohort entry waiting in the queue and if it is ready to commit. Also
        // clean out expired entries.
        final Iterator<CohortEntry> iter = readyEntries.iterator();
        while (iter.hasNext()) {
            final CohortEntry next = iter.next();
            if (next.isReadyToCommit()) {
                log.debug("{}: Next entry to canCommit {}", name, next);

                next.updateLastAccessTime();
                handleCanCommit(next);
                break;
            } else if (next.isExpired(cacheExpiryTimeoutInMillis)) {
                log.warn("{}: canCommit for transaction {} was not received within {} ms - entry removed from cache",
                        name, next.getTransactionID(), cacheExpiryTimeoutInMillis);
            } else if (!next.isAborted()) {
                break;
            }

            iter.remove();
            cohortCache.remove(next.getTransactionID());
        }

        maybeRunOperationOnPendingTransactionsComplete();
    }

    void cleanupExpiredCohortEntries() {
        maybeProcessNextCohortEntry();
    }

    void setRunOnPendingTransactionsComplete(final Runnable operation) {
        runOnPendingTransactionsComplete = operation;
        maybeRunOperationOnPendingTransactionsComplete();
    }

    private void maybeRunOperationOnPendingTransactionsComplete() {
        if (runOnPendingTransactionsComplete != null && readyEntries.isEmpty()) {
            log.debug("{}: Pending transactions complete - running operation {}", name, runOnPendingTransactionsComplete);

            runOnPendingTransactionsComplete.run();
            runOnPendingTransactionsComplete = null;
        }
    }

    @VisibleForTesting
    void setCohortDecorator(final CohortDecorator cohortDecorator) {
        this.cohortDecorator = cohortDecorator;
    }
}
