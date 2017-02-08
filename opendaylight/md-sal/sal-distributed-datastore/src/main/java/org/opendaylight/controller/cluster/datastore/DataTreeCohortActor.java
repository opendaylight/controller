/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Proxy actor which acts as a facade to the user-provided commit cohort. Responsible for
 * decapsulating DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeCohortActor extends AbstractUntypedActor {
    private final CohortBehaviour<?> idleState = new Idle();
    private final DOMDataTreeCommitCohort cohort;
    private final YangInstanceIdentifier registeredPath;
    private CohortBehaviour<?> currentState = idleState;

    private DataTreeCohortActor(final DOMDataTreeCommitCohort cohort, final YangInstanceIdentifier registeredPath) {
        this.cohort = Preconditions.checkNotNull(cohort);
        this.registeredPath = Preconditions.checkNotNull(registeredPath);
    }

    @Override
    protected void handleReceive(final Object message) {
        LOG.debug("handleReceive for cohort {} - currentState: {}, message: {}", cohort.getClass().getName(),
                currentState, message);

        currentState = currentState.handle(message);
    }


    /**
     * Abstract message base for messages handled by {@link DataTreeCohortActor}.
     *
     * @param <R> Reply message type
     */
    abstract static class CommitProtocolCommand<R extends CommitReply> {

        private final TransactionIdentifier txId;

        final TransactionIdentifier getTxId() {
            return txId;
        }

        protected CommitProtocolCommand(TransactionIdentifier txId) {
            this.txId = Preconditions.checkNotNull(txId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [txId=" + txId + "]";
        }
    }

    static final class CanCommit extends CommitProtocolCommand<Success> {

        private final Collection<DOMDataTreeCandidate> candidates;
        private final ActorRef cohort;
        private final SchemaContext schema;

        CanCommit(TransactionIdentifier txId, Collection<DOMDataTreeCandidate> candidates, SchemaContext schema,
                ActorRef cohort) {
            super(txId);
            this.cohort = Preconditions.checkNotNull(cohort);
            this.candidates = Preconditions.checkNotNull(candidates);
            this.schema = Preconditions.checkNotNull(schema);
        }

        Collection<DOMDataTreeCandidate> getCandidates() {
            return candidates;
        }

        SchemaContext getSchema() {
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

        protected CommitReply(ActorRef cohortRef, TransactionIdentifier txId) {
            this.cohortRef = Preconditions.checkNotNull(cohortRef);
            this.txId = Preconditions.checkNotNull(txId);
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

        Success(ActorRef cohortRef, TransactionIdentifier txId) {
            super(cohortRef, txId);
        }
    }

    static final class PreCommit extends CommitProtocolCommand<Success> {

        PreCommit(TransactionIdentifier txId) {
            super(txId);
        }
    }

    static final class Abort extends CommitProtocolCommand<Success> {

        Abort(TransactionIdentifier txId) {
            super(txId);
        }
    }

    static final class Commit extends CommitProtocolCommand<Success> {

        Commit(TransactionIdentifier txId) {
            super(txId);
        }
    }

    private abstract static class CohortBehaviour<E> {

        abstract Class<E> getHandledMessageType();

        CohortBehaviour<?> handle(Object message) {
            if (getHandledMessageType().isInstance(message)) {
                return process(getHandledMessageType().cast(message));
            } else if (message instanceof Abort) {
                return abort();
            }
            throw new UnsupportedOperationException(String.format("Unexpected message %s in cohort behavior %s",
                    message.getClass(), getClass().getSimpleName()));
        }

        abstract CohortBehaviour<?> abort();

        abstract CohortBehaviour<?> process(E message);

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private class Idle extends CohortBehaviour<CanCommit> {

        @Override
        Class<CanCommit> getHandledMessageType() {
            return CanCommit.class;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        CohortBehaviour<?> process(CanCommit message) {
            final PostCanCommitStep nextStep;
            try {
                nextStep = cohort.canCommit(message.getTxId(), message.getCandidates(), message.getSchema()).get();
            } catch (final Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return this;
            }
            getSender().tell(new Success(getSelf(), message.getTxId()), getSelf());
            return new PostCanCommit(message.getTxId(), nextStep);
        }

        @Override
        CohortBehaviour<?> abort() {
            return this;
        }
    }


    private abstract class CohortStateWithStep<M extends CommitProtocolCommand<?>, S extends ThreePhaseCommitStep>
            extends CohortBehaviour<M> {

        private final S step;
        private final TransactionIdentifier txId;

        CohortStateWithStep(TransactionIdentifier txId, S step) {
            this.txId = Preconditions.checkNotNull(txId);
            this.step = Preconditions.checkNotNull(step);
        }

        final S getStep() {
            return step;
        }

        final TransactionIdentifier getTxId() {
            return txId;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        final CohortBehaviour<?> abort() {
            try {
                getStep().abort().get();
            } catch (final Exception e) {
                LOG.warn("Abort of transaction {} failed for cohort {}", txId, cohort, e);
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            getSender().tell(new Success(getSelf(), txId), getSelf());
            return idleState;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [txId=" + txId + ", step=" + step + "]";
        }
    }

    private class PostCanCommit extends CohortStateWithStep<PreCommit, PostCanCommitStep> {

        PostCanCommit(TransactionIdentifier txId, PostCanCommitStep nextStep) {
            super(txId, nextStep);
        }

        @Override
        Class<PreCommit> getHandledMessageType() {
            return PreCommit.class;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        CohortBehaviour<?> process(PreCommit message) {
            final PostPreCommitStep nextStep;
            try {
                nextStep = getStep().preCommit().get();
            } catch (final Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            getSender().tell(new Success(getSelf(), message.getTxId()), getSelf());
            return new PostPreCommit(getTxId(), nextStep);
        }

    }

    private class PostPreCommit extends CohortStateWithStep<Commit, PostPreCommitStep> {

        PostPreCommit(TransactionIdentifier txId, PostPreCommitStep step) {
            super(txId, step);
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        CohortBehaviour<?> process(Commit message) {
            try {
                getStep().commit().get();
            } catch (final Exception e) {
                getSender().tell(new Status.Failure(e), getSender());
                return idleState;
            }
            getSender().tell(new Success(getSelf(), getTxId()), getSelf());
            return idleState;
        }

        @Override
        Class<Commit> getHandledMessageType() {
            return Commit.class;
        }

    }

    static Props props(final DOMDataTreeCommitCohort cohort, final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeCohortActor.class, cohort, registeredPath);
    }
}
