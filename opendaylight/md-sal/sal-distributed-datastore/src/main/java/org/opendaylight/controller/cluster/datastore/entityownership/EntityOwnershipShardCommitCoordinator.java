/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Status.Failure;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.slf4j.Logger;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles commits and retries for the EntityOwnershipShard.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipShardCommitCoordinator {
    private static final Object COMMIT_RETRY_MESSAGE = "entityCommitRetry";

    private final Logger log;
    private int transactionIDCounter = 0;
    private final String localMemberName;
    private final Queue<Modification> pendingModifications = new LinkedList<>();
    private BatchedModifications inflightCommit;
    private Cancellable retryCommitSchedule;

    EntityOwnershipShardCommitCoordinator(String localMemberName, Logger log) {
        this.localMemberName = localMemberName;
        this.log = log;
    }

    boolean handleMessage(Object message, EntityOwnershipShard shard) {
        boolean handled = true;
        if(CommitTransactionReply.isSerializedType(message)) {
            // Successful reply from a local commit.
            inflightCommitSucceeded(shard);
        } else if(message instanceof akka.actor.Status.Failure) {
            // Failure reply from a local commit.
            inflightCommitFailure(((Failure)message).cause(), shard);
        } else if(message.equals(COMMIT_RETRY_MESSAGE)) {
            retryInflightCommit(shard);
        } else {
            handled = false;
        }

        return handled;
    }

    private void retryInflightCommit(EntityOwnershipShard shard) {
        // Shouldn't be null happen but verify anyway
        if(inflightCommit == null) {
            return;
        }

        if(shard.hasLeader()) {
            log.debug("Retrying commit for BatchedModifications {}", inflightCommit.getTransactionID());

            shard.tryCommitModifications(inflightCommit);
        } else {
            scheduleInflightCommitRetry(shard);
        }
    }

    void inflightCommitFailure(Throwable cause, EntityOwnershipShard shard) {
        // This should've originated from a failed inflight commit but verify anyway
        if(inflightCommit == null) {
            return;
        }

        log.debug("Inflight BatchedModifications {} commit failed", inflightCommit.getTransactionID(), cause);

        if(!(cause instanceof NoShardLeaderException)) {
            // If the failure is other than NoShardLeaderException the commit may have been partially
            // processed so retry with a new transaction ID to be safe.
            newInflightCommitWithDifferentTransactionID();
        }

        scheduleInflightCommitRetry(shard);
    }

    private void scheduleInflightCommitRetry(EntityOwnershipShard shard) {
        FiniteDuration duration = shard.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval();

        log.debug("Scheduling retry for BatchedModifications commit {} in {}",
                inflightCommit.getTransactionID(), duration);

        retryCommitSchedule = shard.getContext().system().scheduler().scheduleOnce(duration, shard.getSelf(),
                COMMIT_RETRY_MESSAGE, shard.getContext().dispatcher(), ActorRef.noSender());
    }

    void inflightCommitSucceeded(EntityOwnershipShard shard) {
        // Shouldn't be null but verify anyway
        if(inflightCommit == null) {
            return;
        }

        if(retryCommitSchedule != null) {
            retryCommitSchedule.cancel();
        }

        log.debug("BatchedModifications commit {} succeeded", inflightCommit.getTransactionID());

        inflightCommit = null;
        commitNextBatch(shard);
    }

    void commitNextBatch(EntityOwnershipShard shard) {
        if(inflightCommit != null || pendingModifications.isEmpty() || !shard.hasLeader()) {
            return;
        }

        inflightCommit = newBatchedModifications();
        Iterator<Modification> iter = pendingModifications.iterator();
        while(iter.hasNext()) {
            inflightCommit.addModification(iter.next());
            iter.remove();
            if(inflightCommit.getModifications().size() >=
                    shard.getDatastoreContext().getShardBatchedModificationCount()) {
                break;
            }
        }

        log.debug("Committing next BatchedModifications {}, size {}", inflightCommit.getTransactionID(),
                inflightCommit.getModifications().size());

        shard.tryCommitModifications(inflightCommit);
    }

    void commitModification(Modification modification, EntityOwnershipShard shard) {
        BatchedModifications modifications = newBatchedModifications();
        modifications.addModification(modification);
        commitModifications(modifications, shard);
    }

    void commitModifications(BatchedModifications modifications, EntityOwnershipShard shard) {
        if(modifications.getModifications().isEmpty()) {
            return;
        }

        boolean hasLeader = shard.hasLeader();
        if(inflightCommit != null || !hasLeader) {
            if(log.isDebugEnabled()) {
                log.debug("{} - adding modifications to pending",
                        (inflightCommit != null ? "A commit is inflight" : "No shard leader"));
            }

            pendingModifications.addAll(modifications.getModifications());
        } else {
            inflightCommit = modifications;
            shard.tryCommitModifications(inflightCommit);
        }
    }

    void onStateChanged(EntityOwnershipShard shard, boolean isLeader) {
        if(!isLeader && inflightCommit != null) {
            // We're no longer the leader but we have an inflight local commit. This likely means we didn't get
            // consensus for the commit and switched to follower due to another node with a higher term. We
            // can't be sure if the commit was replicated to any node so we retry it here with a new
            // transaction ID.
            if(retryCommitSchedule != null) {
                retryCommitSchedule.cancel();
            }

            newInflightCommitWithDifferentTransactionID();
            retryInflightCommit(shard);
        } else {
            commitNextBatch(shard);
        }
    }

    private void newInflightCommitWithDifferentTransactionID() {
        BatchedModifications newBatchedModifications = newBatchedModifications();
        newBatchedModifications.getModifications().addAll(inflightCommit.getModifications());
        inflightCommit = newBatchedModifications;
    }

    BatchedModifications newBatchedModifications() {
        BatchedModifications modifications = new BatchedModifications(
                TransactionIdentifier.create(localMemberName, ++transactionIDCounter).toString(),
                DataStoreVersions.CURRENT_VERSION, "");
        modifications.setDoCommitOnReady(true);
        modifications.setReady(true);
        modifications.setTotalMessagesSent(1);
        return modifications;
    }
}
