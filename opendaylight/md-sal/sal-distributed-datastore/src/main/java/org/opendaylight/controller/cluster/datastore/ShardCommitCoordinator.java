/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.compat.BackwardsCompatibleThreePhaseCommitCohort;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;

/**
 * Coordinates commits for a shard ensuring only one concurrent 3-phase commit.
 *
 * @author Thomas Pantelis
 */
class ShardCommitCoordinator {

    // Interface hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    public interface CohortDecorator {
        ShardDataTreeCohort decorate(String transactionID, ShardDataTreeCohort actual);
    }

    private final Map<String, CohortEntry> cohortCache = new HashMap<>();

    private CohortEntry currentCohortEntry;

    private final ShardDataTree dataTree;

    // We use a LinkedList here to avoid synchronization overhead with concurrent queue impls
    // since this should only be accessed on the shard's dispatcher.
    private final Queue<CohortEntry> queuedCohortEntries = new LinkedList<>();

    private int queueCapacity;

    private final Logger log;

    private final String name;

    private final long cacheExpiryTimeoutInMillis;

    // This is a hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    private CohortDecorator cohortDecorator;

    private ReadyTransactionReply readyTransactionReply;

    ShardCommitCoordinator(ShardDataTree dataTree,
            long cacheExpiryTimeoutInMillis, int queueCapacity, ActorRef shardActor, Logger log, String name) {

        this.queueCapacity = queueCapacity;
        this.log = log;
        this.name = name;
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.cacheExpiryTimeoutInMillis = cacheExpiryTimeoutInMillis;
    }

    int getQueueSize() {
        return queuedCohortEntries.size();
    }

    void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    private ReadyTransactionReply readyTransactionReply(Shard shard) {
        if(readyTransactionReply == null) {
            readyTransactionReply = new ReadyTransactionReply(Serialization.serializedActorPath(shard.self()));
        }

        return readyTransactionReply;
    }

    private boolean queueCohortEntry(CohortEntry cohortEntry, ActorRef sender, Shard shard) {
        if(queuedCohortEntries.size() < queueCapacity) {
            queuedCohortEntries.offer(cohortEntry);

            log.debug("{}: Enqueued transaction {}, queue size {}", name, cohortEntry.getTransactionID(),
                    queuedCohortEntries.size());

            return true;
        } else {
            cohortCache.remove(cohortEntry.getTransactionID());

            RuntimeException ex = new RuntimeException(
                    String.format("%s: Could not enqueue transaction %s - the maximum commit queue"+
                                  " capacity %d has been reached.",
                                  name, cohortEntry.getTransactionID(), queueCapacity));
            log.error(ex.getMessage());
            sender.tell(new Status.Failure(ex), shard.self());
            return false;
        }
    }

    /**
     * This method is called to ready a transaction that was prepared by ShardTransaction actor. It caches
     * the prepared cohort entry for the given transactions ID in preparation for the subsequent 3-phase commit.
     *
     * @param ready the ForwardedReadyTransaction message to process
     * @param sender the sender of the message
     * @param shard the transaction's shard actor
     */
    void handleForwardedReadyTransaction(ForwardedReadyTransaction ready, ActorRef sender, Shard shard) {
        log.debug("{}: Readying transaction {}, client version {}", name,
                ready.getTransactionID(), ready.getTxnClientVersion());

        CohortEntry cohortEntry = new CohortEntry(ready.getTransactionID(), ready.getCohort(),
                (MutableCompositeModification) ready.getModification());
        cohortCache.put(ready.getTransactionID(), cohortEntry);

        if(!queueCohortEntry(cohortEntry, sender, shard)) {
            return;
        }

        if(ready.getTxnClientVersion() < DataStoreVersions.LITHIUM_VERSION) {
            // Return our actor path as we'll handle the three phase commit except if the Tx client
            // version < Helium-1 version which means the Tx was initiated by a base Helium version node.
            // In that case, the subsequent 3-phase commit messages won't contain the transactionId so to
            // maintain backwards compatibility, we create a separate cohort actor to provide the compatible behavior.
            ActorRef replyActorPath = shard.self();
            if(ready.getTxnClientVersion() < DataStoreVersions.HELIUM_1_VERSION) {
                log.debug("{}: Creating BackwardsCompatibleThreePhaseCommitCohort", name);
                replyActorPath = shard.getContext().actorOf(BackwardsCompatibleThreePhaseCommitCohort.props(
                        ready.getTransactionID()));
            }

            ReadyTransactionReply readyTransactionReply =
                    new ReadyTransactionReply(Serialization.serializedActorPath(replyActorPath),
                            ready.getTxnClientVersion());
            sender.tell(ready.isReturnSerialized() ? readyTransactionReply.toSerializable() :
                readyTransactionReply, shard.self());
        } else {
            if(ready.isDoImmediateCommit()) {
                cohortEntry.setDoImmediateCommit(true);
                cohortEntry.setReplySender(sender);
                cohortEntry.setShard(shard);
                handleCanCommit(cohortEntry);
            } else {
                // The caller does not want immediate commit - the 3-phase commit will be coordinated by the
                // front-end so send back a ReadyTransactionReply with our actor path.
                sender.tell(readyTransactionReply(shard), shard.self());
            }
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
     * @param shard the transaction's shard actor
     */
    void handleBatchedModifications(BatchedModifications batched, ActorRef sender, Shard shard) {
        CohortEntry cohortEntry = cohortCache.get(batched.getTransactionID());
        if(cohortEntry == null) {
            cohortEntry = new CohortEntry(batched.getTransactionID(),
                    dataTree.newReadWriteTransaction(batched.getTransactionID(),
                        batched.getTransactionChainID()));
            cohortCache.put(batched.getTransactionID(), cohortEntry);
        }

        if(log.isDebugEnabled()) {
            log.debug("{}: Applying {} batched modifications for Tx {}", name,
                    batched.getModifications().size(), batched.getTransactionID());
        }

        cohortEntry.applyModifications(batched.getModifications());

        if(batched.isReady()) {
            if(cohortEntry.getLastBatchedModificationsException() != null) {
                cohortCache.remove(cohortEntry.getTransactionID());
                throw cohortEntry.getLastBatchedModificationsException();
            }

            if(cohortEntry.getTotalBatchedModificationsReceived() != batched.getTotalMessagesSent()) {
                cohortCache.remove(cohortEntry.getTransactionID());
                throw new IllegalStateException(String.format(
                        "The total number of batched messages received %d does not match the number sent %d",
                        cohortEntry.getTotalBatchedModificationsReceived(), batched.getTotalMessagesSent()));
            }

            if(!queueCohortEntry(cohortEntry, sender, shard)) {
                return;
            }

            if(log.isDebugEnabled()) {
                log.debug("{}: Readying Tx {}, client version {}", name,
                        batched.getTransactionID(), batched.getVersion());
            }

            cohortEntry.ready(cohortDecorator, batched.isDoCommitOnReady());

            if(batched.isDoCommitOnReady()) {
                cohortEntry.setReplySender(sender);
                cohortEntry.setShard(shard);
                handleCanCommit(cohortEntry);
            } else {
                sender.tell(readyTransactionReply(shard), shard.self());
            }
        } else {
            sender.tell(new BatchedModificationsReply(batched.getModifications().size()), shard.self());
        }
    }

    /**
     * This method handles {@link ReadyLocalTransaction} message. All transaction modifications have
     * been prepared beforehand by the sender and we just need to drive them through into the dataTree.
     *
     * @param message the ReadyLocalTransaction message to process
     * @param sender the sender of the message
     * @param shard the transaction's shard actor
     */
    void handleReadyLocalTransaction(ReadyLocalTransaction message, ActorRef sender, Shard shard) {
        final ShardDataTreeCohort cohort = new SimpleShardDataTreeCohort(dataTree, message.getModification(),
                message.getTransactionID());
        final CohortEntry cohortEntry = new CohortEntry(message.getTransactionID(), cohort);
        cohortCache.put(message.getTransactionID(), cohortEntry);
        cohortEntry.setDoImmediateCommit(message.isDoCommitOnReady());

        if(!queueCohortEntry(cohortEntry, sender, shard)) {
            return;
        }

        log.debug("{}: Applying local modifications for Tx {}", name, message.getTransactionID());

        if (message.isDoCommitOnReady()) {
            cohortEntry.setReplySender(sender);
            cohortEntry.setShard(shard);
            handleCanCommit(cohortEntry);
        } else {
            sender.tell(readyTransactionReply(shard), shard.self());
        }
    }

    private void handleCanCommit(CohortEntry cohortEntry) {
        String transactionID = cohortEntry.getTransactionID();

        cohortEntry.updateLastAccessTime();

        if(currentCohortEntry != null) {
            // There's already a Tx commit in progress so we can't process this entry yet - but it's in the
            // queue and will get processed after all prior entries complete.

            if(log.isDebugEnabled()) {
                log.debug("{}: Commit for Tx {} already in progress - skipping canCommit for {} for now",
                        name, currentCohortEntry.getTransactionID(), transactionID);
            }

            return;
        }

        // No Tx commit currently in progress - check if this entry is the next one in the queue, If so make
        // it the current entry and proceed with canCommit.
        // Purposely checking reference equality here.
        if(queuedCohortEntries.peek() == cohortEntry) {
            currentCohortEntry = queuedCohortEntries.poll();
            doCanCommit(currentCohortEntry);
        } else {
            if(log.isDebugEnabled()) {
                log.debug("{}: Tx {} is the next pending canCommit - skipping {} for now",
                        name, queuedCohortEntries.peek().getTransactionID(), transactionID);
            }
        }
    }

    /**
     * This method handles the canCommit phase for a transaction.
     *
     * @param transactionID the ID of the transaction to canCommit
     * @param sender the actor to which to send the response
     * @param shard the transaction's shard actor
     */
    void handleCanCommit(String transactionID, final ActorRef sender, final Shard shard) {
        // Lookup the cohort entry that was cached previously (or should have been) by
        // transactionReady (via the ForwardedReadyTransaction message).
        final CohortEntry cohortEntry = cohortCache.get(transactionID);
        if(cohortEntry == null) {
            // Either canCommit was invoked before ready(shouldn't happen)  or a long time passed
            // between canCommit and ready and the entry was expired from the cache.
            IllegalStateException ex = new IllegalStateException(
                    String.format("%s: No cohort entry found for transaction %s", name, transactionID));
            log.error(ex.getMessage());
            sender.tell(new Status.Failure(ex), shard.self());
            return;
        }

        cohortEntry.setReplySender(sender);
        cohortEntry.setShard(shard);

        handleCanCommit(cohortEntry);
    }

    private void doCanCommit(final CohortEntry cohortEntry) {
        boolean canCommit = false;
        try {
            canCommit = cohortEntry.canCommit();

            log.debug("{}: canCommit for {}: {}", name, cohortEntry.getTransactionID(), canCommit);

            if(cohortEntry.isDoImmediateCommit()) {
                if(canCommit) {
                    doCommit(cohortEntry);
                } else {
                    cohortEntry.getReplySender().tell(new Status.Failure(new TransactionCommitFailedException(
                                "Can Commit failed, no detailed cause available.")), cohortEntry.getShard().self());
                }
            } else {
                cohortEntry.getReplySender().tell(
                        canCommit ? CanCommitTransactionReply.YES.toSerializable() :
                            CanCommitTransactionReply.NO.toSerializable(), cohortEntry.getShard().self());
            }
        } catch (Exception e) {
            log.debug("{}: An exception occurred during canCommit", name, e);

            Throwable failure = e;
            if(e instanceof ExecutionException) {
                failure = e.getCause();
            }

            cohortEntry.getReplySender().tell(new Status.Failure(failure), cohortEntry.getShard().self());
        } finally {
            if(!canCommit) {
                // Remove the entry from the cache now.
                currentTransactionComplete(cohortEntry.getTransactionID(), true);
            }
        }
    }

    private boolean doCommit(CohortEntry cohortEntry) {
        log.debug("{}: Committing transaction {}", name, cohortEntry.getTransactionID());

        boolean success = false;

        // We perform the preCommit phase here atomically with the commit phase. This is an
        // optimization to eliminate the overhead of an extra preCommit message. We lose front-end
        // coordination of preCommit across shards in case of failure but preCommit should not
        // normally fail since we ensure only one concurrent 3-phase commit.

        try {
            cohortEntry.preCommit();

            cohortEntry.getShard().continueCommit(cohortEntry);

            cohortEntry.updateLastAccessTime();

            success = true;
        } catch (Exception e) {
            log.error("{} An exception occurred while preCommitting transaction {}",
                    name, cohortEntry.getTransactionID(), e);
            cohortEntry.getReplySender().tell(new akka.actor.Status.Failure(e), cohortEntry.getShard().self());

            currentTransactionComplete(cohortEntry.getTransactionID(), true);
        }

        return success;
    }

    /**
     * This method handles the preCommit and commit phases for a transaction.
     *
     * @param transactionID the ID of the transaction to commit
     * @param sender the actor to which to send the response
     * @param shard the transaction's shard actor
     * @return true if the transaction was successfully prepared, false otherwise.
     */
    boolean handleCommit(final String transactionID, final ActorRef sender, final Shard shard) {
        // Get the current in-progress cohort entry in the commitCoordinator if it corresponds to
        // this transaction.
        final CohortEntry cohortEntry = getCohortEntryIfCurrent(transactionID);
        if(cohortEntry == null) {
            // We're not the current Tx - the Tx was likely expired b/c it took too long in
            // between the canCommit and commit messages.
            IllegalStateException ex = new IllegalStateException(
                    String.format("%s: Cannot commit transaction %s - it is not the current transaction",
                            name, transactionID));
            log.error(ex.getMessage());
            sender.tell(new akka.actor.Status.Failure(ex), shard.self());
            return false;
        }

        cohortEntry.setReplySender(sender);
        return doCommit(cohortEntry);
    }

    void handleAbort(final String transactionID, final ActorRef sender, final Shard shard) {
        CohortEntry cohortEntry = getCohortEntryIfCurrent(transactionID);
        if(cohortEntry != null) {
            // We don't remove the cached cohort entry here (ie pass false) in case the Tx was
            // aborted during replication in which case we may still commit locally if replication
            // succeeds.
            currentTransactionComplete(transactionID, false);
        } else {
            cohortEntry = getAndRemoveCohortEntry(transactionID);
        }

        if(cohortEntry == null) {
            return;
        }

        log.debug("{}: Aborting transaction {}", name, transactionID);

        final ActorRef self = shard.getSelf();
        try {
            cohortEntry.abort();

            shard.getShardMBean().incrementAbortTransactionsCount();

            if(sender != null) {
                sender.tell(new AbortTransactionReply().toSerializable(), self);
            }
        } catch (Exception e) {
            log.error("{}: An exception happened during abort", name, e);

            if(sender != null) {
                sender.tell(new akka.actor.Status.Failure(e), self);
            }
        }
    }

    /**
     * Returns the cohort entry for the Tx commit currently in progress if the given transaction ID
     * matches the current entry.
     *
     * @param transactionID the ID of the transaction
     * @return the current CohortEntry or null if the given transaction ID does not match the
     *         current entry.
     */
    public CohortEntry getCohortEntryIfCurrent(String transactionID) {
        if(isCurrentTransaction(transactionID)) {
            return currentCohortEntry;
        }

        return null;
    }

    public CohortEntry getCurrentCohortEntry() {
        return currentCohortEntry;
    }

    public CohortEntry getAndRemoveCohortEntry(String transactionID) {
        return cohortCache.remove(transactionID);
    }

    public boolean isCurrentTransaction(String transactionID) {
        return currentCohortEntry != null &&
                currentCohortEntry.getTransactionID().equals(transactionID);
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
    public void currentTransactionComplete(String transactionID, boolean removeCohortEntry) {
        if(removeCohortEntry) {
            cohortCache.remove(transactionID);
        }

        if(isCurrentTransaction(transactionID)) {
            currentCohortEntry = null;

            log.debug("{}: currentTransactionComplete: {}", name, transactionID);

            maybeProcessNextCohortEntry();
        }
    }

    private void maybeProcessNextCohortEntry() {
        // Check if there's a next cohort entry waiting in the queue and if it is ready to commit. Also
        // clean out expired entries.
        Iterator<CohortEntry> iter = queuedCohortEntries.iterator();
        while(iter.hasNext()) {
            CohortEntry next = iter.next();
            if(next.isReadyToCommit()) {
                if(currentCohortEntry == null) {
                    if(log.isDebugEnabled()) {
                        log.debug("{}: Next entry to canCommit {}", name, next);
                    }

                    iter.remove();
                    currentCohortEntry = next;
                    currentCohortEntry.updateLastAccessTime();
                    doCanCommit(currentCohortEntry);
                }

                break;
            } else if(next.isExpired(cacheExpiryTimeoutInMillis)) {
                log.warn("{}: canCommit for transaction {} was not received within {} ms - entry removed from cache",
                        name, next.getTransactionID(), cacheExpiryTimeoutInMillis);
            } else if(!next.isAborted()) {
                break;
            }

            iter.remove();
            cohortCache.remove(next.getTransactionID());
        }
    }

    void cleanupExpiredCohortEntries() {
        maybeProcessNextCohortEntry();
    }

    @VisibleForTesting
    void setCohortDecorator(CohortDecorator cohortDecorator) {
        this.cohortDecorator = cohortDecorator;
    }

    static class CohortEntry {
        private final String transactionID;
        private ShardDataTreeCohort cohort;
        private final ReadWriteShardDataTreeTransaction transaction;
        private RuntimeException lastBatchedModificationsException;
        private ActorRef replySender;
        private Shard shard;
        private boolean doImmediateCommit;
        private final Stopwatch lastAccessTimer = Stopwatch.createStarted();
        private int totalBatchedModificationsReceived;
        private boolean aborted;

        CohortEntry(String transactionID, ReadWriteShardDataTreeTransaction transaction) {
            this.transaction = Preconditions.checkNotNull(transaction);
            this.transactionID = transactionID;
        }

        CohortEntry(String transactionID, ShardDataTreeCohort cohort,
                MutableCompositeModification compositeModification) {
            this.transactionID = transactionID;
            this.cohort = cohort;
            this.transaction = null;
        }

        CohortEntry(String transactionID, ShardDataTreeCohort cohort) {
            this.transactionID = transactionID;
            this.cohort = cohort;
            this.transaction = null;
        }

        void updateLastAccessTime() {
            lastAccessTimer.reset();
            lastAccessTimer.start();
        }

        String getTransactionID() {
            return transactionID;
        }

        DataTreeCandidate getCandidate() {
            return cohort.getCandidate();
        }

        int getTotalBatchedModificationsReceived() {
            return totalBatchedModificationsReceived;
        }

        RuntimeException getLastBatchedModificationsException() {
            return lastBatchedModificationsException;
        }

        void applyModifications(Iterable<Modification> modifications) {
            totalBatchedModificationsReceived++;
            if(lastBatchedModificationsException == null) {
                for (Modification modification : modifications) {
                        try {
                            modification.apply(transaction.getSnapshot());
                        } catch (RuntimeException e) {
                            lastBatchedModificationsException = e;
                            throw e;
                        }
                }
            }
        }

        boolean canCommit() throws InterruptedException, ExecutionException {
            // We block on the future here (and also preCommit(), commit(), abort()) so we don't have to worry
            // about possibly accessing our state on a different thread outside of our dispatcher.
            // TODO: the ShardDataTreeCohort returns immediate Futures anyway which begs the question - why
            // bother even returning Futures from ShardDataTreeCohort if we have to treat them synchronously
            // anyway?. The Futures are really a remnant from when we were using the InMemoryDataBroker.
            return cohort.canCommit().get();
        }

        void preCommit() throws InterruptedException, ExecutionException {
            cohort.preCommit().get();
        }

        void commit() throws InterruptedException, ExecutionException {
            cohort.commit().get();
        }

        void abort() throws InterruptedException, ExecutionException {
            aborted = true;
            cohort.abort().get();
        }

        void ready(CohortDecorator cohortDecorator, boolean doImmediateCommit) {
            Preconditions.checkState(cohort == null, "cohort was already set");

            setDoImmediateCommit(doImmediateCommit);

            cohort = transaction.ready();

            if(cohortDecorator != null) {
                // Call the hook for unit tests.
                cohort = cohortDecorator.decorate(transactionID, cohort);
            }
        }

        boolean isReadyToCommit() {
            return replySender != null;
        }

        boolean isExpired(long expireTimeInMillis) {
            return lastAccessTimer.elapsed(TimeUnit.MILLISECONDS) >= expireTimeInMillis;
        }

        boolean isDoImmediateCommit() {
            return doImmediateCommit;
        }

        void setDoImmediateCommit(boolean doImmediateCommit) {
            this.doImmediateCommit = doImmediateCommit;
        }

        ActorRef getReplySender() {
            return replySender;
        }

        void setReplySender(ActorRef replySender) {
            this.replySender = replySender;
        }

        Shard getShard() {
            return shard;
        }

        void setShard(Shard shard) {
            this.shard = shard;
        }


        boolean isAborted() {
            return aborted;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CohortEntry [transactionID=").append(transactionID).append(", doImmediateCommit=")
                    .append(doImmediateCommit).append("]");
            return builder.toString();
        }
    }
}
