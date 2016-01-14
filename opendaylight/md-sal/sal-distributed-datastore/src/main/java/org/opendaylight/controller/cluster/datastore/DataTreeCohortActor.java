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
import akka.japi.Creator;
import com.google.common.base.Preconditions;
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


    static abstract class CommitProtocolCommand<R extends CommitReply> {

        private String txId;

        public String getTxId() {
            return txId;
        }

        protected CommitProtocolCommand(String txId) {
            this.txId = Preconditions.checkNotNull(txId);
        }
    }

    static final class CanCommit extends CommitProtocolCommand<Success> {

        private final DOMDataTreeCandidate candidate;
        private ActorRef cohort;
        private SchemaContext schema;

        public CanCommit(String txId, DOMDataTreeCandidate candidate, SchemaContext schema, ActorRef cohort) {
            super(txId);
            this.cohort = Preconditions.checkNotNull(cohort);
            this.candidate = Preconditions.checkNotNull(candidate);
            this.schema = Preconditions.checkNotNull(schema);
        }

        DOMDataTreeCandidate getCandidate() {
            return candidate;
        }

        public SchemaContext getSchema() {
            return schema;
        }

        ActorRef getCohort() {
            return cohort;
        }

    }

    static abstract class CommitReply {

        private final ActorRef cohortRef;
        private final String txId;

        protected CommitReply(ActorRef cohortRef, String txId) {
            this.cohortRef = Preconditions.checkNotNull(cohortRef);
            this.txId = Preconditions.checkNotNull(txId);
        }

        ActorRef getCohort() {
            return cohortRef;
        }

        public String getTxId() {
            return txId;
        }

    }

    static final class Success extends CommitReply {

        public Success(ActorRef cohortRef, String txId) {
            super(cohortRef, txId);
        }

    }

    static final class PreCommit extends CommitProtocolCommand<Success> {

        public PreCommit(String txId) {
            super(txId);
        }
    }

    static final class Abort extends CommitProtocolCommand<Success> {

        public Abort(String txId) {
            super(txId);
        }
    }

    static final class Commit extends CommitProtocolCommand<Success> {

        public Commit(String txId) {
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
                // FIXME: Provide schema context
                nextStep = cohort.canCommit(message.getTxId(), message.getCandidate(), message.getSchema()).get();
            } catch (Exception e) {
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
        private final String txId;

        CohortStateWithStep(String txId, S step) {
            this.txId = Preconditions.checkNotNull(txId);
            this.step = Preconditions.checkNotNull(step);
        }

        final S getStep() {
            return step;
        }

        final String getTxId() {
            return txId;
        }

        @Override
        final CohortBehaviour<?> abort() {
            try {
                getStep().abort().get();
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            getSender().tell(new Success(getSelf(), txId), getSelf());
            return idleState;
        }

    }

    private class PostCanCommit extends CohortStateWithStep<PreCommit, PostCanCommitStep> {

        PostCanCommit(String txId, PostCanCommitStep nextStep) {
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
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            getSender().tell(new Success(getSelf(), message.getTxId()), getSelf());
            return new PostPreCommit(getTxId(), nextStep);
        }

    }

    private class PostPreCommit extends CohortStateWithStep<Commit, PostPreCommitStep> {

        PostPreCommit(String txId, PostPreCommitStep step) {
            super(txId, step);
        }

        @Override
        CohortBehaviour<?> process(Commit message) {
            try {
                getStep().commit().get();
            } catch (Exception e) {
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

    public static Props props(final DOMDataTreeCommitCohort cohort) {
        return Props.create(new DataTreeChangeListenerCreator(cohort));
    }

    private static final class DataTreeChangeListenerCreator implements Creator<DataTreeCohortActor> {
        private static final long serialVersionUID = 1L;
        private final DOMDataTreeCommitCohort listener;

        DataTreeChangeListenerCreator(final DOMDataTreeCommitCohort cohort) {
            this.listener = Preconditions.checkNotNull(cohort);
        }

        @Override
        public DataTreeCohortActor create() {
            return new DataTreeCohortActor(listener);
        }
    }
}
