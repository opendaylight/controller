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
import com.google.common.base.Supplier;
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
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        actorContext = new ActorContext(getSystem(), actorFactory.createActor(Props.create(DoNothingActor.class)),
                new MockClusterWrapper(), new MockConfiguration(),
                DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache()) {
                    @Override
                    public Timer getOperationTimer(String operationName) {
                        return commitTimer;
                    }

                    @Override
                    public double getTxCreationLimit() {
                        return 10.0;
                    }
                };

        doReturn(commitTimerContext).when(commitTimer).time();
        doReturn(commitSnapshot).when(commitTimer).getSnapshot();
        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }
    }

    @Test
    public void testCanCommitYesWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION)))), "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithOneCohort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.no(CURRENT_VERSION)))), "txn-1");

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitYesWithTwoCohorts() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifyCohortActors();
    }

    @Test
    public void testCanCommitNoWithThreeCohorts() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.yes(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(
                        CanCommitTransactionReply.no(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder("txn-1")));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifyCanCommit(proxy.canCommit(), false);
        verifyCohortActors();
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithExceptionFailure() throws Throwable {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit(new TestException()))), "txn-1");

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCanCommitWithInvalidResponseType() throws Throwable {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectCanCommit("invalid"))), "txn-1");

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithFailedCohortFuture() throws Throwable {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1")),
                newCohortInfoWithFailedFuture(new TestException()),
                newCohortInfo(new CohortActor.Builder("txn-1")));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test
    public void testAllThreePhasesSuccessful() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).
                        expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).
                        expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    @Test(expected = TestException.class)
    public void testCommitWithExceptionFailure() throws Throwable {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).
                        expectCommit(CommitTransactionReply.instance(CURRENT_VERSION))),
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).
                        expectCommit(new TestException())));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        propagateExecutionExceptionCause(proxy.commit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommitWithInvalidResponseType() throws Throwable {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(CanCommitTransactionReply.yes(CURRENT_VERSION)).
                        expectCommit("invalid"))), "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        propagateExecutionExceptionCause(proxy.commit());
    }

    @Test
    public void testAbort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectAbort(
                        AbortTransactionReply.instance(CURRENT_VERSION)))), "txn-1");

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").expectAbort(new RuntimeException("mock")))), "txn-1");

        // The exception should not get propagated.
        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testAbortWithFailedCohortFuture() throws Throwable {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfoWithFailedFuture(new TestException()),
                newCohortInfo(new CohortActor.Builder("txn-1")));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifySuccessfulFuture(proxy.abort());
        verifyCohortActors();
    }

    @Test
    public void testWithNoCohorts() throws Exception {
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext,
                Collections.<CohortInfo>emptyList(), "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    @Test
    public void testBackwardsCompatibilityWithPreBoron() throws Exception {
        List<CohortInfo> cohorts = Arrays.asList(
                newCohortInfo(new CohortActor.Builder("txn-1").
                        expectCanCommit(ThreePhaseCommitCohortMessages.CanCommitTransaction.class,
                                CanCommitTransactionReply.yes(DataStoreVersions.LITHIUM_VERSION)).
                        expectCommit(ThreePhaseCommitCohortMessages.CommitTransaction.class,
                                CommitTransactionReply.instance(DataStoreVersions.LITHIUM_VERSION)),
                        DataStoreVersions.LITHIUM_VERSION));
        ThreePhaseCommitCohortProxy proxy = new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");

        verifyCanCommit(proxy.canCommit(), true);
        verifySuccessfulFuture(proxy.preCommit());
        verifySuccessfulFuture(proxy.commit());
        verifyCohortActors();
    }

    private void propagateExecutionExceptionCause(ListenableFuture<?> future) throws Throwable {

        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch(ExecutionException e) {
            verifyCohortActors();
            throw e.getCause();
        }
    }

    private CohortInfo newCohortInfo(CohortActor.Builder builder, final short version) {
        TestActorRef<CohortActor> actor = actorFactory.createTestActor(builder.props().
                withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId("cohort"));
        cohortActors.add(actor);
        return new CohortInfo(Futures.successful(getSystem().actorSelection(actor.path())), new Supplier<Short>() {
            @Override
            public Short get() {
                return version;
            }
        });
    }

    private static CohortInfo newCohortInfoWithFailedFuture(Exception failure) {
        return new CohortInfo(Futures.<ActorSelection>failed(failure), new Supplier<Short>() {
            @Override
            public Short get() {
                return CURRENT_VERSION;
            }
        });
    }

    private CohortInfo newCohortInfo(CohortActor.Builder builder) {
        return newCohortInfo(builder, CURRENT_VERSION);
    }

    private void verifyCohortActors() {
        for(TestActorRef<CohortActor> actor: cohortActors) {
            actor.underlyingActor().verify();
        }
    }

    private <T> T verifySuccessfulFuture(ListenableFuture<T> future) throws Exception {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch(Exception e) {
            verifyCohortActors();
            throw e;
        }
    }

    private void verifyCanCommit(ListenableFuture<Boolean> future, boolean expected) throws Exception {
        Boolean actual = verifySuccessfulFuture(future);
        assertEquals("canCommit", expected, actual);
    }

    private static class CohortActor extends UntypedActor {
        private final Builder builder;
        private final AtomicInteger canCommitCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger abortCount = new AtomicInteger();
        private volatile AssertionError assertionError;

        private CohortActor(Builder builder) {
            this.builder = builder;
        }

        @Override
        public void onReceive(Object message) {
            if(CanCommitTransaction.isSerializedType(message)) {
                canCommitCount.incrementAndGet();
                onMessage("CanCommitTransaction", message, CanCommitTransaction.fromSerializable(message),
                        builder.expCanCommitType, builder.canCommitReply);
            } else if(CommitTransaction.isSerializedType(message)) {
                commitCount.incrementAndGet();
                onMessage("CommitTransaction", message, CommitTransaction.fromSerializable(message),
                        builder.expCommitType, builder.commitReply);
            } else if(AbortTransaction.isSerializedType(message)) {
                abortCount.incrementAndGet();
                onMessage("AbortTransaction", message, AbortTransaction.fromSerializable(message),
                        builder.expAbortType, builder.abortReply);
            } else {
                assertionError = new AssertionError("Unexpected message " + message);
            }
        }

        private void onMessage(String name, Object rawMessage, AbstractThreePhaseCommitMessage actualMessage,
                Class<?> expType, Object reply) {
            try {
                assertNotNull("Unexpected " + name, expType);
                assertEquals(name + " type", expType, rawMessage.getClass());
                assertEquals(name + " transactionId", builder.transactionId, actualMessage.getTransactionID());

                if(reply instanceof Throwable) {
                    getSender().tell(new akka.actor.Status.Failure((Throwable)reply), self());
                } else {
                    getSender().tell(reply, self());
                }
            } catch(AssertionError e) {
                assertionError = e;
            }
        }

        void verify() {
            if(assertionError != null) {
                throw assertionError;
            }

            if(builder.expCanCommitType != null) {
                assertEquals("CanCommitTransaction count", 1, canCommitCount.get());
            }

            if(builder.expCommitType != null) {
                assertEquals("CommitTransaction count", 1, commitCount.get());
            }

            if(builder.expAbortType != null) {
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
            private final String transactionId;

            Builder(String transactionId) {
                this.transactionId = transactionId;
            }

            Builder expectCanCommit(Class<?> expCanCommitType, Object canCommitReply) {
                this.expCanCommitType = expCanCommitType;
                this.canCommitReply = canCommitReply;
                return this;
            }

            Builder expectCanCommit(Object canCommitReply) {
                return expectCanCommit(CanCommitTransaction.class, canCommitReply);
            }

            Builder expectCommit(Class<?> expCommitType, Object commitReply) {
                this.expCommitType = expCommitType;
                this.commitReply = commitReply;
                return this;
            }

            Builder expectCommit(Object commitReply) {
                return expectCommit(CommitTransaction.class, commitReply);
            }

            Builder expectAbort(Class<?> expAbortType, Object abortReply) {
                this.expAbortType = expAbortType;
                this.abortReply = abortReply;
                return this;
            }

            Builder expectAbort(Object abortReply) {
                return expectAbort(AbortTransaction.class, abortReply);
            }

            Props props() {
                return Props.create(CohortActor.class, this);
            }
        }
    }
}
