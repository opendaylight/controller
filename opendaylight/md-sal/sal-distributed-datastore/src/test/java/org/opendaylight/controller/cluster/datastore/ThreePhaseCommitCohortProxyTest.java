/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.dispatch.Dispatchers;
import akka.dispatch.Futures;
import akka.testkit.TestActorRef;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ThreePhaseCommitCohortProxy.CohortInfo;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.AbstractThreePhaseCommitMessage;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {
    static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

    }

    private ActorUtils actorUtils;

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
        actorUtils = new ActorUtils(getSystem(), actorFactory.createActor(Props.create(DoNothingActor.class)),
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

        lenient().doReturn(commitTimerContext).when(commitTimer).time();
        lenient().doReturn(commitSnapshot).when(commitTimer).getSnapshot();
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            lenient().doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }
    }

    @Test
    public void testCanCommitYesWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)))),
            tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.no(CURRENT_VERSION)))),
            tx);

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitYesWithTwoCohorts() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))),
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)))),
            tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithThreeCohorts() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))),
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.no(CURRENT_VERSION))),
            newCohortInfo(new CohortActor.Builder(tx))), tx);

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitWithExceptionFailure() {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils,
            List.of(newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(new TestException()))), tx);

        propagateExecutionExceptionCause(proxy.canCommit(), TestException.class);
    }

    @Test
    public void testCanCommitWithInvalidResponseType() {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils,
            List.of(newCohortInfo(new CohortActor.Builder(tx).expectCanCommit("invalid"))), tx);

        assertEquals("Unexpected response type class java.lang.String",
            propagateExecutionExceptionCause(proxy.canCommit(), IllegalArgumentException.class));
    }

    @Test
    public void testCanCommitWithFailedCohortFuture() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx)),
            newCohortInfoWithFailedFuture(new TestException()),
            newCohortInfo(new CohortActor.Builder(tx))), tx);

        propagateExecutionExceptionCause(proxy.canCommit(), TestException.class);
    }

    @Test
    public void testAllThreePhasesSuccessful() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION)))), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    @Test
    public void testCommitWithExceptionFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                .expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
            .expectCommit(new TestException()))), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        propagateExecutionExceptionCause(proxy.commit(), TestException.class);
    }

    @Test
    public void testCommitWithInvalidResponseType() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils,List.of(
            newCohortInfo(new CohortActor.Builder(tx).expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION))
                .expectCommit("invalid"))),
            tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        assertEquals("Unexpected response type class java.lang.String",
            propagateExecutionExceptionCause(proxy.commit(), IllegalArgumentException.class));
    }

    @Test
    public void testAbort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils,
            List.of(newCohortInfo(new CohortActor.Builder(tx).expectAbort(
                AbortTransactionReply.instance(CURRENT_VERSION)))),
            tx);

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils,
            List.of(newCohortInfo(new CohortActor.Builder(tx).expectAbort(new RuntimeException("mock")))), tx);

        // The exception should not get propagated.
        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailedCohortFuture() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(
            newCohortInfoWithFailedFuture(new TestException()), newCohortInfo(new CohortActor.Builder(tx))), tx);

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testWithNoCohorts() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorUtils, List.of(), tx);

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    private String propagateExecutionExceptionCause(final ListenableFuture<?> future,
            final Class<? extends Exception> expected) {
        final var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS)).getCause();
        verifyCohortActors();
        assertThat(ex, instanceOf(expected));
        return ex.getMessage();
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

    private static class CohortActor extends UntypedAbstractActor {
        private final Builder builder;
        private final AtomicInteger canCommitCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger abortCount = new AtomicInteger();
        private volatile AssertionError assertionError;

        CohortActor(final Builder builder) {
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
                this.transactionId = requireNonNull(transactionId);
            }

            Builder expectCanCommit(final Class<?> newExpCanCommitType, final Object newCanCommitReply) {
                expCanCommitType = newExpCanCommitType;
                canCommitReply = newCanCommitReply;
                return this;
            }

            Builder expectCanCommit(final Object newCanCommitReply) {
                return expectCanCommit(CanCommitTransaction.class, newCanCommitReply);
            }

            Builder expectCommit(final Class<?> newExpCommitType, final Object newCommitReply) {
                expCommitType = newExpCommitType;
                commitReply = newCommitReply;
                return this;
            }

            Builder expectCommit(final Object newCommitReply) {
                return expectCommit(CommitTransaction.class, newCommitReply);
            }

            Builder expectAbort(final Class<?> newExpAbortType, final Object newAbortReply) {
                expAbortType = newExpAbortType;
                abortReply = newAbortReply;
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
