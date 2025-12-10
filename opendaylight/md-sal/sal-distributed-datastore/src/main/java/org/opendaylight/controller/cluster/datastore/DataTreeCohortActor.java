/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Proxy actor which acts as a facade to the user-provided commit cohort. Responsible for
 * decapsulating DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeCohortActor extends AbstractUntypedActor {
    private final Idle idleState = new Idle();
    private final DOMDataTreeCommitCohort cohort;
    private final YangInstanceIdentifier registeredPath;
    private final Map<TransactionIdentifier, CohortBehaviour<?, ?>> currentStateMap = new HashMap<>();

    private DataTreeCohortActor(final DOMDataTreeCommitCohort cohort, final YangInstanceIdentifier registeredPath) {
        this.cohort = requireNonNull(cohort);
        this.registeredPath = requireNonNull(registeredPath);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public ActorRef getSender() {
        return super.getSender();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (!(message instanceof CommitProtocolCommand<?> command)) {
            unknownMessage(message);
            return;
        }

        final var currentState = currentStateMap.computeIfAbsent(command.getTxId(), key -> idleState);
        LOG.debug("handleReceive for cohort {} - currentState: {}, message: {}", cohort.getClass().getName(),
            currentState, message);

        currentState.handle(command);
    }

    /**
     * Abstract message base for messages handled by {@link DataTreeCohortActor}.
     *
     * @param <R> Reply message type
     */
    abstract static sealed class CommitProtocolCommand<R extends CommitReply> {
        private final TransactionIdentifier txId;

        CommitProtocolCommand(final TransactionIdentifier txId) {
            this.txId = requireNonNull(txId);
        }

        final TransactionIdentifier getTxId() {
            return txId;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [txId=" + txId + "]";
        }
    }

    static final class CanCommit extends CommitProtocolCommand<Success> {
        private final Collection<DOMDataTreeCandidate> candidates;
        private final ActorRef cohort;
        private final EffectiveModelContext schema;

        CanCommit(final TransactionIdentifier txId, final Collection<DOMDataTreeCandidate> candidates,
                final EffectiveModelContext schema, final ActorRef cohort) {
            super(txId);
            this.cohort = requireNonNull(cohort);
            this.candidates = requireNonNull(candidates);
            this.schema = requireNonNull(schema);
        }

        Collection<DOMDataTreeCandidate> getCandidates() {
            return candidates;
        }

        EffectiveModelContext getSchema() {
            return schema;
        }

        ActorRef getCohort() {
            return cohort;
        }

        @Override
        public String toString() {
            return "CanCommit [txId=" + getTxId() + ", candidates=" + candidates + ", cohort=" + cohort  + "]";
        }
    }

    abstract static class CommitReply {
        private final ActorRef cohortRef;
        private final TransactionIdentifier txId;

        protected CommitReply(final ActorRef cohortRef, final TransactionIdentifier txId) {
            this.cohortRef = requireNonNull(cohortRef);
            this.txId = requireNonNull(txId);
        }

        ActorRef getCohort() {
            return cohortRef;
        }

        final TransactionIdentifier getTxId() {
            return txId;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [txId=" + txId + ", cohortRef=" + cohortRef + "]";
        }
    }

    static final class Success extends CommitReply {
        Success(final ActorRef cohortRef, final TransactionIdentifier txId) {
            super(cohortRef, txId);
        }
    }

    static final class PreCommit extends CommitProtocolCommand<Success> {
        PreCommit(final TransactionIdentifier txId) {
            super(txId);
        }
    }

    static final class Abort extends CommitProtocolCommand<Success> {
        Abort(final TransactionIdentifier txId) {
            super(txId);
        }
    }

    static final class Commit extends CommitProtocolCommand<Success> {
        Commit(final TransactionIdentifier txId) {
            super(txId);
        }
    }

    private abstract class CohortBehaviour<M extends CommitProtocolCommand<?>, S extends ThreePhaseCommitStep> {
        private final Class<M> handledMessageType;

        CohortBehaviour(final Class<M> handledMessageType) {
            this.handledMessageType = requireNonNull(handledMessageType);
        }

        void handle(final CommitProtocolCommand<?> command) {
            if (handledMessageType.isInstance(command)) {
                onMessage(command);
            } else if (command instanceof Abort) {
                onAbort(((Abort)command).getTxId());
            } else {
                getSender().tell(new Status.Failure(new IllegalArgumentException(String.format(
                        "Unexpected message %s in cohort behavior %s", command.getClass(),
                        getClass().getSimpleName()))), self());
            }
        }

        private void onMessage(final CommitProtocolCommand<?> message) {
            final ActorRef sender = getSender();
            TransactionIdentifier txId = message.getTxId();
            ListenableFuture<S> future = process(handledMessageType.cast(message));
            Executor callbackExecutor = future.isDone() ? MoreExecutors.directExecutor()
                    : DataTreeCohortActor.this::executeInSelf;
            Futures.addCallback(future, new FutureCallback<S>() {
                @Override
                public void onSuccess(final S nextStep) {
                    success(txId, sender, nextStep);
                }

                @Override
                public void onFailure(final Throwable failure) {
                    failed(txId, sender, failure);
                }
            }, callbackExecutor);
        }

        private void failed(final TransactionIdentifier txId, final ActorRef sender, final Throwable failure) {
            currentStateMap.remove(txId);
            sender.tell(new Status.Failure(failure), self());
        }

        private void success(final TransactionIdentifier txId, final ActorRef sender, final S nextStep) {
            currentStateMap.computeIfPresent(txId, (key, behaviour) -> nextBehaviour(txId, nextStep));
            sender.tell(new Success(self(), txId), self());
        }

        private void onAbort(final TransactionIdentifier txId) {
            currentStateMap.remove(txId);
            final ActorRef sender = getSender();
            Futures.addCallback(abort(), new FutureCallback<Object>() {
                @Override
                public void onSuccess(final Object noop) {
                    sender.tell(new Success(self(), txId), self());
                }

                @Override
                public void onFailure(final Throwable failure) {
                    LOG.warn("Abort of transaction {} failed for cohort {}", txId, cohort, failure);
                    sender.tell(new Status.Failure(failure), self());
                }
            }, MoreExecutors.directExecutor());
        }

        abstract @Nullable CohortBehaviour<?, ?> nextBehaviour(TransactionIdentifier txId, S nextStep);

        abstract @NonNull ListenableFuture<S> process(M command);

        abstract ListenableFuture<?> abort();

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private class Idle extends CohortBehaviour<CanCommit, PostCanCommitStep> {
        Idle() {
            super(CanCommit.class);
        }

        @Override
        ListenableFuture<PostCanCommitStep> process(final CanCommit message) {
            return cohort.canCommit(message.getTxId(), message.getSchema(), message.getCandidates());
        }

        @Override
        CohortBehaviour<?, ?> nextBehaviour(final TransactionIdentifier txId, final PostCanCommitStep nextStep) {
            return new PostCanCommit(txId, nextStep);
        }

        @Override
        ListenableFuture<?> abort() {
            return ThreePhaseCommitStep.NOOP_ABORT_FUTURE;
        }
    }

    private abstract class CohortStateWithStep<M extends CommitProtocolCommand<?>, S extends ThreePhaseCommitStep,
            N extends ThreePhaseCommitStep> extends CohortBehaviour<M, N> {
        private final S step;
        private final TransactionIdentifier txId;

        CohortStateWithStep(final Class<M> handledMessageType, final TransactionIdentifier txId, final S step) {
            super(handledMessageType);
            this.txId = requireNonNull(txId);
            this.step = requireNonNull(step);
        }

        final S getStep() {
            return step;
        }

        @Override
        ListenableFuture<?> abort() {
            return getStep().abort();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [txId=" + txId + ", step=" + step + "]";
        }
    }

    private class PostCanCommit extends CohortStateWithStep<PreCommit, PostCanCommitStep, PostPreCommitStep> {
        PostCanCommit(final TransactionIdentifier txId, final PostCanCommitStep nextStep) {
            super(PreCommit.class, txId, nextStep);
        }

        @SuppressWarnings("unchecked")
        @Override
        ListenableFuture<PostPreCommitStep> process(final PreCommit message) {
            return (ListenableFuture<PostPreCommitStep>) getStep().preCommit();
        }

        @Override
        CohortBehaviour<?, ?> nextBehaviour(final TransactionIdentifier txId, final PostPreCommitStep nextStep) {
            return new PostPreCommit(txId, nextStep);
        }

    }

    private class PostPreCommit extends CohortStateWithStep<Commit, PostPreCommitStep, NoopThreePhaseCommitStep> {
        PostPreCommit(final TransactionIdentifier txId, final PostPreCommitStep step) {
            super(Commit.class, txId, step);
        }

        @SuppressWarnings("unchecked")
        @Override
        ListenableFuture<NoopThreePhaseCommitStep> process(final Commit message) {
            return (ListenableFuture<NoopThreePhaseCommitStep>) getStep().commit();
        }

        @Override
        CohortBehaviour<?, ?> nextBehaviour(final TransactionIdentifier txId, final NoopThreePhaseCommitStep nextStep) {
            return null;
        }
    }

    private interface NoopThreePhaseCommitStep extends ThreePhaseCommitStep {
    }

    static Props props(final DOMDataTreeCommitCohort cohort, final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeCohortActor.class, cohort, registeredPath);
    }
}
