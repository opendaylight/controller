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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.Status;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.dispatch.CompletionStages;
import org.apache.pekko.pattern.Patterns;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite cohort, which coordinates multiple user-provided cohorts as if it was only one cohort.
 * <p/>
 * It tracks current operation and list of cohorts which successfuly finished previous phase in
 * case, if abort is necessary to invoke it only on cohort steps which are still active.
 */
class UserCohorts {
    private static final Logger LOG = LoggerFactory.getLogger(UserCohorts.class);

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
        COMMITTED,
        /**
         * Some of cohorts responded back with unsuccessful message.
         */
        FAILED,
        /**
         * Abort message was send to all cohorts which responded with success previously.
         */
        ABORTED
    }

    private final @NonNull DataTreeCohortActorRegistry registry;
    private final @NonNull EffectiveModelContext modelContext;
    private final @NonNull TransactionIdentifier txId;
    private final @NonNull Duration stepTimeout;
    private final @NonNull Shard shard;

    private @NonNull List<Success> successfulFromPrevious = List.of();
    private State state = State.IDLE;

    UserCohorts(final DataTreeCohortActorRegistry registry, final Shard shard, final EffectiveModelContext modelContext,
            final TransactionIdentifier txId, final Duration stepTimeout) {
        this.registry = requireNonNull(registry);
        this.shard = requireNonNull(shard);
        this.modelContext = requireNonNull(modelContext);
        this.txId = requireNonNull(txId);
        this.stepTimeout = requireNonNull(stepTimeout);
    }

    void reset() {
        switch (state) {
            case null -> throw new NullPointerException();
            case CAN_COMMIT_SENT, CAN_COMMIT_SUCCESSFUL, PRE_COMMIT_SENT, PRE_COMMIT_SUCCESSFUL, COMMIT_SENT ->
                abort();
            case ABORTED, COMMITTED, FAILED, IDLE -> {
                // No-op
            }
        }

        successfulFromPrevious = List.of();
        state = State.IDLE;
    }

    @Nullable CompletionStage<Empty> canCommit(final @NonNull DataTreeCandidate tip) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: canCommit - candidate: {}", txId, tip);
        } else {
            LOG.debug("{}: canCommit - candidate rootPath: {}", txId, tip.getRootPath());
        }

        final var messages = registry.createCanCommitMessages(txId, tip, modelContext);
        LOG.debug("{}: canCommit - messages: {}", txId, messages);
        if (messages.isEmpty()) {
            successfulFromPrevious = List.of();
            changeStateFrom(State.IDLE, State.CAN_COMMIT_SUCCESSFUL);
            return null;
        }

        final var futures = messages.stream()
            .map(message -> {
                final var actor = message.getCohort();
                LOG.trace("{}: requesting canCommit from {}", txId, actor);
                return Patterns.ask(actor, message, stepTimeout).exceptionally(Failure::new);
            })
            .toList();
        changeStateFrom(State.IDLE, State.CAN_COMMIT_SENT);
        return processResponses(futures, State.CAN_COMMIT_SENT, State.CAN_COMMIT_SUCCESSFUL);
    }

    @Nullable CompletionStage<Empty> preCommit() {
        LOG.debug("{}: preCommit - successfulFromPrevious: {}", txId, successfulFromPrevious);

        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SUCCESSFUL);
            return null;
        }

        final var futures = sendMessageToSuccessful(new DataTreeCohortActor.PreCommit(txId));
        changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SENT);
        return processResponses(futures, State.PRE_COMMIT_SENT, State.PRE_COMMIT_SUCCESSFUL);
    }

    @Nullable CompletionStage<Empty> commit() {
        LOG.debug("{}: commit - successfulFromPrevious: {}", txId, successfulFromPrevious);
        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMITTED);
            return null;
        }

        final var futures = sendMessageToSuccessful(new DataTreeCohortActor.Commit(txId));
        changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMIT_SENT);
        return processResponses(futures, State.COMMIT_SENT, State.COMMITTED);
    }

    @Nullable CompletionStage<Empty> abort() {
        LOG.debug("{}: abort - successfulFromPrevious: {}", txId, successfulFromPrevious);

        state = State.ABORTED;
        if (successfulFromPrevious.isEmpty()) {
            return null;
        }

        final var message = new DataTreeCohortActor.Abort(txId);
        return CompletionStages.sequence(successfulFromPrevious.stream()
            .map(success -> Patterns.ask(success.getCohort(), message, stepTimeout))
            // FIXME: do not use executeInSelf()
            .toList(), shard::executeInSelf)
            .thenApply(ignored -> Empty.value());
    }

    private List<CompletionStage<Object>> sendMessageToSuccessful(final Object message) {
        LOG.debug("{}: sendMesageToSuccessful: {}", txId, message);
        return successfulFromPrevious.stream()
            .map(success -> Patterns.ask(success.getCohort(), message, stepTimeout))
            .toList();
    }

    private @NonNull CompletionStage<Empty> processResponses(final List<CompletionStage<Object>> futures,
            final State currentState, final State afterState) {
        LOG.debug("{}: processResponses - currentState: {}, afterState: {}", txId, currentState, afterState);
        final var returnFuture = new CompletableFuture<Empty>();

        CompletionStages.sequence(futures, null)
            .whenComplete((results, failure) ->
                // FIXME: do not use executeInSelf()
                shard.executeInSelf(() -> processResponses(failure, results, currentState, afterState, returnFuture)));
        return returnFuture;
    }

    private void processResponses(final Throwable failure, final Iterable<Object> results,
            final State currentState, final State afterState, final CompletableFuture<Empty> resultFuture) {
        if (failure != null) {
            successfulFromPrevious = List.of();
            resultFuture.completeExceptionally(failure);
            return;
        }

        final var failed = new ArrayList<Status.Failure>(1);
        final var successful = new ArrayList<DataTreeCohortActor.Success>();
        for (var result : results) {
            switch (result) {
                case DataTreeCohortActor.Success success -> successful.add(success);
                case Status.Failure fail -> failed.add(fail);
                default ->
                    LOG.warn("{}: unrecognized response {}, ignoring it", txId, result);
            }
        }

        LOG.debug("{}: processResponses - successful: {}, failed: {}", txId, successful, failed);

        if (failed.isEmpty()) {
            successfulFromPrevious = successful;
            changeStateFrom(currentState, afterState);
            resultFuture.complete(Empty.value());
            return;
        }

        changeStateFrom(currentState, State.FAILED);
        final var it = failed.iterator();
        final var firstEx = it.next().cause();
        while (it.hasNext()) {
            firstEx.addSuppressed(it.next().cause());
        }

        successfulFromPrevious = List.of();
        resultFuture.completeExceptionally(firstEx);
    }

    void changeStateFrom(final State expected, final State followup) {
        checkState(state == expected);
        state = followup;
    }
}
