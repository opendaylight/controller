/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.ActorRef;
import akka.actor.Status;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Coordinates commits for a shard ensuring only one concurrent 3-phase commit.
 *
 * @author Thomas Pantelis
 */
public class ShardCommitCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(ShardCommitCoordinator.class);

    private static final Object CAN_COMMIT_REPLY_TRUE =
            new CanCommitTransactionReply(Boolean.TRUE).toSerializable();

    private static final Object CAN_COMMIT_REPLY_FALSE =
            new CanCommitTransactionReply(Boolean.FALSE).toSerializable();

    private final Cache<String, CohortEntry> cohortCache;

    private CohortEntry currentCohortEntry;

    private final Queue<CohortEntry> queuedCohortEntries;

    private final int queueCapacity;

    public ShardCommitCoordinator(long cacheExpiryTimeoutInSec, int queueCapacity) {
        cohortCache = CacheBuilder.newBuilder().expireAfterAccess(
                cacheExpiryTimeoutInSec, TimeUnit.SECONDS).build();

        // We use a LinkedList here to avoid synchronization overhead with concurrent queue impls
        // since this should only be accessed on the shard's dispatcher.
        queuedCohortEntries = new ArrayDeque<>(queueCapacity);
        this.queueCapacity = queueCapacity;
    }

    /**
     * This method caches a cohort entry for the given transactions ID in preparation for the
     * subsequent 3-phase commit.
     *
     * @param transactionID the ID of the transaction
     * @param cohort the cohort to participate in the transaction commit
     * @param modification the modification made by the transaction
     */
    public void transactionReady(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
            Modification modification) {

        cohortCache.put(transactionID, new CohortEntry(transactionID, cohort, modification));
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
        if(LOG.isDebugEnabled()) {
            LOG.debug("Processing canCommit for transaction {} for shard {}",
                    transactionID, shard.path());
        }

        // Lookup the cohort entry that was cached previously (or should have been) by
        // transactionReady (via the ForwardedReadyTransaction message).
        final CohortEntry cohortEntry = cohortCache.getIfPresent(transactionID);
        if(cohortEntry == null) {
            // Either canCommit was invoked before ready(shouldn't happen)  or a long time passed
            // between canCommit and ready and the entry was expired from the cache.
            IllegalStateException ex = new IllegalStateException(
                    String.format("No cohort entry found for transaction %s", transactionID));
            LOG.error(ex.getMessage());
            sender.tell(new Status.Failure(ex), shard);
            return;
        }

        cohortEntry.setCanCommitSender(sender);
        cohortEntry.setShard(shard);

        if(currentCohortEntry != null) {
            // There's already a Tx commit in progress - attempt to queue this entry to be
            // committed after the current Tx completes.
            LOG.debug("Transaction {} is already in progress - queueing transaction {}",
                    currentCohortEntry.getTransactionID(), transactionID);

            if (!queuedCohortEntries.offer(cohortEntry)) {
                removeCohortEntry(transactionID);

                RuntimeException ex = new RuntimeException(
                        String.format("Could not enqueue transaction %s - the maximum commit queue"+
                                      " capacity %d has been reached.",
                                transactionID, queueCapacity));
                LOG.error(ex.getMessage());
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
                    canCommit ? CAN_COMMIT_REPLY_TRUE : CAN_COMMIT_REPLY_FALSE, cohortEntry.getShard());

            if(!canCommit) {
                // Remove the entry from the cache now since the Tx will be aborted.
                removeCohortEntry(cohortEntry.getTransactionID());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.debug("An exception occurred during canCommit", e);

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
                doCanCommit(currentCohortEntry);
            }
        }
    }

    static class CohortEntry {
        private final String transactionID;
        private final DOMStoreThreePhaseCommitCohort cohort;
        private final Modification modification;
        private ActorRef canCommitSender;
        private ActorRef shard;
        private long lastAccessTime;

        CohortEntry(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
                Modification modification) {
            this.transactionID = transactionID;
            this.cohort = cohort;
            this.modification = modification;
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

        Modification getModification() {
            return modification;
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
