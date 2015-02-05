package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    @Mock
    private ActorContext actorContext;

    @Mock
    private DatastoreContext datastoreContext;

    @Mock
    private Timer commitTimer;

    @Mock
    private Timer.Context commitTimerContext;

    @Mock
    private Snapshot commitSnapshot;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(getSystem()).when(actorContext).getActorSystem();
        doReturn(datastoreContext).when(actorContext).getDatastoreContext();
        doReturn(100).when(datastoreContext).getShardTransactionCommitTimeoutInSeconds();
        doReturn(commitTimer).when(actorContext).getOperationTimer("commit");
        doReturn(commitTimerContext).when(commitTimer).time();
        doReturn(commitSnapshot).when(commitTimer).getSnapshot();
        doReturn(TimeUnit.SECONDS.toNanos(1) * 1.0).when(commitSnapshot).getMean();
        doReturn(TimeUnit.SECONDS.toNanos(2) * 1.0).when(commitSnapshot).getStdDev();
        doReturn(10.0).when(actorContext).getTxCreationLimit();
    }

    private Future<ActorSelection> newCohort() {
        ActorPath path = getSystem().actorOf(Props.create(DoNothingActor.class)).path();
        ActorSelection actorSelection = getSystem().actorSelection(path);
        return Futures.successful(actorSelection);
    }

    private final ThreePhaseCommitCohortProxy setupProxy(int nCohorts) throws Exception {
        List<Future<ActorSelection>> cohortFutures = Lists.newArrayList();
        for(int i = 1; i <= nCohorts; i++) {
            cohortFutures.add(newCohort());
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohortFutures, "txn-1");
    }

    private ThreePhaseCommitCohortProxy setupProxyWithFailedCohortPath()
            throws Exception {
        List<Future<ActorSelection>> cohortFutures = Lists.newArrayList();
        cohortFutures.add(newCohort());
        cohortFutures.add(Futures.<ActorSelection>failed(new TestException()));

        return new ThreePhaseCommitCohortProxy(actorContext, cohortFutures, "txn-1");
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

        stubber.when(actorContext).executeOperationAsync(any(ActorSelection.class),
                isA(requestType), any(Timeout.class));
    }

    private void verifyCohortInvocations(int nCohorts, Class<?> requestType) {
        verify(actorContext, times(nCohorts)).executeOperationAsync(
                any(ActorSelection.class), isA(requestType), any(Timeout.class));
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
                CanCommitTransactionReply.YES);

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get(5, TimeUnit.SECONDS));

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                CanCommitTransactionReply.NO);

        future = proxy.canCommit();

        assertEquals("canCommit", false, future.get(5, TimeUnit.SECONDS));

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohorts() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                CanCommitTransactionReply.YES, CanCommitTransactionReply.YES);

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get(5, TimeUnit.SECONDS));

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohortsAndOneFailure() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(3);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                CanCommitTransactionReply.YES, CanCommitTransactionReply.NO, CanCommitTransactionReply.YES);

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
        // Precommit is currently a no-op
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

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
                CanCommitTransactionReply.YES, CanCommitTransactionReply.YES);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply(), new PreCommitTransactionReply());

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS,
                new CommitTransactionReply(), new CommitTransactionReply());

        assertEquals(10.0, actorContext.getTxCreationLimit(), 1e-15);

        proxy.canCommit().get(5, TimeUnit.SECONDS);
        proxy.preCommit().get(5, TimeUnit.SECONDS);
        proxy.commit().get(5, TimeUnit.SECONDS);

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
        verifyCohortInvocations(2, CommitTransaction.SERIALIZABLE_CLASS);

        // Verify that the creation limit was changed to 0.5 (based on setup)
        verify(actorContext).setTxCreationLimit(0.5);
    }
}
