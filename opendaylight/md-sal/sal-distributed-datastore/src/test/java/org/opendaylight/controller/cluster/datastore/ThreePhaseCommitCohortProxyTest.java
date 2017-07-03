/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.dispatch.Futures;
import akka.testkit.TestActorRef;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ThreePhaseCommitCohortProxy.CohortInfo;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.AbstractThreePhaseCommitMessage;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    private ActorContext actorContext;

    @Mock
    private Timer commitTimer;

    @Mock
    private Timer.Context commitTimerContext;

    @Mock
    private Snapshot commitSnapshot;

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private final List<TestActorRef<CohortActor>> cohortActors = new ArrayList<>();
    private final TransactionIdentifier tx = nextTransactionId();


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        actorContext = new ActorContext(getSystem(), actorFactory.createActor(Props.create(DoNothingActor.class)),
                new MockClusterWrapper(), new MockConfiguration(), DatastoreContext.newBuilder().build(),
                new PrimaryShardInfoFutureCache()) {
            @Override
            public Timer getOperationTimer(final String operationName) {
                return commitTimer;
            }

            @Override
            public double getTxCreationLimit() {
                return 10.0;
            }
        };

        doReturn(commitTimerContext).when(commitTimer).time();
        doReturn(commitSnapshot).when(commitTimer).getSnapshot();
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }
    }

    @Test
    public void testCanCommitYesWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION)))), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.no(CURRENT_VERSION)))), tx);

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitYesWithTwoCohorts() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithThreeCohorts() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(
                        CanCommitTransactionReply.no(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder(tx)));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithExceptionFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(new TestException()))), tx);

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCanCommitWithInvalidResponseType() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectCanCommit("invalid"))), tx);

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithFailedCohortFuture() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx)),
                newCohortInfoWithFailedFuture(new TestException()),
                newCohortInfo(new CohortActor.Builder(tx)));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test
    public void testAllThreePhasesSuccessful() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(
                        new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
                newCohortInfo(
                        new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    @Test(expected = TestException.class)
    public void testCommitWithExceptionFailure() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(
                        new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
                newCohortInfo(
                        new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                                .expectCommit(new TestException())));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        propagateExecutionExceptionCause(proxy.commit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommitWithInvalidResponseType() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext,
                Arrays.asList(newCohortInfo(new CohortActor.Builder(tx)
                        .expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).expectCommit("invalid"))), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        propagateExecutionExceptionCause(proxy.commit());
    }

    @Test
    public void testAbort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectAbort(
                        AbortTransactionReply.instance(CURRENT_VERSION)))), tx);

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder(tx).expectAbort(new RuntimeException("mock")))), tx);

        // The exception should not get propagated.
        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailedCohortFuture() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfoWithFailedFuture(new TestException()), newCohortInfo(new CohortActor.Builder(tx)));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, tx);

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testWithNoCohorts() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext,
                Collections.<CohortInfo>emptyList(), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    private void propagateExecutionExceptionCause(final ListenableFuture<?> future) throws Exception {

        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            verifyCohortActors();
            Throwables.propagateIfPossible(e.getCause(), Exception.class);
            throw new RuntimeException(e.getCause());
        }
    }

    private CohortInfo newCohortInfo(final CohortActor.Builder builder, final short version) {
        TestActorRef<CohortActor> actor = actorFactory.createTestActor(builder.props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId("cohort"));
        cohortActors.add(actor);
        return new CohortInfo(Futures.successful(getSystem().actorSelection(actor.path())), () -> version);
    }

    private CohortInfo newCohortInfo(final CohortActor.Builder builder) {
        return newCohortInfo(builder, CURRENT_VERSION);
    }

    private static CohortInfo newCohortInfoWithFailedFuture(final Exception failure) {
        return new CohortInfo(Futures.<ActorSelection>failed(failure), () -> CURRENT_VERSION);
    }

    private void verifyCohortActors() {
        for (TestActorRef<CohortActor> actor: cohortActors) {
            actor.underlyingActor().verify();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private <T> T verifySuccessfulFuture(final ListenableFuture<T> future) throws Exception {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            verifyCohortActors();
            throw e;
        }
    }

    private void verifyCanCommit(final ListenableFuture<Boolean> future, final boolean expected) throws Exception {
        Boolean actual = verifySuccessfulFuture(future);
        assertEquals("canCommit", expected, actual);
    }

    private static class CohortActor extends UntypedActor {
        private final Builder builder;
        private final AtomicInteger canCommitCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger abortCount = new AtomicInteger();
        private volatile AssertionError assertionError;

        private CohortActor(final Builder builder) {
            this.builder = builder;
        }

        @Override
        public void onReceive(final Object message) {
            if (CanCommitTransaction.isSerializedType(message)) {
                canCommitCount.incrementAndGet();
                onMessage("CanCommitTransaction", message, CanCommitTransaction.fromSerializable(message),
                        builder.expCanCommitType, builder.canCommitReply);
            } else if (CommitTransaction.isSerializedType(message)) {
                commitCount.incrementAndGet();
                onMessage("CommitTransaction", message, CommitTransaction.fromSerializable(message),
                        builder.expCommitType, builder.commitReply);
            } else if (AbortTransaction.isSerializedType(message)) {
                abortCount.incrementAndGet();
                onMessage("AbortTransaction", message, AbortTransaction.fromSerializable(message),
                        builder.expAbortType, builder.abortReply);
            } else {
                assertionError = new AssertionError("Unexpected message " + message);
            }
        }

        private void onMessage(final String name, final Object rawMessage,
                final AbstractThreePhaseCommitMessage actualMessage, final Class<?> expType, final Object reply) {
            try {
                assertNotNull("Unexpected " + name, expType);
                assertEquals(name + " type", expType, rawMessage.getClass());
                assertEquals(name + " transactionId", builder.transactionId, actualMessage.getTransactionId());

                if (reply instanceof Throwable) {
                    getSender().tell(new akka.actor.Status.Failure((Throwable)reply), self());
                } else {
                    getSender().tell(reply, self());
                }
            } catch (AssertionError e) {
                assertionError = e;
            }
        }

        void verify() {
            if (assertionError != null) {
                throw assertionError;
            }

            if (builder.expCanCommitType != null) {
                assertEquals("CanCommitTransaction count", 1, canCommitCount.get());
            }

            if (builder.expCommitType != null) {
                assertEquals("CommitTransaction count", 1, commitCount.get());
            }

            if (builder.expAbortType != null) {
                assertEquals("AbortTransaction count", 1, abortCount.get());
            }
        }

        static class Builder {
            private Class<?> expCanCommitType;
            private Class<?> expCommitType;
            private Class<?> expAbortType;
            private Object canCommitReply;
            private Object commitReply;
            private Object abortReply;
            private final TransactionIdentifier transactionId;

            Builder(final TransactionIdentifier transactionId) {
                this.transactionId = Preconditions.checkNotNull(transactionId);
            }

            Builder expectCanCommit(final Class<?> newExpCanCommitType, final Object newCanCommitReply) {
                this.expCanCommitType = newExpCanCommitType;
                this.canCommitReply = newCanCommitReply;
                return this;
            }

            Builder expectCanCommit(final Object newCanCommitReply) {
                return expectCanCommit(CanCommitTransaction.class, newCanCommitReply);
            }

            Builder expectCommit(final Class<?> newExpCommitType, final Object newCommitReply) {
                this.expCommitType = newExpCommitType;
                this.commitReply = newCommitReply;
                return this;
            }

            Builder expectCommit(final Object newCommitReply) {
                return expectCommit(CommitTransaction.class, newCommitReply);
            }

            Builder expectAbort(final Class<?> newExpAbortType, final Object newAbortReply) {
                this.expAbortType = newExpAbortType;
                this.abortReply = newAbortReply;
                return this;
            }

            Builder expectAbort(final Object newAbortReply) {
                return expectAbort(AbortTransaction.class, newAbortReply);
            }

            Props props() {
                return Props.create(CohortActor.class, this);
            }
        }
    }
}
