/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortDecorator;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;

final class CohortEntry {
    enum State {
        PENDING,
        CAN_COMMITTED,
        PRE_COMMITTED,
        COMMITTED,
        ABORTED
    }

    private static final Timeout COMMIT_STEP_TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    private final Stopwatch lastAccessTimer = Stopwatch.createStarted();
    private final ReadWriteShardDataTreeTransaction transaction;
    private final TransactionIdentifier transactionID;
    private final CompositeDataTreeCohort userCohorts;
    private final short clientVersion;

    private State state = State.PENDING;
    private RuntimeException lastBatchedModificationsException;
    private int totalBatchedModificationsReceived;
    private ShardDataTreeCohort cohort;
    private boolean doImmediateCommit;
    private ActorRef replySender;
    private Shard shard;

    private CohortEntry(final TransactionIdentifier transactionID, final ReadWriteShardDataTreeTransaction transaction,
            final DataTreeCohortActorRegistry cohortRegistry, final SchemaContext schema, final short clientVersion) {
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionID = Preconditions.checkNotNull(transactionID);
        this.clientVersion = clientVersion;
        this.userCohorts = new CompositeDataTreeCohort(cohortRegistry, transactionID, schema, COMMIT_STEP_TIMEOUT);
    }

    private CohortEntry(final TransactionIdentifier transactionID, final ShardDataTreeCohort cohort, final DataTreeCohortActorRegistry cohortRegistry,
            final SchemaContext schema, final short clientVersion) {
        this.transactionID = Preconditions.checkNotNull(transactionID);
        this.cohort = cohort;
        this.transaction = null;
        this.clientVersion = clientVersion;
        this.userCohorts = new CompositeDataTreeCohort(cohortRegistry, transactionID, schema, COMMIT_STEP_TIMEOUT);
    }

    static CohortEntry createOpen(final TransactionIdentifier transactionID, final ReadWriteShardDataTreeTransaction transaction,
            final DataTreeCohortActorRegistry cohortRegistry, final SchemaContext schema, final short clientVersion) {
        return new CohortEntry(transactionID, transaction, cohortRegistry, schema, clientVersion);
    }

    static CohortEntry createReady(final TransactionIdentifier transactionID, final ShardDataTreeCohort cohort,
            final DataTreeCohortActorRegistry cohortRegistry, final SchemaContext schema, final short clientVersion) {
        return new CohortEntry(transactionID, cohort, cohortRegistry, schema, clientVersion);
    }

    void updateLastAccessTime() {
        lastAccessTimer.reset();
        lastAccessTimer.start();
    }

    TransactionIdentifier getTransactionID() {
        return transactionID;
    }

    short getClientVersion() {
        return clientVersion;
    }

    State getState() {
        return state;
    }

    DataTreeCandidate getCandidate() {
        return cohort.getCandidate();
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
        state = State.CAN_COMMITTED;
        cohort.canCommit(callback);
    }

    void preCommit(final FutureCallback<Void> callback) {
        state = State.PRE_COMMITTED;
        cohort.preCommit(new FutureCallback<DataTreeCandidateTip>() {
            @Override
            public void onSuccess(final DataTreeCandidateTip result) {

                try {
                    userCohorts.canCommit(result);
                    userCohorts.preCommit();
                } catch (ExecutionException e) {
                    callback.onFailure(e.getCause());
                    return;
                } catch (TimeoutException e) {
                    callback.onFailure(e);
                    return;
                }

                callback.onSuccess(null);
            }

            @Override
            public void onFailure(final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    void commit() throws InterruptedException, ExecutionException, TimeoutException {
        state = State.COMMITTED;
        cohort.commit().get();
        userCohorts.commit();
    }

    void abort() throws InterruptedException, ExecutionException, TimeoutException {
        state = State.ABORTED;
        cohort.abort().get();
        userCohorts.abort();
    }

    void ready(final CohortDecorator cohortDecorator, final boolean doImmediateCommit) {
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

    boolean isExpired(final long expireTimeInMillis) {
        return lastAccessTimer.elapsed(TimeUnit.MILLISECONDS) >= expireTimeInMillis;
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


    boolean isAborted() {
        return state == State.ABORTED;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CohortEntry [transactionID=").append(transactionID).append(", doImmediateCommit=")
                .append(doImmediateCommit).append("]");
        return builder.toString();
    }
}