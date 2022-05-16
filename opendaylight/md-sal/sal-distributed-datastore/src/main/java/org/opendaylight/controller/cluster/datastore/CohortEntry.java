/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortDecorator;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

final class CohortEntry {
    private final ReadWriteShardDataTreeTransaction transaction;
    private final TransactionIdentifier transactionId;
    private final short clientVersion;

    private RuntimeException lastBatchedModificationsException;
    private int totalBatchedModificationsReceived;
    private int totalOperationsProcessed;
    private ShardDataTreeCohort cohort;
    private boolean doImmediateCommit;
    private ActorRef replySender;
    private Shard shard;

    private CohortEntry(final ReadWriteShardDataTreeTransaction transaction, final short clientVersion) {
        cohort = null;
        this.transaction = requireNonNull(transaction);
        transactionId = transaction.getIdentifier();
        this.clientVersion = clientVersion;
    }

    private CohortEntry(final ShardDataTreeCohort cohort, final short clientVersion) {
        this.cohort = requireNonNull(cohort);
        transactionId = cohort.getIdentifier();
        transaction = null;
        this.clientVersion = clientVersion;
    }

    static CohortEntry createOpen(final ReadWriteShardDataTreeTransaction transaction, final short clientVersion) {
        return new CohortEntry(transaction, clientVersion);
    }

    static CohortEntry createReady(final ShardDataTreeCohort cohort, final short clientVersion) {
        return new CohortEntry(cohort, clientVersion);
    }

    TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    short getClientVersion() {
        return clientVersion;
    }

    boolean isFailed() {
        return cohort != null && cohort.isFailed();
    }

    DataTreeModification getDataTreeModification() {
        return cohort.getDataTreeModification();
    }

    ReadWriteShardDataTreeTransaction getTransaction() {
        return transaction;
    }

    int getTotalBatchedModificationsReceived() {
        return totalBatchedModificationsReceived;
    }

    int getTotalOperationsProcessed() {
        return totalOperationsProcessed;
    }

    RuntimeException getLastBatchedModificationsException() {
        return lastBatchedModificationsException;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", justification = "Re-thrown")
    void applyModifications(final List<Modification> modifications) {
        totalBatchedModificationsReceived++;
        if (lastBatchedModificationsException == null) {
            totalOperationsProcessed += modifications.size();
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

    void canCommit(final FutureCallback<Empty> callback) {
        cohort.canCommit(callback);
    }

    void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        cohort.preCommit(callback);
    }

    void commit(final FutureCallback<UnsignedLong> callback) {
        cohort.commit(callback);
    }

    void abort(final FutureCallback<Empty> callback) {
        cohort.abort(callback);
    }

    void ready(final Optional<SortedSet<String>> participatingShardNames, final CohortDecorator cohortDecorator) {
        checkState(cohort == null, "cohort was already set");

        cohort = transaction.ready(participatingShardNames);

        if (cohortDecorator != null) {
            // Call the hook for unit tests.
            cohort = cohortDecorator.decorate(transactionId, cohort);
        }
    }

    boolean isSealed() {
        return cohort != null;
    }

    Optional<SortedSet<String>> getParticipatingShardNames() {
        return cohort != null ? cohort.getParticipatingShardNames() : Optional.empty();
    }

    boolean isDoImmediateCommit() {
        return doImmediateCommit;
    }

    void setDoImmediateCommit(final boolean doImmediateCommit) {
        this.doImmediateCommit = doImmediateCommit;
    }

    ActorRef getReplySender() {
        return replySender;
    }

    void setReplySender(final ActorRef replySender) {
        this.replySender = replySender;
    }

    Shard getShard() {
        return shard;
    }

    void setShard(final Shard shard) {
        this.shard = shard;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CohortEntry [transactionId=").append(transactionId).append(", doImmediateCommit=")
                .append(doImmediateCommit).append("]");
        return builder.toString();
    }
}
