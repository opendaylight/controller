/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.Status.Failure;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.Lists;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

/**
 * Composite cohort, which coordinates multiple user-provided cohorts as if it was only one cohort.
 * <p/>
 * It tracks current operation and list of cohorts which successfuly finished previous phase in
 * case, if abort is necessary to invoke it only on cohort steps which are still active.
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

    static final Recover<Object> EXCEPTION_TO_MESSAGE = new Recover<>() {
        @Override
        public Failure recover(final Throwable error) {
            return new Failure(error);
        }
    };

    private final DataTreeCohortActorRegistry registry;
    private final TransactionIdentifier txId;
    private final EffectiveModelContext schema;
    private final Executor callbackExecutor;
    private final Timeout timeout;

    private @NonNull List<Success> successfulFromPrevious = List.of();
    private State state = State.IDLE;

    CompositeDataTreeCohort(final DataTreeCohortActorRegistry registry, final TransactionIdentifier transactionID,
        final EffectiveModelContext schema, final Executor callbackExecutor, final Timeout timeout) {
        this.registry = requireNonNull(registry);
        txId = requireNonNull(transactionID);
        this.schema = requireNonNull(schema);
        this.callbackExecutor = requireNonNull(callbackExecutor);
        this.timeout = requireNonNull(timeout);
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
            case ABORTED:
            case COMMITED:
            case FAILED:
            case IDLE:
                break;
            default:
                throw new IllegalStateException("Unhandled state " + state);
        }

        successfulFromPrevious = List.of();
        state = State.IDLE;
    }

    Optional<CompletionStage<Empty>> canCommit(final DataTreeCandidate tip) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: canCommit - candidate: {}", txId, tip);
        } else {
            LOG.debug("{}: canCommit - candidate rootPath: {}", txId, tip.getRootPath());
        }

        final List<CanCommit> messages = registry.createCanCommitMessages(txId, tip, schema);
        LOG.debug("{}: canCommit - messages: {}", txId, messages);
        if (messages.isEmpty()) {
            successfulFromPrevious = List.of();
            changeStateFrom(State.IDLE, State.CAN_COMMIT_SUCCESSFUL);
            return Optional.empty();
        }

        final List<Entry<ActorRef, Future<Object>>> futures = new ArrayList<>(messages.size());
        for (CanCommit message : messages) {
            final ActorRef actor = message.getCohort();
            final Future<Object> future = Patterns.ask(actor, message, timeout).recover(EXCEPTION_TO_MESSAGE,
                ExecutionContexts.global());
            LOG.trace("{}: requesting canCommit from {}", txId, actor);
            futures.add(new SimpleImmutableEntry<>(actor, future));
        }

        changeStateFrom(State.IDLE, State.CAN_COMMIT_SENT);
        return Optional.of(processResponses(futures, State.CAN_COMMIT_SENT, State.CAN_COMMIT_SUCCESSFUL));
    }

    Optional<CompletionStage<Empty>> preCommit() {
        LOG.debug("{}: preCommit - successfulFromPrevious: {}", txId, successfulFromPrevious);

        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SUCCESSFUL);
            return Optional.empty();
        }

        final List<Entry<ActorRef, Future<Object>>> futures = sendMessageToSuccessful(
            new DataTreeCohortActor.PreCommit(txId));
        changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SENT);
        return Optional.of(processResponses(futures, State.PRE_COMMIT_SENT, State.PRE_COMMIT_SUCCESSFUL));
    }

    Optional<CompletionStage<Empty>> commit() {
        LOG.debug("{}: commit - successfulFromPrevious: {}", txId, successfulFromPrevious);
        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMITED);
            return Optional.empty();
        }

        final List<Entry<ActorRef, Future<Object>>> futures = sendMessageToSuccessful(
            new DataTreeCohortActor.Commit(txId));
        changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMIT_SENT);
        return Optional.of(processResponses(futures, State.COMMIT_SENT, State.COMMITED));
    }

    Optional<CompletionStage<?>> abort() {
        LOG.debug("{}: abort - successfulFromPrevious: {}", txId, successfulFromPrevious);

        state = State.ABORTED;
        if (successfulFromPrevious.isEmpty()) {
            return Optional.empty();
        }

        final DataTreeCohortActor.Abort message = new DataTreeCohortActor.Abort(txId);
        final List<Future<Object>> futures = new ArrayList<>(successfulFromPrevious.size());
        for (Success s : successfulFromPrevious) {
            futures.add(Patterns.ask(s.getCohort(), message, timeout));
        }

        return Optional.of(FutureConverters.toJava(Futures.sequence(futures, ExecutionContexts.global())));
    }

    private List<Entry<ActorRef, Future<Object>>> sendMessageToSuccessful(final Object message) {
        LOG.debug("{}: sendMesageToSuccessful: {}", txId, message);

        final List<Entry<ActorRef, Future<Object>>> ret = new ArrayList<>(successfulFromPrevious.size());
        for (Success s : successfulFromPrevious) {
            final ActorRef actor = s.getCohort();
            ret.add(new SimpleImmutableEntry<>(actor, Patterns.ask(actor, message, timeout)));
        }
        return ret;
    }

    private @NonNull CompletionStage<Empty> processResponses(final List<Entry<ActorRef, Future<Object>>> futures,
            final State currentState, final State afterState) {
        LOG.debug("{}: processResponses - currentState: {}, afterState: {}", txId, currentState, afterState);
        final CompletableFuture<Empty> returnFuture = new CompletableFuture<>();
        Future<Iterable<Object>> aggregateFuture = Futures.sequence(Lists.transform(futures, Entry::getValue),
                ExecutionContexts.global());

        aggregateFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(final Throwable failure, final Iterable<Object> results) {
                callbackExecutor.execute(
                    () -> processResponses(failure, results, currentState, afterState, returnFuture));
            }
        }, ExecutionContexts.global());

        return returnFuture;
    }

    private void processResponses(final Throwable failure, final Iterable<Object> results,
            final State currentState, final State afterState, final CompletableFuture<Empty> resultFuture) {
        if (failure != null) {
            successfulFromPrevious = List.of();
            resultFuture.completeExceptionally(failure);
            return;
        }

        final Collection<Failure> failed = new ArrayList<>(1);
        final List<Success> successful = new ArrayList<>();
        for (Object result : results) {
            if (result instanceof DataTreeCohortActor.Success) {
                successful.add((Success) result);
            } else if (result instanceof Status.Failure) {
                failed.add((Failure) result);
            } else {
                LOG.warn("{}: unrecognized response {}, ignoring it", txId, result);
            }
        }

        LOG.debug("{}: processResponses - successful: {}, failed: {}", txId, successful, failed);

        if (!failed.isEmpty()) {
            changeStateFrom(currentState, State.FAILED);
            final Iterator<Failure> it = failed.iterator();
            final Throwable firstEx = it.next().cause();
            while (it.hasNext()) {
                firstEx.addSuppressed(it.next().cause());
            }

            successfulFromPrevious = List.of();
            resultFuture.completeExceptionally(firstEx);
        } else {
            successfulFromPrevious = successful;
            changeStateFrom(currentState, afterState);
            resultFuture.complete(Empty.value());
        }
    }

    void changeStateFrom(final State expected, final State followup) {
        checkState(state == expected);
        state = followup;
    }
}
