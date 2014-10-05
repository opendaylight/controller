/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.ActorRef;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Coordinates commits for a shard ensuring that one concurrent 3-phase commit.
 *
 * @author Thomas Pantelis
 */
public class ShardCommitCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(ShardCommitCoordinator.class);

    private static final int CACHE_EXPIRATION_IN_MINUTES = 5;

    // We expire entries in case canCommit does not occur after a transaction is readied. This
    // should be rare - could occur due to network interruption between canCommit and ready.
    private final Cache<String, CohortEntry> cohortCache = CacheBuilder.newBuilder().expireAfterAccess(
            CACHE_EXPIRATION_IN_MINUTES, TimeUnit.MINUTES).build();

    private CohortEntry currentCohortEntry;

    private final Queue<CohortEntry> queuedCohortEntries = new LinkedList<>();

    public void transactionReady(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
            Modification modification) {

        cohortCache.put(transactionID, new CohortEntry(transactionID, cohort, modification));
    }

    public void handleCanCommit(CanCommitTransaction canCommit, final ActorRef sender,
            final ActorRef shard) {
        String transactionID = canCommit.getTransactionID();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Processing canCommit for transaction {} for shard {}",
                    transactionID, shard.path());
        }

        final CohortEntry cohortEntry = cohortCache.getIfPresent(transactionID);
        if(cohortEntry == null) {
            // Either canCommit was invoked before ready or a long time passed between
            // canCommit and ready and the entry was expired from the cache. Either way this
            // shouldn't happen.
            IllegalStateException ex = new IllegalStateException(
                    "No cohort entry found for transaction " + transactionID);
            LOG.error(ex.getMessage());
            sender.tell(new akka.actor.Status.Failure(ex), shard);
            return;
        }

        // Remove from the cache - the CohortEntry will either become the current entry or be put
        // on the queue.
        cohortCache.invalidate(transactionID);

        cohortEntry.setCanCommitSender(sender);
        cohortEntry.setShard(shard);

        if(currentCohortEntry != null) {
            LOG.debug("Transaction {} is already in progress - queueing transaction {}",
                    currentCohortEntry.getTransactionID(), transactionID);
            queuedCohortEntries.offer(cohortEntry);
            // TODO - need to set a timer
            return;
        }

        currentCohortEntry = cohortEntry;

        doCanCommit(cohortEntry);
    }

    private void doCanCommit(final CohortEntry cohortEntry) {
        ListenableFuture<Boolean> future = cohortEntry.getCohort().canCommit();
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean canCommit) {
                cohortEntry.getCanCommitSender().tell(
                        new CanCommitTransactionReply(canCommit).toSerializable(),
                        cohortEntry.getShard());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.debug("An exception occurred during canCommit", t);
                cohortEntry.getCanCommitSender().tell(new akka.actor.Status.Failure(t),
                        cohortEntry.getShard());
            }
        });
    }

    public CohortEntry getCohortEntryIfCurrent(String transactionID) {
        if(isCurrentTransaction(transactionID)) {
            return currentCohortEntry;
        }

        return null;
    }

    public boolean isCurrentTransaction(String transactionID) {
        return currentCohortEntry != null &&
                currentCohortEntry.getTransactionID().equals(transactionID);
    }

    public CohortEntry getCurrentCohortEntry() {
        return currentCohortEntry;
    }

    public void currentTransactionComplete() {
        currentCohortEntry = queuedCohortEntries.poll();
        if(currentCohortEntry != null) {
            doCanCommit(currentCohortEntry);
        }
    }

    static class CohortEntry {
        private final String transactionID;
        private final DOMStoreThreePhaseCommitCohort cohort;
        private final Modification modification;
        private ActorRef canCommitSender;
        private ActorRef shard;

        CohortEntry(String transactionID, DOMStoreThreePhaseCommitCohort cohort,
                Modification modification) {
            this.transactionID = transactionID;
            this.cohort = cohort;
            this.modification = modification;
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
