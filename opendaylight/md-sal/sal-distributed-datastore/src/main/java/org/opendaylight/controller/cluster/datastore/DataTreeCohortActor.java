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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy actor which acts as a facade to the user-provided listener. Responsible for decapsulating
 * DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeCohortActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActor.class);
    private final CohortState<?> idleState = new Idle();
    private final DOMDataTreeCommitCohort cohort;
    private CohortState<?> currentState = idleState;

    private DataTreeCohortActor(final DOMDataTreeCommitCohort cohort) {
        this.cohort = Preconditions.checkNotNull(cohort);
    }

    @Override
    protected void handleReceive(final Object message) {
        currentState = currentState.handle(message);
    }


    static abstract class CommitProtocolCommand<R extends CommitReply> {

    }

    static final class CanCommit extends CommitProtocolCommand<Success> {

        private String txId;

        public String getTxId() {
            return txId;
        }

        public DOMDataTreeCandidate getCandidate() {
            return null;
        }

    }

    static abstract class CommitReply {

        private ActorRef cohortRef;
        private String txId;

        ActorRef getCohort() {
            return cohortRef;
        }

        public String getTxId() {
            return txId;
        }

    }

    static final class Success extends CommitReply {

    }

    static final class PreCommit extends CommitProtocolCommand<Success> {

    }

    static final class Abort extends CommitProtocolCommand<Success> {

    }

    static final class Commit extends CommitProtocolCommand<Success> {

    }

    private static abstract class CohortState<E> {

        abstract Class<E> getForwardMessage();

        CohortState<?> handle(Object message) {
            if (getForwardMessage().isInstance(message)) {
                return process(getForwardMessage().cast(message));
            } else if (message instanceof Abort) {
                return abort();
            }
            throw new UnsupportedOperationException();
        }

        abstract CohortState<?> abort();

        abstract CohortState<?> process(E message);

    }

    private class Idle extends CohortState<CanCommit> {

        @Override
        Class<CanCommit> getForwardMessage() {
            return CanCommit.class;
        }

        @Override
        CohortState<?> process(CanCommit message) {
            final PostCanCommitStep nextStep;
            try {
                nextStep = cohort.canCommit(message.getTxId(), message.getCandidate(), null).get();
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return this;
            }
            return new PostCanCommit(nextStep);
        }

        @Override
        CohortState<?> abort() {
            return this;
        }

    }


    private abstract class CohortStateWithStep<M extends CommitProtocolCommand<?>, S extends ThreePhaseCommitStep>
            extends CohortState<M> {

        private final S step;

        CohortStateWithStep(S step) {
            this.step = step;
        }

        S getStep() {
            return step;
        }

        @Override
        CohortState<?> abort() {
            try {
                getStep().abort().get();
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            getSender().tell(new Success(), getSelf());
            return idleState;
        }

    }

    private class PostCanCommit extends CohortStateWithStep<PreCommit, PostCanCommitStep> {

        PostCanCommit(PostCanCommitStep nextStep) {
            super(nextStep);
        }

        @Override
        Class<PreCommit> getForwardMessage() {
            return PreCommit.class;
        }

        @Override
        CohortState<?> process(PreCommit message) {
            final PostPreCommitStep nextStep;
            try {
                nextStep = getStep().preCommit().get();
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSelf());
                return idleState;
            }
            return new PostPreCommit(nextStep);
        }

    }

    private class PostPreCommit extends CohortStateWithStep<Commit, PostPreCommitStep> {

        PostPreCommit(PostPreCommitStep step) {
            super(step);
        }

        @Override
        CohortState<?> process(Commit message) {
            try {
                getStep().commit().get();
            } catch (Exception e) {
                getSender().tell(new Status.Failure(e), getSender());
                return idleState;
            }
            getSender().tell(new Success(), getSelf());
            return idleState;
        }

        @Override
        Class<Commit> getForwardMessage() {
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
