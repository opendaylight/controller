package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class ActorContextTest extends AbstractActorTest{

    static final Logger log = LoggerFactory.getLogger(ActorContextTest.class);

    private static class TestMessage {
    }

    private static class MockShardManager extends UntypedActor {

        private final boolean found;
        private final ActorRef actorRef;
        private final Map<String,Object> findPrimaryResponses = Maps.newHashMap();

        private MockShardManager(boolean found, ActorRef actorRef){

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override public void onReceive(Object message) throws Exception {
            if(message instanceof FindPrimary) {
                FindPrimary fp = (FindPrimary)message;
                Object resp = findPrimaryResponses.get(fp.getShardName());
                if(resp == null) {
                    log.error("No expected FindPrimary response found for shard name {}", fp.getShardName());
                } else {
                    getSender().tell(resp, getSelf());
                }

                return;
            }

            if(found){
                getSender().tell(new LocalShardFound(actorRef), getSelf());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).getShardName()), getSelf());
            }
        }

        void addFindPrimaryResp(String shardName, Object resp) {
            findPrimaryResponses.put(shardName, resp);
        }

        private static Props props(final boolean found, final ActorRef actorRef){
            return Props.create(new MockShardManagerCreator(found, actorRef) );
        }

        private static Props props(){
            return Props.create(new MockShardManagerCreator() );
        }

        @SuppressWarnings("serial")
        private static class MockShardManagerCreator implements Creator<MockShardManager> {
            final boolean found;
            final ActorRef actorRef;

            MockShardManagerCreator() {
                this.found = false;
                this.actorRef = null;
            }

            MockShardManagerCreator(boolean found, ActorRef actorRef) {
                this.found = found;
                this.actorRef = actorRef;
            }

            @Override
            public MockShardManager create() throws Exception {
                return new MockShardManager(found, actorRef);
            }
        }
    }

    @Test
    public void testFindLocalShardWithShardFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(true, shardActorRef));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Optional<ActorRef> out = actorContext.findLocalShard("default");

                    assertEquals(shardActorRef, out.get());


                    expectNoMsg();
                }
            };
        }};

    }

    @Test
    public void testFindLocalShardWithShardNotFound(){
        new JavaTestKit(getSystem()) {{
            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(false, null));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            Optional<ActorRef> out = actorContext.findLocalShard("default");
            assertTrue(!out.isPresent());
        }};

    }

    @Test
    public void testExecuteRemoteOperation() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(true, shardActorRef));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

            Object out = actorContext.executeOperation(actor, "hello");

            assertEquals("hello", out);
        }};
    }

    @Test
    public void testExecuteRemoteOperationAsync() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(true, shardActorRef));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

            Future<Object> future = actorContext.executeOperationAsync(actor, "hello");

            try {
                Object result = Await.result(future, Duration.create(3, TimeUnit.SECONDS));
                assertEquals("Result", "hello", result);
            } catch(Exception e) {
                throw new AssertionError(e);
            }
        }};
    }

    @Test
    public void testIsPathLocal() {
        MockClusterWrapper clusterWrapper = new MockClusterWrapper();
        ActorContext actorContext = null;

        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(null));
        assertEquals(false, actorContext.isPathLocal(""));

        clusterWrapper.setSelfAddress(null);
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(""));

        // even if the path is in local format, match the primary path (first 3 elements) and return true
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/token2/token3/$a"));

        // self address of remote format,but Tx path local format.
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal(
            "akka://system/user/shardmanager/shard/transaction"));

        // self address of local format,but Tx path remote format.
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(
            "akka://system@127.0.0.1:2550/user/shardmanager/shard/transaction"));

        //local path but not same
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test1/user/$a"));

        //ip and port same
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2550/"));

        // forward-slash missing in address
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2550"));

        //ips differ
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.1.0.1:2550/"));

        //ports differ
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2551/"));
    }

    @Test
    public void testResolvePathForRemoteActor() {
        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(
                        ClusterWrapper.class),
                        mock(Configuration.class));

        String actual = actorContext.resolvePath(
                "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard",
                "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);
    }

    @Test
    public void testResolvePathForLocalActor() {
        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class));

        String actual = actorContext.resolvePath(
                "akka://system/user/shardmanager/shard",
                "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka://system/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);
    }

    @Test
    public void testResolvePathForRemoteActorWithProperRemoteAddress() {
        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class));

        String actual = actorContext.resolvePath(
                "akka.tcp://system@7.0.0.1:2550/user/shardmanager/shard",
                "akka.tcp://system@7.0.0.1:2550/user/shardmanager/shard/transaction");

        String expected = "akka.tcp://system@7.0.0.1:2550/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);
    }

    @Test
    public void testRateLimiting(){
        DatastoreContext dataStoreContext = DatastoreContext.newBuilder().dataStoreType("config").
                transactionCreationInitialRateLimit(155L).build();

        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), dataStoreContext);

        // Check that the initial value is being picked up from DataStoreContext
        assertEquals(dataStoreContext.getTransactionCreationInitialRateLimit(), actorContext.getTxCreationLimit(), 1e-15);

        actorContext.setTxCreationLimit(1.0);

        assertEquals(1.0, actorContext.getTxCreationLimit(), 1e-15);


        StopWatch watch = new StopWatch();

        watch.start();

        actorContext.acquireTxCreationPermit();
        actorContext.acquireTxCreationPermit();
        actorContext.acquireTxCreationPermit();

        watch.stop();

        assertTrue("did not take as much time as expected", watch.getTime() > 1000);
    }

    @Test
    public void testClientDispatcherIsGlobalDispatcher(){
        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), DatastoreContext.newBuilder().build());

        assertEquals(getSystem().dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

    }

    @Test
    public void testClientDispatcherIsNotGlobalDispatcher(){
        ActorSystem actorSystem = ActorSystem.create("with-custom-dispatchers", ConfigFactory.load("application-with-custom-dispatchers.conf"));

        ActorContext actorContext =
                new ActorContext(actorSystem, mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), DatastoreContext.newBuilder().build());

        assertNotEquals(actorSystem.dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

        actorSystem.shutdown();

    }

    @Test
    public void testSetDatastoreContext() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(), mock(ClusterWrapper.class),
                            mock(Configuration.class), DatastoreContext.newBuilder().
                                operationTimeoutInSeconds(5).shardTransactionCommitTimeoutInSeconds(7).build());

            assertEquals("getOperationDuration", 5, actorContext.getOperationDuration().toSeconds());
            assertEquals("getTransactionCommitOperationTimeout", 7,
                    actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());

            DatastoreContext newContext = DatastoreContext.newBuilder().operationTimeoutInSeconds(6).
                    shardTransactionCommitTimeoutInSeconds(8).build();

            actorContext.setDatastoreContext(newContext);

            expectMsgClass(duration("5 seconds"), DatastoreContext.class);

            Assert.assertSame("getDatastoreContext", newContext, actorContext.getDatastoreContext());

            assertEquals("getOperationDuration", 6, actorContext.getOperationDuration().toSeconds());
            assertEquals("getTransactionCommitOperationTimeout", 8,
                    actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());
        }};
    }

    @Test
    public void testBroadcast() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef1 = getSystem().actorOf(Props.create(MessageCollectorActor.class));
            ActorRef shardActorRef2 = getSystem().actorOf(Props.create(MessageCollectorActor.class));

            TestActorRef<MockShardManager> shardManagerActorRef = TestActorRef.create(getSystem(), MockShardManager.props());
            MockShardManager shardManagerActor = shardManagerActorRef.underlyingActor();
            shardManagerActor.addFindPrimaryResp("shard1", new PrimaryFound(shardActorRef1.path().toString()));
            shardManagerActor.addFindPrimaryResp("shard2", new PrimaryFound(shardActorRef2.path().toString()));
            shardManagerActor.addFindPrimaryResp("shard3", new NoShardLeaderException("not found"));

            Configuration mockConfig = mock(Configuration.class);
            doReturn(Sets.newLinkedHashSet(Arrays.asList("shard1", "shard2", "shard3"))).
                    when(mockConfig).getAllShardNames();

            ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
                    mock(ClusterWrapper.class), mockConfig,
                    DatastoreContext.newBuilder().shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build());

            actorContext.broadcast(new TestMessage());

            expectFirstMatching(shardActorRef1, TestMessage.class);
            expectFirstMatching(shardActorRef2, TestMessage.class);
        }};
    }

    private <T> T expectFirstMatching(ActorRef actor, Class<T> clazz) {
        int count = 5000 / 50;
        for(int i = 0; i < count; i++) {
            try {
                T message = (T) MessageCollectorActor.getFirstMatching(actor, clazz);
                if(message != null) {
                    return message;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }
}
