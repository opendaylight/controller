package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.dispatch.Futures;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.SerializableMessage;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;

import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    @Mock
    private ActorContext actorContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(getSystem()).when(actorContext).getActorSystem();
    }

    private Future<ActorPath> newCohortPath() {
        ActorPath path = getSystem().actorOf(Props.create(DoNothingActor.class)).path();
        doReturn(mock(ActorSelection.class)).when(actorContext).actorSelection(path);
        return Futures.successful(path);
    }

    private final ThreePhaseCommitCohortProxy setupProxy(int nCohorts) throws Exception {
        List<Future<ActorPath>> cohortPathFutures = Lists.newArrayList();
        for(int i = 1; i <= nCohorts; i++) {
            cohortPathFutures.add(newCohortPath());
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohortPathFutures, "txn-1");
    }

    private ThreePhaseCommitCohortProxy setupProxyWithFailedCohortPath()
            throws Exception {
        List<Future<ActorPath>> cohortPathFutures = Lists.newArrayList();
        cohortPathFutures.add(newCohortPath());
        cohortPathFutures.add(Futures.<ActorPath>failed(new TestException()));

        return new ThreePhaseCommitCohortProxy(actorContext, cohortPathFutures, "txn-1");
    }

    private void setupMockActorContext(Class<?> requestType, Object... responses) {
        Stubber stubber = doReturn(responses[0] instanceof Throwable ? Futures
                .failed((Throwable) responses[0]) : Futures
                .successful(((SerializableMessage) responses[0]).toSerializable()));

        for(int i = 1; i < responses.length; i++) {
            stubber = stubber.doReturn(responses[i] instanceof Throwable ? Futures
                    .failed((Throwable) responses[i]) : Futures
                    .successful(((SerializableMessage) responses[i]).toSerializable()));
        }

        stubber.when(actorContext).executeRemoteOperationAsync(any(ActorSelection.class),
                isA(requestType), any(FiniteDuration.class));
    }

    private void verifyCohortInvocations(int nCohorts, Class<?> requestType) {
        verify(actorContext, times(nCohorts)).executeRemoteOperationAsync(
                any(ActorSelection.class), isA(requestType), any(FiniteDuration.class));
    }

    private void propagateExecutionExceptionCause(ListenableFuture<?> future) throws Throwable {

        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch(ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testCanCommitWithOneCohort() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get(5, TimeUnit.SECONDS));

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(false));

        future = proxy.canCommit();

        assertEquals("canCommit", false, future.get(5, TimeUnit.SECONDS));

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohorts() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true), new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get(5, TimeUnit.SECONDS));

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohortsAndOneFailure() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(3);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true), new CanCommitTransactionReply(false),
                new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", false, future.get(5, TimeUnit.SECONDS));

        verifyCohortInvocations(3, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithExceptionFailure() throws Throwable {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS, new TestException());

        propagateExecutionExceptionCause(proxy.canCommit());
    }

    @Test(expected = ExecutionException.class)
    public void testCanCommitWithInvalidResponseType() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

        proxy.canCommit().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testCanCommitWithFailedCohortPath() throws Throwable {

        ThreePhaseCommitCohortProxy proxy = setupProxyWithFailedCohortPath();

        try {
            propagateExecutionExceptionCause(proxy.canCommit());
        } finally {
            verifyCohortInvocations(0, CanCommitTransaction.SERIALIZABLE_CLASS);
        }
    }

    @Test
    public void testPreCommit() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

        proxy.preCommit().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(1, PreCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected = ExecutionException.class)
    public void testPreCommitWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply(), new RuntimeException("mock"));

        proxy.preCommit().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testAbort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(AbortTransaction.SERIALIZABLE_CLASS, new AbortTransactionReply());

        proxy.abort().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(1, AbortTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testAbortWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(AbortTransaction.SERIALIZABLE_CLASS, new RuntimeException("mock"));

        // The exception should not get propagated.
        proxy.abort().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(1, AbortTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testAbortWithFailedCohortPath() throws Throwable {

        ThreePhaseCommitCohortProxy proxy = setupProxyWithFailedCohortPath();

        // The exception should not get propagated.
        proxy.abort().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(0, AbortTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCommit() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS, new CommitTransactionReply(),
                new CommitTransactionReply());

        proxy.commit().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(2, CommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected = TestException.class)
    public void testCommitWithFailure() throws Throwable {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS, new CommitTransactionReply(),
                new TestException());

        propagateExecutionExceptionCause(proxy.commit());
    }

    @Test(expected = ExecutionException.class)
    public void testCommitWithInvalidResponseType() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS, new PreCommitTransactionReply());

        proxy.commit().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testCommitWithFailedCohortPath() throws Throwable {

        ThreePhaseCommitCohortProxy proxy = setupProxyWithFailedCohortPath();

        try {
            propagateExecutionExceptionCause(proxy.commit());
        } finally {
            verifyCohortInvocations(0, CommitTransaction.SERIALIZABLE_CLASS);
        }
    }

    @Test
    public void testAllThreePhasesSuccessful() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true), new CanCommitTransactionReply(true));

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply(), new PreCommitTransactionReply());

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS,
                new CommitTransactionReply(), new CommitTransactionReply());

        proxy.canCommit().get(5, TimeUnit.SECONDS);
        proxy.preCommit().get(5, TimeUnit.SECONDS);
        proxy.commit().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
        verifyCohortInvocations(2, PreCommitTransaction.SERIALIZABLE_CLASS);
        verifyCohortInvocations(2, CommitTransaction.SERIALIZABLE_CLASS);
    }
}
