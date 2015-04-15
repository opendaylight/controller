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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.compat.BackwardsCompatibleThreePhaseCommitCohort;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.slf4j.Logger;

/**
 * Coordinates commits for a shard ensuring only one concurrent 3-phase commit.
 *
 * @author Thomas Pantelis
 */
public class ShardCommitCoordinator {

    // Interface hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    public interface CohortDecorator {
        DOMStoreThreePhaseCommitCohort decorate(String transactionID, DOMStoreThreePhaseCommitCohort actual);
    }

    private final Cache<String, CohortEntry> cohortCache;

    private CohortEntry currentCohortEntry;

    private final DOMTransactionFactory transactionFactory;

    private final Queue<CohortEntry> queuedCohortEntries;

    private int queueCapacity;

    private final Logger log;

    private final String name;

    private final RemovalListener<String, CohortEntry> cacheRemovalListener =
            new RemovalListener<String, CohortEntry>() {
                @Override
                public void onRemoval(RemovalNotification<String, CohortEntry> notification) {
                    if(notification.getCause() == RemovalCause.EXPIRED) {
                        log.warn("{}: Transaction {} was timed out of the cache", name, notification.getKey());
                    }
                }
            };

    // This is a hook for unit tests to replace or decorate the DOMStoreThreePhaseCommitCohorts.
    private CohortDecorator cohortDecorator;

    private ReadyTransactionReply readyTransactionReply;

    public ShardCommitCoordinator(DOMTransactionFactory transactionFactory,
            long cacheExpiryTimeoutInSec, int queueCapacity, ActorRef shardActor, Logger log, String name) {

        this.queueCapacity = queueCapacity;
        this.log = log;
        this.name = name;
        this.transactionFactory = transactionFactory;

        cohortCache = CacheBuilder.newBuilder().expireAfterAccess(cacheExpiryTimeoutInSec, TimeUnit.SECONDS).
                removalListener(cacheRemovalListener).build();

        // We use a LinkedList here to avoid synchronization overhead with concurrent queue impls
        // since this should only be accessed on the shard's dispatcher.
        queuedCohortEntries = new LinkedList<>();
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    private ReadyTransactionReply readyTransactionReply(Shard shard) {
        if(readyTransactionReply == null) {
            readyTransactionReply = new ReadyTransactionReply(Serialization.serializedActorPath(shard.self()));
        }

        return readyTransactionReply;
    }

    /**
     * This method is called to ready a transaction that was prepared by ShardTransaction actor. It caches
     * the prepared cohort entry for the given transactions ID in preparation for the subsequent 3-phase commit.
     */
    public void handleForwardedReadyTransaction(ForwardedReadyTransaction ready, ActorRef sender, Shard shard) {
        log.debug("{}: Readying transaction {}, client version {}", name,
                ready.getTransactionID(), ready.getTxnClientVersion());

        CohortEntry cohortEntry = new CohortEntry(ready.getTransactionID(), ready.getCohort(),
                (MutableCompositeModification) ready.getModification());
        cohortCache.put(ready.getTransactionID(), cohortEntry);

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
     * @param batched the BatchedModifications
     * @param shardActor the transaction's shard actor
     *
     * @throws ExecutionException if an error occurs loading the cache
     */
    boolean handleBatchedModifications(BatchedModifications batched, ActorRef sender, Shard shard)
            throws ExecutionException {
        CohortEntry cohortEntry = cohortCache.getIfPresent(batched.getTransactionID());
        if(cohortEntry == null) {
            cohortEntry = new CohortEntry(batched.getTransactionID(),
                    transactionFactory.<DOMStoreWriteTransaction>newTransaction(
                        TransactionProxy.TransactionType.WRITE_ONLY, batched.getTransactionID(),
                        batched.getTransactionChainID()));
            cohortCache.put(batched.getTransactionID(), cohortEntry);
        }

        if(log.isDebugEnabled()) {
            log.debug("{}: Applying {} batched modifications for Tx {}", name,
                    batched.getModifications().size(), batched.getTransactionID());
        }

        cohortEntry.applyModifications(batched.getModifications());

        if(batched.isReady()) {
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

        return batched.isReady();
    }

    private void handleCanCommit(CohortEntry cohortEntry) {
        String transactionID = cohortEntry.getTransactionID();

        if(log.isDebugEnabled()) {
            log.debug("{}: Processing canCommit for transaction {} for shard {}",
                    name, transactionID, cohortEntry.getShard().self().path());
        }

        if(currentCohortEntry != null) {
            // There's already a Tx commit in progress - attempt to queue this entry to be
            // committed after the current Tx completes.
            log.debug("{}: Transaction {} is already in progress - queueing transaction {}",
                    name, currentCohortEntry.getTransactionID(), transactionID);

            if(queuedCohortEntries.size() < queueCapacity) {
                queuedCohortEntries.offer(cohortEntry);
            } else {
                removeCohortEntry(transactionID);

                RuntimeException ex = new RuntimeException(
                        String.format("%s: Could not enqueue transaction %s - the maximum commit queue"+
                                      " capacity %d has been reached.",
                                      name, transactionID, queueCapacity));
                log.error(ex.getMessage());
                cohortEntry.getReplySender().tell(new Status.Failure(ex), cohortEntry.getShard().self());
            }
        } else {
            // No Tx commit currently in progress - make this the current entry and proceed with
            // canCommit.
            cohortEntry.updateLastAccessTime();
            currentCohortEntry = cohortEntry;

            doCanCommit(cohortEntry);
        }
    }

    /**
     * This method handles the canCommit phase for a transaction.
     *
     * @param canCommit the CanCommitTransaction message
     * @param sender the actor that sent the message
     * @param shard the transaction's shard actor
     */
    public void handleCanCommit(String transactionID, final ActorRef sender, final Shard shard) {
        // Lookup the cohort entry that was cached previously (or should have been) by
        // transactionReady (via the ForwardedReadyTransaction message).
        final CohortEntry cohortEntry = cohortCache.getIfPresent(transactionID);
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
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            canCommit = cohortEntry.getCohort().canCommit().get();

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
            log.debug("{}: An exception occurred during canCommit: {}", name, e);

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
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            cohortEntry.getCohort().preCommit().get();

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

        return doCommit(cohortEntry);
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
        CohortEntry cohortEntry = cohortCache.getIfPresent(transactionID);
        cohortCache.invalidate(transactionID);
        return cohortEntry;
    }

    public void removeCohortEntry(String transactionID) {
        cohortCache.invalidate(transactionID);
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
            removeCohortEntry(transactionID);
        }

        if(isCurrentTransaction(transactionID)) {
            // Dequeue the next cohort entry waiting in the queue.
            currentCohortEntry = queuedCohortEntries.poll();
            if(currentCohortEntry != null) {
                currentCohortEntry.updateLastAccessTime();
                doCanCommit(currentCohortEntry);
            }
        }
    }

    @VisibleForTesting
    void setCohortDecorator(CohortDecorator cohortDecorator) {
        this.cohortDecorator = cohortDecorator;
    }


    static class CohortEntry {
        private final String transactionID;
        private DOMStoreThreePhaseCommitCohort cohort;
        private final MutableCompositeModification compositeModification;
        private final DOMStoreWriteTransaction transaction;
        private ActorRef replySender;
        private Shard shard;
        private long lastAccessTime;
        private boolean doImmediateCommit;

        CohortEntry(String transactionID, DOMStoreWriteTransaction transaction) {
            this.compositeModification = new MutableCompositeModification();
            this.transaction = transaction;
            this.transactionID = transactionID;
        }

        CohortEntry(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
                MutableCompositeModification compositeModification) {
            this.transactionID = transactionID;
            this.cohort = cohort;
            this.compositeModification = compositeModification;
            this.transaction = null;
        }

        void updateLastAccessTime() {
            lastAccessTime = System.currentTimeMillis();
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        String getTransactionID() {
            return transactionID;
        }

        DOMStoreThreePhaseCommitCohort getCohort() {
            return cohort;
        }

        MutableCompositeModification getModification() {
            return compositeModification;
        }

        void applyModifications(Iterable<Modification> modifications) {
            for(Modification modification: modifications) {
                compositeModification.addModification(modification);
                modification.apply(transaction);
            }
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

        boolean hasModifications(){
            return compositeModification.getModifications().size() > 0;
        }
    }
}
