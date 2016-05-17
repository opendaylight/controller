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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy actor which acts as a facade to the user-provided commit cohort. Responsible for
 * decapsulating DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeCohortActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActor.class);
    private final CohortBehaviour<?> idleState = new Idle();
    private final DOMDataTreeCommitCohort cohort;
    private CohortBehaviour<?> currentState = idleState;

    private DataTreeCohortActor(final DOMDataTreeCommitCohort cohort) {
        this.cohort = Preconditions.checkNotNull(cohort);
    }

    @Override
    protected void handleReceive(final Object message) {
        currentState = currentState.handle(message);
    }


    /**
     * Abstract message base for messages handled by {@link DataTreeCohortActor}.
     *
     * @param <R> Reply message type
     */
    static abstract class CommitProtocolCommand<R extends CommitReply> {

        private final TransactionIdentifier<?> txId;

        final TransactionIdentifier<?> getTxId() {
            return txId;
        }

        protected CommitProtocolCommand(TransactionIdentifier<?> txId) {
            this.txId = Preconditions.checkNotNull(txId);
        }
    }

    static final class CanCommit extends CommitProtocolCommand<Success> {

        private final DOMDataTreeCandidate candidate;
        private final ActorRef cohort;
        private final SchemaContext schema;

        CanCommit(TransactionIdentifier<?> txId, DOMDataTreeCandidate candidate, SchemaContext schema, ActorRef cohort) {
            super(txId);
            this.cohort = Preconditions.checkNotNull(cohort);
            this.candidate = Preconditions.checkNotNull(candidate);
            this.schema = Preconditions.checkNotNull(schema);
        }

        DOMDataTreeCandidate getCandidate() {
            return candidate;
        }

        SchemaContext getSchema() {
            return schema;
        }

        ActorRef getCohort() {
            return cohort;
        }

    }

    static abstract class CommitReply {

        private final ActorRef cohortRef;
        private final TransactionIdentifier<?> txId;

        protected CommitReply(ActorRef cohortRef, TransactionIdentifier<?> txId) {
            this.cohortRef = Preconditions.checkNotNull(cohortRef);
            this.txId = Preconditions.checkNotNull(txId);
        }

        ActorRef getCohort() {
            return cohortRef;
        }

        final TransactionIdentifier<?> getTxId() {
            return txId;
        }
    }

    static final class Success extends CommitReply {

        public Success(ActorRef cohortRef, TransactionIdentifier<?> txId) {
            super(cohortRef, txId);
        }

    }

    static final class PreCommit extends CommitProtocolCommand<Success> {

        public PreCommit(TransactionIdentifier<?> txId) {
            super(txId);
        }
    }

    static final class Abort extends CommitProtocolCommand<Success> {

        public Abort(TransactionIdentifier<?> txId) {
            super(txId);
        }
    }

    static final class Commit extends CommitProtocolCommand<Success> {

        public Commit(TransactionIdentifier<?> txId) {
            super(txId);
        }
    }

    private static abstract class CohortBehaviour<E> {

        abstract Class<E> getHandledMessageType();

        CohortBehaviour<?> handle(Object message) {
            if (getHandledMessageType().isInstance(message)) {
                return process(getHandledMessageType().cast(message));
            } else if (message instanceof Abort) {
                return abort();
            }
            throw new UnsupportedOperationException();
        }

        abstract CohortBehaviour<?> abort();

        abstract CohortBehaviour<?> process(E message);

    }

    private class Idle extends CohortBehaviour<CanCommit> {

        @Override
        Class<CanCommit> getHandledMessageType() {
            return CanCommit.class;
        }

        @Override
        CohortBehaviour<?> process(CanCommit message) {
            final PostCanCommitStep nextStep;
            try {
                nextStep = cohort.canCommit(message.getTxId(), message.getCandidate(), message.getSchema()).get();
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
        private final TransactionIdentifier<?> txId;

        CohortStateWithStep(TransactionIdentifier<?> txId, S step) {
            this.txId = Preconditions.checkNotNull(txId);
            this.step = Preconditions.checkNotNull(step);
        }

        final S getStep() {
            return step;
        }

        final TransactionIdentifier<?> getTxId() {
            return txId;
        }

        @Override
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

    }

    private class PostCanCommit extends CohortStateWithStep<PreCommit, PostCanCommitStep> {

        PostCanCommit(TransactionIdentifier<?> txId, PostCanCommitStep nextStep) {
            super(txId, nextStep);
        }

        @Override
        Class<PreCommit> getHandledMessageType() {
            return PreCommit.class;
        }

        @Override
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

        PostPreCommit(TransactionIdentifier<?> txId, PostPreCommitStep step) {
            super(txId, step);
        }

        @Override
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

    static Props props(final DOMDataTreeCommitCohort cohort) {
        return Props.create(DataTreeCohortActor.class, cohort);
    }
}
