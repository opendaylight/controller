/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNER_QNAME;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Status.Failure;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.slf4j.Logger;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles commits and retries for the EntityOwnershipShard.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipShardCommitCoordinator {
    private static final Object COMMIT_RETRY_MESSAGE = new Object() {
        @Override
        public String toString() {
            return "entityCommitRetry";
        }
    };
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("entity-ownership-internal");

    private final Queue<Modification> pendingModifications = new LinkedList<>();
    private final LocalHistoryIdentifier historyId;
    private final Logger log;

    private BatchedModifications inflightCommit;
    private Cancellable retryCommitSchedule;
    private long transactionIDCounter = 0;

    EntityOwnershipShardCommitCoordinator(final MemberName localMemberName, final Logger log) {
        this.log = requireNonNull(log);
        historyId = new LocalHistoryIdentifier(
                ClientIdentifier.create(FrontendIdentifier.create(localMemberName, FRONTEND_TYPE), 0), 0);
    }

    boolean handleMessage(final Object message, final EntityOwnershipShard shard) {
        boolean handled = true;
        if (CommitTransactionReply.isSerializedType(message)) {
            // Successful reply from a local commit.
            inflightCommitSucceeded(shard);
        } else if (message instanceof akka.actor.Status.Failure) {
            // Failure reply from a local commit.
            inflightCommitFailure(((Failure) message).cause(), shard);
        } else if (COMMIT_RETRY_MESSAGE.equals(message)) {
            retryInflightCommit(shard);
        } else {
            handled = false;
        }

        return handled;
    }

    private void retryInflightCommit(final EntityOwnershipShard shard) {
        // Shouldn't be null happen but verify anyway
        if (inflightCommit == null) {
            return;
        }

        if (shard.hasLeader()) {
            log.debug("Retrying commit for BatchedModifications {}", inflightCommit.getTransactionId());

            shard.tryCommitModifications(inflightCommit);
        } else {
            scheduleInflightCommitRetry(shard);
        }
    }

    void inflightCommitFailure(final Throwable cause, final EntityOwnershipShard shard) {
        // This should've originated from a failed inflight commit but verify anyway
        if (inflightCommit == null) {
            return;
        }

        log.debug("Inflight BatchedModifications {} commit failed", inflightCommit.getTransactionId(), cause);

        if (!(cause instanceof NoShardLeaderException)) {
            // If the failure is other than NoShardLeaderException the commit may have been partially
            // processed so retry with a new transaction ID to be safe.
            newInflightCommitWithDifferentTransactionID();
        }

        scheduleInflightCommitRetry(shard);
    }

    private void scheduleInflightCommitRetry(final EntityOwnershipShard shard) {
        FiniteDuration duration = shard.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval();

        log.debug("Scheduling retry for BatchedModifications commit {} in {}",
                inflightCommit.getTransactionId(), duration);

        retryCommitSchedule = shard.getContext().system().scheduler().scheduleOnce(duration, shard.getSelf(),
                COMMIT_RETRY_MESSAGE, shard.getContext().dispatcher(), ActorRef.noSender());
    }

    void inflightCommitSucceeded(final EntityOwnershipShard shard) {
        // Shouldn't be null but verify anyway
        if (inflightCommit == null) {
            return;
        }

        if (retryCommitSchedule != null) {
            retryCommitSchedule.cancel();
        }

        log.debug("BatchedModifications commit {} succeeded", inflightCommit.getTransactionId());

        inflightCommit = null;
        commitNextBatch(shard);
    }

    void commitNextBatch(final EntityOwnershipShard shard) {
        if (inflightCommit != null || pendingModifications.isEmpty() || !shard.hasLeader()) {
            return;
        }

        inflightCommit = newBatchedModifications();
        Iterator<Modification> iter = pendingModifications.iterator();
        while (iter.hasNext()) {
            inflightCommit.addModification(iter.next());
            iter.remove();
            if (inflightCommit.getModifications().size()
                    >= shard.getDatastoreContext().getShardBatchedModificationCount()) {
                break;
            }
        }

        log.debug("Committing next BatchedModifications {}, size {}", inflightCommit.getTransactionId(),
                inflightCommit.getModifications().size());

        shard.tryCommitModifications(inflightCommit);
    }

    void commitModification(final Modification modification, final EntityOwnershipShard shard) {
        commitModifications(ImmutableList.of(modification), shard);
    }

    void commitModifications(final List<Modification> modifications, final EntityOwnershipShard shard) {
        if (modifications.isEmpty()) {
            return;
        }

        boolean hasLeader = shard.hasLeader();
        if (inflightCommit != null || !hasLeader) {
            if (log.isDebugEnabled()) {
                log.debug("{} - adding modifications to pending",
                        inflightCommit != null ? "A commit is inflight" : "No shard leader");
            }

            pendingModifications.addAll(modifications);
        } else {
            inflightCommit = newBatchedModifications();
            inflightCommit.addModifications(modifications);
            shard.tryCommitModifications(inflightCommit);
        }
    }

    void onStateChanged(final EntityOwnershipShard shard, final boolean isLeader) {
        shard.possiblyRemoveAllInitialCandidates(shard.getLeader());

        possiblyPrunePendingCommits(shard, isLeader);

        if (!isLeader && inflightCommit != null) {
            // We're no longer the leader but we have an inflight local commit. This likely means we didn't get
            // consensus for the commit and switched to follower due to another node with a higher term. We
            // can't be sure if the commit was replicated to any node so we retry it here with a new
            // transaction ID.
            if (retryCommitSchedule != null) {
                retryCommitSchedule.cancel();
            }

            newInflightCommitWithDifferentTransactionID();
            retryInflightCommit(shard);
        } else {
            commitNextBatch(shard);
        }
    }

    private void possiblyPrunePendingCommits(final EntityOwnershipShard shard, final boolean isLeader) {
        // If we were the leader and transitioned to follower, we'll try to forward pending commits to the new leader.
        // However certain commits, e.g. entity owner changes, should only be committed by a valid leader as the
        // criteria used to determine the commit may be stale. Since we're no longer a valid leader, we should not
        // forward such commits thus we prune the pending modifications. We still should forward local candidate change
        // commits.
        if (shard.hasLeader() && !isLeader) {
            // We may have already submitted a transaction for replication and commit. We don't need the base Shard to
            // forward it since we also have it stored in the inflightCommit and handle retries. So we just clear
            // pending transactions and drop them.
            shard.convertPendingTransactionsToMessages();

            // Prune the inflightCommit.
            if (inflightCommit != null) {
                inflightCommit = pruneModifications(inflightCommit);
            }

            // Prune the subsequent pending modifications.
            pendingModifications.removeIf(mod -> !canForwardModificationToNewLeader(mod));
        }
    }

    private @Nullable BatchedModifications pruneModifications(final BatchedModifications toPrune) {
        BatchedModifications prunedModifications = new BatchedModifications(toPrune.getTransactionId(),
                toPrune.getVersion());
        prunedModifications.setDoCommitOnReady(toPrune.isDoCommitOnReady());
        if (toPrune.isReady()) {
            prunedModifications.setReady(toPrune.getParticipatingShardNames());
        }
        prunedModifications.setTotalMessagesSent(toPrune.getTotalMessagesSent());
        for (Modification mod: toPrune.getModifications()) {
            if (canForwardModificationToNewLeader(mod)) {
                prunedModifications.addModification(mod);
            }
        }

        return !prunedModifications.getModifications().isEmpty() ? prunedModifications : null;
    }

    private boolean canForwardModificationToNewLeader(final Modification mod) {
        // If this is a WRITE of entity owner we don't want to forward it to a new leader since the criteria used
        // to determine the new owner might be stale.
        if (mod instanceof WriteModification) {
            WriteModification writeMod = (WriteModification)mod;
            boolean canForward = !writeMod.getPath().getLastPathArgument().getNodeType().equals(ENTITY_OWNER_QNAME);

            if (!canForward) {
                log.debug("Not forwarding WRITE modification for {} to new leader", writeMod.getPath());
            }

            return canForward;
        }

        return true;
    }

    private void newInflightCommitWithDifferentTransactionID() {
        BatchedModifications newBatchedModifications = newBatchedModifications();
        newBatchedModifications.addModifications(inflightCommit.getModifications());
        inflightCommit = newBatchedModifications;
    }

    private BatchedModifications newBatchedModifications() {
        BatchedModifications modifications = new BatchedModifications(
            new TransactionIdentifier(historyId, ++transactionIDCounter), DataStoreVersions.CURRENT_VERSION);
        modifications.setDoCommitOnReady(true);
        modifications.setReady();
        modifications.setTotalMessagesSent(1);
        return modifications;
    }
}
