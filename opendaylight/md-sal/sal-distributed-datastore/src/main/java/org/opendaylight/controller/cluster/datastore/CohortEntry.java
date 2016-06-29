/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortDecorator;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class CohortEntry {
    private final ReadWriteShardDataTreeTransaction transaction;
    private final TransactionIdentifier transactionID;
    private final short clientVersion;

    private RuntimeException lastBatchedModificationsException;
    private int totalBatchedModificationsReceived;
    private ShardDataTreeCohort cohort;
    private boolean doImmediateCommit;
    private ActorRef replySender;
    private Shard shard;

    private CohortEntry(final ReadWriteShardDataTreeTransaction transaction, final short clientVersion) {
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionID = transaction.getIdentifier();
        this.clientVersion = clientVersion;
    }

    private CohortEntry(final ShardDataTreeCohort cohort, final short clientVersion) {
        this.cohort = Preconditions.checkNotNull(cohort);
        this.transactionID = cohort.getIdentifier();
        this.transaction = null;
        this.clientVersion = clientVersion;
    }

    static CohortEntry createOpen(final ReadWriteShardDataTreeTransaction transaction, final short clientVersion) {
        return new CohortEntry(transaction, clientVersion);
    }

    static CohortEntry createReady(final ShardDataTreeCohort cohort, final short clientVersion) {
        return new CohortEntry(cohort, clientVersion);
    }

    TransactionIdentifier getTransactionID() {
        return transactionID;
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

    RuntimeException getLastBatchedModificationsException() {
        return lastBatchedModificationsException;
    }

    void applyModifications(final Iterable<Modification> modifications) {
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

    void canCommit(final FutureCallback<Void> callback) {
        cohort.canCommit(callback);
    }

    void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        cohort.preCommit(callback);
    }

    void commit(final FutureCallback<UnsignedLong> callback) {
        cohort.commit(callback);
    }

    void abort() throws InterruptedException, ExecutionException, TimeoutException {
        cohort.abort().get();
    }

    void ready(final CohortDecorator cohortDecorator) {
        Preconditions.checkState(cohort == null, "cohort was already set");

        cohort = transaction.ready();

        if(cohortDecorator != null) {
            // Call the hook for unit tests.
            cohort = cohortDecorator.decorate(transactionID, cohort);
        }
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
        builder.append("CohortEntry [transactionID=").append(transactionID).append(", doImmediateCommit=")
                .append(doImmediateCommit).append("]");
        return builder.toString();
    }
}