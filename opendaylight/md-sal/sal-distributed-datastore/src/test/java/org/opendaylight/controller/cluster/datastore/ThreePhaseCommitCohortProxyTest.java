package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;

public class ThreePhaseCommitCohortProxyTest extends AbstractActorTest {

    private ThreePhaseCommitCohortProxy proxy;
    private Props props;
    private ActorRef actorRef;
    private MockActorContext actorContext;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Before
    public void setUp(){
        props = Props.create(MessageCollectorActor.class);
        actorRef = getSystem().actorOf(props);
        actorContext = new MockActorContext(this.getSystem());

        proxy =
            new ThreePhaseCommitCohortProxy(actorContext,
                Arrays.asList(actorRef.path()), "txn-1", executor);

    }

    @Test
    public void testCanCommit() throws Exception {
        actorContext.setExecuteRemoteOperationResponse(new CanCommitTransactionReply(true));

        ListenableFuture<Boolean> future = proxy.canCommit();

        Assert.assertTrue(future.get().booleanValue());

    }

    @Test
    public void testPreCommit() throws Exception {
        actorContext.setExecuteRemoteOperationResponse(new PreCommitTransactionReply());

        ListenableFuture<Void> future = proxy.preCommit();

        future.get();

    }

    @Test
    public void testAbort() throws Exception {
        actorContext.setExecuteRemoteOperationResponse(new AbortTransactionReply());

        ListenableFuture<Void> future = proxy.abort();

        future.get();

    }

    @Test
    public void testCommit() throws Exception {
        actorContext.setExecuteRemoteOperationResponse(new CommitTransactionReply());

        ListenableFuture<Void> future = proxy.commit();

        future.get();
    }

    @Test
    public void testGetCohortPaths() throws Exception {
        assertNotNull(proxy.getCohortPaths());
    }
}
