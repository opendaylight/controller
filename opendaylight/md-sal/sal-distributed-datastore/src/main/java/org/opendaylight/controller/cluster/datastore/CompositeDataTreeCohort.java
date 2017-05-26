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
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * Composite cohort, which coordinates multiple user-provided cohorts as if it was only one cohort.
 * <p/>
 * It tracks current operation and list of cohorts which successfuly finished previous phase in
 * case, if abort is necessary to invoke it only on cohort steps which are still active.
 *
 */
class CompositeDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeDataTreeCohort.class);

    private enum State {
        /**
         * Cohorts are idle, no messages were sent.
         */
        IDLE,
        /**
         * CanCommit message was sent to all participating cohorts.
         */
        CAN_COMMIT_SENT,
        /**
         * Successful canCommit responses were received from every participating cohort.
         */
        CAN_COMMIT_SUCCESSFUL,
        /**
         * PreCommit message was sent to all participating cohorts.
         */
        PRE_COMMIT_SENT,
        /**
         * Successful preCommit responses were received from every participating cohort.
         */
        PRE_COMMIT_SUCCESSFUL,
        /**
         * Commit message was send to all participating cohorts.
         */
        COMMIT_SENT,
        /**
         * Successful commit responses were received from all participating cohorts.
         */
        COMMITED,
        /**
         * Some of cohorts responded back with unsuccessful message.
         */
        FAILED,
        /**
         * Abort message was send to all cohorts which responded with success previously.
         */
        ABORTED
    }

    protected static final Recover<Object> EXCEPTION_TO_MESSAGE = new Recover<Object>() {
        @Override
        public Failure recover(final Throwable error) throws Throwable {
            return new Failure(error);
        }
    };


    private final DataTreeCohortActorRegistry registry;
    private final TransactionIdentifier txId;
    private final SchemaContext schema;
    private final Timeout timeout;
    private Iterable<Success> successfulFromPrevious;
    private State state = State.IDLE;

    CompositeDataTreeCohort(final DataTreeCohortActorRegistry registry, final TransactionIdentifier transactionID,
        final SchemaContext schema, final Timeout timeout) {
        this.registry = Preconditions.checkNotNull(registry);
        this.txId = Preconditions.checkNotNull(transactionID);
        this.schema = Preconditions.checkNotNull(schema);
        this.timeout = Preconditions.checkNotNull(timeout);
    }

    void reset() {
        switch (state) {
            case CAN_COMMIT_SENT:
            case CAN_COMMIT_SUCCESSFUL:
            case PRE_COMMIT_SENT:
            case PRE_COMMIT_SUCCESSFUL:
            case COMMIT_SENT:
                abort();
                break;
            default :
                break;
        }

        successfulFromPrevious = null;
        state = State.IDLE;
    }

    void canCommit(final DataTreeCandidate tip) throws ExecutionException, TimeoutException {
        LOG.debug("{}: canCommit -  candidate: {}", txId, tip);

        Collection<CanCommit> messages = registry.createCanCommitMessages(txId, tip, schema);

        LOG.debug("{}: canCommit - messages: {}", txId, messages);

        // FIXME: Optimize empty collection list with pre-created futures, containing success.
        Future<Iterable<Object>> canCommitsFuture = Futures.traverse(messages,
            input -> Patterns.ask(input.getCohort(), input, timeout).recover(EXCEPTION_TO_MESSAGE,
                    ExecutionContexts.global()), ExecutionContexts.global());
        changeStateFrom(State.IDLE, State.CAN_COMMIT_SENT);
        processResponses(canCommitsFuture, State.CAN_COMMIT_SENT, State.CAN_COMMIT_SUCCESSFUL);
    }

    void preCommit() throws ExecutionException, TimeoutException {
        LOG.debug("{}: preCommit - successfulFromPrevious: {}", txId, successfulFromPrevious);

        Preconditions.checkState(successfulFromPrevious != null);
        Future<Iterable<Object>> preCommitFutures = sendMesageToSuccessful(new DataTreeCohortActor.PreCommit(txId));
        changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SENT);
        processResponses(preCommitFutures, State.PRE_COMMIT_SENT, State.PRE_COMMIT_SUCCESSFUL);
    }

    void commit() throws ExecutionException, TimeoutException {
        LOG.debug("{}: commit - successfulFromPrevious: {}", txId, successfulFromPrevious);

        Preconditions.checkState(successfulFromPrevious != null);
        Future<Iterable<Object>> commitsFuture = sendMesageToSuccessful(new DataTreeCohortActor.Commit(txId));
        changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMIT_SENT);
        processResponses(commitsFuture, State.COMMIT_SENT, State.COMMITED);
    }

    Optional<Future<Iterable<Object>>> abort() {
        LOG.debug("{}: abort - successfulFromPrevious: {}", txId, successfulFromPrevious);

        state = State.ABORTED;
        if (successfulFromPrevious != null && !Iterables.isEmpty(successfulFromPrevious)) {
            return Optional.of(sendMesageToSuccessful(new DataTreeCohortActor.Abort(txId)));
        }

        return Optional.empty();
    }

    private Future<Iterable<Object>> sendMesageToSuccessful(final Object message) {
        LOG.debug("{}: sendMesageToSuccessful: {}", txId, message);

        return Futures.traverse(successfulFromPrevious, cohortResponse -> Patterns.ask(
                cohortResponse.getCohort(), message, timeout), ExecutionContexts.global());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void processResponses(final Future<Iterable<Object>> resultsFuture, final State currentState,
            final State afterState) throws TimeoutException, ExecutionException {
        LOG.debug("{}: processResponses - currentState: {}, afterState: {}", txId, currentState, afterState);

        final Iterable<Object> results;
        try {
            results = Await.result(resultsFuture, timeout.duration());
        } catch (Exception e) {
            successfulFromPrevious = null;
            LOG.debug("{}: processResponses - error from Future", txId, e);
            Throwables.propagateIfInstanceOf(e, TimeoutException.class);
            throw Throwables.propagate(e);
        }
        Iterable<Failure> failed = Iterables.filter(results, Status.Failure.class);
        Iterable<Success> successful = Iterables.filter(results, DataTreeCohortActor.Success.class);

        LOG.debug("{}: processResponses - successful: {}, failed: {}", txId, successful, failed);

        successfulFromPrevious = successful;
        if (!Iterables.isEmpty(failed)) {
            changeStateFrom(currentState, State.FAILED);
            Iterator<Failure> it = failed.iterator();
            Throwable firstEx = it.next().cause();
            while (it.hasNext()) {
                firstEx.addSuppressed(it.next().cause());
            }
            Throwables.propagateIfPossible(firstEx, ExecutionException.class);
            Throwables.propagateIfPossible(firstEx, TimeoutException.class);
            throw Throwables.propagate(firstEx);
        }
        changeStateFrom(currentState, afterState);
    }

    void changeStateFrom(final State expected, final State followup) {
        Preconditions.checkState(state == expected);
        state = followup;
    }
}
