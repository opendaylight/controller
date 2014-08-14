package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.dispatch.Futures;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {

    @Mock
    private ActorContext actorContext;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        doReturn(getSystem()).when(actorContext).getActorSystem();
    }

    private ThreePhaseCommitCohortProxy setupProxy(int nCohorts) {
        List<ActorPath> cohorts = Lists.newArrayList();
        for(int i = 1; i <= nCohorts; i++) {
            ActorPath path = getSystem().actorOf(Props.create(MessageCollectorActor.class)).path();
            cohorts.add(path);
            doReturn(mock(ActorSelection.class)).when(actorContext).actorSelection(path);
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohorts, "txn-1");
    }

    private void setupMockActorContext(Class<?> requestType, Object... responses) {
        Stubber stubber = doReturn(responses[0] instanceof Throwable ?
                Futures.failed((Throwable) responses[0]) :
                Futures.successful(((SerializableMessage)responses[0]).toSerializable()));

        for(int i = 1; i < responses.length; i++) {
            stubber = stubber.doReturn(responses[i] instanceof Throwable ?
                    Futures.failed((Throwable) responses[i]) :
                    Futures.successful(((SerializableMessage)responses[i]).toSerializable()));
        }

        stubber.when(actorContext).executeRemoteOperationAsync(any(ActorSelection.class),
                isA(requestType), any(FiniteDuration.class));
    }

    private void verifyCohortInvocations(int nCohorts, Class<?> requestType) {
        verify(actorContext, times(nCohorts)).executeRemoteOperationAsync(any(ActorSelection.class),
                isA(requestType), any(FiniteDuration.class));
    }

    @Test
    public void testCanCommitWithOneCohort() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get());

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(false));

        future = proxy.canCommit();

        assertEquals("canCommit", false, future.get());

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohorts() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true), new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", true, future.get());

        verifyCohortInvocations(2, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCanCommitWithMultipleCohortsAndOneFailure() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(3);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new CanCommitTransactionReply(true), new CanCommitTransactionReply(false),
                new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        assertEquals("canCommit", false, future.get());

        verifyCohortInvocations(3, CanCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected=ExecutionException.class)
    public void testCanCommitWithExceptionFailure() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS, new RuntimeException("mock"));

        proxy.canCommit().get();
    }

    @Test(expected=ExecutionException.class)
    public void testCanCommitWithInvalidResponseType() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CanCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

        proxy.canCommit().get();
    }

    @Test
    public void testPreCommit() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

        proxy.preCommit().get();

        verifyCohortInvocations(1, PreCommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected=ExecutionException.class)
    public void testPreCommitWithFailure() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(PreCommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply(), new RuntimeException("mock"));

        proxy.preCommit().get();
    }

    @Test
    public void testAbort() throws Exception {
        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(AbortTransaction.SERIALIZABLE_CLASS, new AbortTransactionReply());

        proxy.abort().get();

        verifyCohortInvocations(1, AbortTransaction.SERIALIZABLE_CLASS);
    }

    @Test
    public void testCommit() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS, new CommitTransactionReply(),
                new CommitTransactionReply());

        proxy.commit().get();

        verifyCohortInvocations(2, CommitTransaction.SERIALIZABLE_CLASS);
    }

    @Test(expected=ExecutionException.class)
    public void testCommitWithFailure() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS,
                new CommitTransactionReply(), new RuntimeException("mock"));

        proxy.commit().get();
    }

    @Test(expected=ExecutionException.class)
    public void teseCommitWithInvalidResponseType() throws Exception {

        ThreePhaseCommitCohortProxy proxy = setupProxy(1);

        setupMockActorContext(CommitTransaction.SERIALIZABLE_CLASS,
                new PreCommitTransactionReply());

        proxy.commit().get();
    }

    @Test
    public void testGetCohortPaths() {

        ThreePhaseCommitCohortProxy proxy = setupProxy(2);

        List<ActorPath> paths = proxy.getCohortPaths();
        assertNotNull("getCohortPaths returned null", paths);
        assertEquals("getCohortPaths size", 2, paths.size());
    }
}
