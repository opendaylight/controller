/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Status;
import akka.actor.Status.Failure;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

class CompositeDataTreeCohort {

    private enum State {
        IDLE, CAN_COMMIT_SEND, CAN_COMMIT_SUCCESSFUL, PRE_COMMIT_SEND, PRE_COMMIT_SUCCESSFUL, COMMIT_SEND, COMMITED, FAILED, ABORTED
    }

    private final DataTreeCohortActorRegistry registry;
    private final String txId;
    private final SchemaContext schema;
    private final Duration maxStepDuration;
    private final Timeout timeout;
    private Iterable<Success> successfulFromPrevious;
    private State state = State.IDLE;



    CompositeDataTreeCohort(DataTreeCohortActorRegistry registry, String txId, SchemaContext schema,
            FiniteDuration maxStepDuration) {
        this.registry = Preconditions.checkNotNull(registry);
        this.txId = Preconditions.checkNotNull(txId);
        this.schema = schema;
        this.maxStepDuration = Preconditions.checkNotNull(maxStepDuration);
        this.timeout = new Timeout(maxStepDuration);
    }

    boolean canCommit(DataTreeCandidateTip tip) throws TimeoutException {
        Collection<CanCommit> messages = registry.createCanCommitMessages(txId, tip, schema);
        Future<Iterable<Object>> canCommitsFuture =
                Futures.traverse(messages, new Function<CanCommit, Future<Object>>() {
                    @Override
                    public Future<Object> apply(CanCommit input) {
                        return Patterns.ask(input.getCohort(), input, timeout);
                    }
                }, ExecutionContexts.global());
        changeStateFrom(State.IDLE, State.CAN_COMMIT_SEND);
        return processResponses(canCommitsFuture, State.CAN_COMMIT_SEND, State.CAN_COMMIT_SUCCESSFUL);
    }

    void preCommit() throws TimeoutException {
        Preconditions.checkState(successfulFromPrevious != null);
        Future<Iterable<Object>> preCommitFutures = sendMesageToSuccessful(new DataTreeCohortActor.PreCommit(txId));
        changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SEND);
        processResponses(preCommitFutures, State.PRE_COMMIT_SEND, State.PRE_COMMIT_SUCCESSFUL);
    }

    void commit() throws TimeoutException {
        Preconditions.checkState(successfulFromPrevious != null);
        Future<Iterable<Object>> commitsFuture = sendMesageToSuccessful(new DataTreeCohortActor.Commit(txId));
        changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMIT_SEND);
        processResponses(commitsFuture, State.COMMIT_SEND, State.COMMITED);
    }

    void abort() throws TimeoutException {
        if (successfulFromPrevious != null) {
            sendMesageToSuccessful(new DataTreeCohortActor.Abort(txId));
        }
    }

    private Future<Iterable<Object>> sendMesageToSuccessful(final Object message) {
        return Futures.traverse(successfulFromPrevious, new Function<DataTreeCohortActor.Success, Future<Object>>() {

            @Override
            public Future<Object> apply(DataTreeCohortActor.Success cohortResponse) throws Exception {
                return Patterns.ask(cohortResponse.getCohort(), message, timeout);
            }

        }, ExecutionContexts.global());
    }

    private boolean processResponses(Future<Iterable<Object>> resultsFuture, State currentState, State afterState)
            throws TimeoutException {
        final Iterable<Object> results;
        try {
            results = Await.result(resultsFuture, maxStepDuration);
        } catch (Exception e) {
            successfulFromPrevious = null;
            Throwables.propagateIfInstanceOf(e, TimeoutException.class);
            throw Throwables.propagate(e);
        }
        Iterable<Failure> failed = Iterables.filter(results, Status.Failure.class);
        Iterable<Success> successful = Iterables.filter(results, DataTreeCohortActor.Success.class);
        successfulFromPrevious = successful;
        if (!Iterables.isEmpty(failed)) {
            changeStateFrom(currentState, State.FAILED);
            return false;
        }
        changeStateFrom(currentState, afterState);
        return true;
    }

    void changeStateFrom(State expected, State followup) {
        if (expected != null) {
            Preconditions.checkState(state == expected);
        }
        state = followup;
    }
}
