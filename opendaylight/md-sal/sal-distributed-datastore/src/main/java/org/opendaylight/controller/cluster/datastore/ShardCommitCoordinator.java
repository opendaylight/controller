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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
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

    public ShardCommitCoordinator(DOMTransactionFactory transactionFactory,
            long cacheExpiryTimeoutInSec, int queueCapacity, Logger log, String name) {

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

    /**
     * This method is called to ready a transaction that was prepared by ShardTransaction actor. It caches
     * the prepared cohort entry for the given transactions ID in preparation for the subsequent 3-phase commit.
     *
     * @param transactionID the ID of the transaction
     * @param cohort the cohort to participate in the transaction commit
     * @param modification the modifications made by the transaction
     */
    public void transactionReady(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
            MutableCompositeModification modification) {

        cohortCache.put(transactionID, new CohortEntry(transactionID, cohort, modification));
    }

    /**
     * This method handles a BatchedModifications message for a transaction being prepared directly on the
     * Shard actor instead if via a ShardTransaction actor. If there's no currently cached
     * DOMStoreWriteTransaction, one is created. The batched modifications are applied to the write Tx. If
     * the BatchedModifications is ready to commit then a DOMStoreThreePhaseCommitCohort is created.
     *
     * @param batched the BatchedModifications
     * @param shardActor the transaction's shard actor
     *
     * @throws ExecutionException if an error occurs loading the cache
     */
    public BatchedModificationsReply handleTransactionModifications(BatchedModifications batched, ActorRef shardActor)
            throws ExecutionException {
        CohortEntry cohortEntry = cohortCache.getIfPresent(batched.getTransactionID());
        if(cohortEntry == null) {
            cohortEntry = new CohortEntry(transactionFactory.<DOMStoreWriteTransaction>newTransaction(
                    TransactionProxy.TransactionType.WRITE_ONLY, batched.getTransactionID(),
                    batched.getTransactionChainID()));
            cohortCache.put(batched.getTransactionID(), cohortEntry);
        }

        cohortEntry.setTransactionID(batched.getTransactionID());

        if(log.isDebugEnabled()) {
            log.debug("{}: Applying {} batched modifications for Tx {}", name,
                    batched.getModifications().size(), batched.getTransactionID());
        }

        cohortEntry.applyModifications(batched.getModifications());

        String cohortPath = null;
        if(batched.isReady()) {
            if(log.isDebugEnabled()) {
                log.debug("{}: Readying Tx {}, client version {}", name,
                        batched.getTransactionID(), batched.getVersion());
            }

            cohortEntry.ready(cohortDecorator);
            cohortPath = Serialization.serializedActorPath(shardActor);
        }

        return new BatchedModificationsReply(batched.getModifications().size(), cohortPath);
    }

    /**
     * This method handles the canCommit phase for a transaction.
     *
     * @param canCommit the CanCommitTransaction message
     * @param sender the actor that sent the message
     * @param shard the transaction's shard actor
     */
    public void handleCanCommit(CanCommitTransaction canCommit, final ActorRef sender,
            final ActorRef shard) {
        String transactionID = canCommit.getTransactionID();
        if(log.isDebugEnabled()) {
            log.debug("{}: Processing canCommit for transaction {} for shard {}",
                    name, transactionID, shard.path());
        }

        // Lookup the cohort entry that was cached previously (or should have been) by
        // transactionReady (via the ForwardedReadyTransaction message).
        final CohortEntry cohortEntry = cohortCache.getIfPresent(transactionID);
        if(cohortEntry == null) {
            // Either canCommit was invoked before ready(shouldn't happen)  or a long time passed
            // between canCommit and ready and the entry was expired from the cache.
            IllegalStateException ex = new IllegalStateException(
                    String.format("%s: No cohort entry found for transaction %s", name, transactionID));
            log.error(ex.getMessage());
            sender.tell(new Status.Failure(ex), shard);
            return;
        }

        cohortEntry.setCanCommitSender(sender);
        cohortEntry.setShard(shard);

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
                sender.tell(new Status.Failure(ex), shard);
            }
        } else {
            // No Tx commit currently in progress - make this the current entry and proceed with
            // canCommit.
            cohortEntry.updateLastAccessTime();
            currentCohortEntry = cohortEntry;

            doCanCommit(cohortEntry);
        }
    }

    private void doCanCommit(final CohortEntry cohortEntry) {

        try {
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            Boolean canCommit = cohortEntry.getCohort().canCommit().get();

            cohortEntry.getCanCommitSender().tell(
                    canCommit ? CanCommitTransactionReply.YES.toSerializable() :
                        CanCommitTransactionReply.NO.toSerializable(), cohortEntry.getShard());

            if(!canCommit) {
                // Remove the entry from the cache now since the Tx will be aborted.
                removeCohortEntry(cohortEntry.getTransactionID());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.debug("{}: An exception occurred during canCommit: {}", name, e);

            // Remove the entry from the cache now since the Tx will be aborted.
            removeCohortEntry(cohortEntry.getTransactionID());
            cohortEntry.getCanCommitSender().tell(new Status.Failure(e), cohortEntry.getShard());
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
    void setCohortDecarator(CohortDecorator cohortDecorator) {
        this.cohortDecorator = cohortDecorator;
    }


    static class CohortEntry {
        private String transactionID;
        private DOMStoreThreePhaseCommitCohort cohort;
        private final MutableCompositeModification compositeModification;
        private final DOMStoreWriteTransaction transaction;
        private ActorRef canCommitSender;
        private ActorRef shard;
        private long lastAccessTime;

        CohortEntry(DOMStoreWriteTransaction transaction) {
            this.compositeModification = new MutableCompositeModification();
            this.transaction = transaction;
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

        void setTransactionID(String transactionID) {
            this.transactionID = transactionID;
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

        void ready(CohortDecorator cohortDecorator) {
            cohort = transaction.ready();

            if(cohortDecorator != null) {
                // Call the hook for unit tests.
                cohort = cohortDecorator.decorate(transactionID, cohort);
            }
        }

        ActorRef getCanCommitSender() {
            return canCommitSender;
        }

        void setCanCommitSender(ActorRef canCommitSender) {
            this.canCommitSender = canCommitSender;
        }

        ActorRef getShard() {
            return shard;
        }

        void setShard(ActorRef shard) {
            this.shard = shard;
        }
    }
}
