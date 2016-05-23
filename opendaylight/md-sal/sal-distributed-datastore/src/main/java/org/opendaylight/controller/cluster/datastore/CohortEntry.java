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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortDecorator;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
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
    private final TransactionIdentifier<?> transactionID;
    private final CompositeDataTreeCohort userCohorts;
    private final ABIVersion clientVersion;

    private State state = State.PENDING;
    private RuntimeException lastBatchedModificationsException;
    private int totalBatchedModificationsReceived;
    private ShardDataTreeCohort cohort;
    private boolean doImmediateCommit;
    private ActorRef replySender;
    private Shard shard;

    CohortEntry(TransactionIdentifier<?> transactionID, ReadWriteShardDataTreeTransaction transaction,
            DataTreeCohortActorRegistry cohortRegistry, SchemaContext schema, ABIVersion clientVersion) {
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionID = Preconditions.checkNotNull(transactionID);
        this.clientVersion = Preconditions.checkNotNull(clientVersion);
        this.userCohorts = new CompositeDataTreeCohort(cohortRegistry, transactionID, schema, COMMIT_STEP_TIMEOUT);
    }

    CohortEntry(TransactionIdentifier<?> transactionID, ShardDataTreeCohort cohort, DataTreeCohortActorRegistry cohortRegistry,
            SchemaContext schema, ABIVersion clientVersion) {
        this.transactionID = Preconditions.checkNotNull(transactionID);
        this.cohort = cohort;
        this.transaction = null;
        this.clientVersion = Preconditions.checkNotNull(clientVersion);
        this.userCohorts = new CompositeDataTreeCohort(cohortRegistry, transactionID, schema, COMMIT_STEP_TIMEOUT);
    }

    void updateLastAccessTime() {
        lastAccessTimer.reset();
        lastAccessTimer.start();
    }

    TransactionIdentifier<?> getTransactionID() {
        return transactionID;
    }

    ABIVersion getClientVersion() {
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
        state = State.CAN_COMMITTED;

        // We block on the future here (and also preCommit(), commit(), abort()) so we don't have to worry
        // about possibly accessing our state on a different thread outside of our dispatcher.
        // TODO: the ShardDataTreeCohort returns immediate Futures anyway which begs the question - why
        // bother even returning Futures from ShardDataTreeCohort if we have to treat them synchronously
        // anyway?. The Futures are really a remnant from when we were using the InMemoryDataBroker.
        return cohort.canCommit().get();
    }



    void preCommit() throws InterruptedException, ExecutionException, TimeoutException {
        state = State.PRE_COMMITTED;
        cohort.preCommit().get();
        userCohorts.canCommit(cohort.getCandidate());
        userCohorts.preCommit();
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