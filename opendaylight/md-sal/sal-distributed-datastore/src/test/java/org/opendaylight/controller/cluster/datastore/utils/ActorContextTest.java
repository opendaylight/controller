package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.ActorNotInitialized;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class ActorContextTest extends AbstractActorTest{

    private static class MockShardManager extends UntypedActor {

        private final boolean found;
        private final ActorRef actorRef;

        private MockShardManager(boolean found, ActorRef actorRef){

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override public void onReceive(Object message) throws Exception {
            if(found){
                getSender().tell(new LocalShardFound(actorRef), getSelf());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).getShardName()), getSelf());
            }
        }

        private static Props props(final boolean found, final ActorRef actorRef){
            return Props.create(new MockShardManagerCreator(found, actorRef) );
        }

        @SuppressWarnings("serial")
        private static class MockShardManagerCreator implements Creator<MockShardManager> {
            final boolean found;
            final ActorRef actorRef;

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
        DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

        doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
        doReturn("config").when(mockDataStoreContext).getDataStoreType();
        doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), mockDataStoreContext);

        // Check that the initial value is being picked up from DataStoreContext
        assertEquals(mockDataStoreContext.getTransactionCreationInitialRateLimit(), actorContext.getTxCreationLimit(), 1e-15);

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

        DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

        doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
        doReturn("config").when(mockDataStoreContext).getDataStoreType();
        doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), mockDataStoreContext);

        assertEquals(getSystem().dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

    }

    @Test
    public void testClientDispatcherIsNotGlobalDispatcher(){

        DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

        doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
        doReturn("config").when(mockDataStoreContext).getDataStoreType();
        doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

        ActorSystem actorSystem = ActorSystem.create("with-custom-dispatchers", ConfigFactory.load("application-with-custom-dispatchers.conf"));

        ActorContext actorContext =
                new ActorContext(actorSystem, mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), mockDataStoreContext);

        assertNotEquals(actorSystem.dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

        actorSystem.shutdown();

    }

    @Test
    public void testFindPrimaryShardAsyncPrimaryFound() throws Exception {

        new JavaTestKit(getSystem()) {
            {
                TestActorRef<MessageCollectorActor> shardManager =
                        TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

                DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

                doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
                doReturn("config").when(mockDataStoreContext).getDataStoreType();
                doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

                ActorContext actorContext =
                        new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                                mock(Configuration.class), mockDataStoreContext) {
                            @Override
                            protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                                return Futures.successful((Object) new PrimaryFound("akka://test-system/test"));
                            }
                        };


                Future<ActorSelection> foobar = actorContext.findPrimaryShardAsync("foobar");
                ActorSelection actual = Await.result(foobar, Duration.apply(100, TimeUnit.MILLISECONDS));

                assertNotNull(actual);

                ActorSelection cached = actorContext.getPrimaryShardActorSelectionCache().getIfPresent("foobar");

                assertEquals(cached, actual);

                // Wait for 200 Milliseconds. The cached entry should have been removed.

                Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

                cached = actorContext.getPrimaryShardActorSelectionCache().getIfPresent("foobar");

                assertNull(cached);

            }};

    }

    @Test
    public void testFindPrimaryShardAsyncPrimaryNotFound() throws Exception {

        new JavaTestKit(getSystem()) {
            {
                TestActorRef<MessageCollectorActor> shardManager =
                        TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

                DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

                doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
                doReturn("config").when(mockDataStoreContext).getDataStoreType();
                doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

                ActorContext actorContext =
                        new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                                mock(Configuration.class), mockDataStoreContext) {
                            @Override
                            protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                                return Futures.successful((Object) new PrimaryNotFound("foobar"));
                            }
                        };


                Future<ActorSelection> foobar = actorContext.findPrimaryShardAsync("foobar");

                try {
                    Await.result(foobar, Duration.apply(100, TimeUnit.MILLISECONDS));
                    fail("Expected PrimaryNotFoundException");
                } catch(PrimaryNotFoundException e){

                }

                ActorSelection cached = actorContext.getPrimaryShardActorSelectionCache().getIfPresent("foobar");

                assertNull(cached);

            }};

    }

    @Test
    public void testFindPrimaryShardAsyncActorNotInitialized() throws Exception {

        new JavaTestKit(getSystem()) {
            {
                TestActorRef<MessageCollectorActor> shardManager =
                        TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

                DatastoreContext mockDataStoreContext = mock(DatastoreContext.class);

                doReturn(155L).when(mockDataStoreContext).getTransactionCreationInitialRateLimit();
                doReturn("config").when(mockDataStoreContext).getDataStoreType();
                doReturn(Timeout.apply(100, TimeUnit.MILLISECONDS)).when(mockDataStoreContext).getShardLeaderElectionTimeout();

                ActorContext actorContext =
                        new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                                mock(Configuration.class), mockDataStoreContext) {
                            @Override
                            protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                                return Futures.successful((Object) new ActorNotInitialized());
                            }
                        };


                Future<ActorSelection> foobar = actorContext.findPrimaryShardAsync("foobar");

                try {
                    Await.result(foobar, Duration.apply(100, TimeUnit.MILLISECONDS));
                    fail("Expected NotInitializedException");
                } catch(NotInitializedException e){

                }

                ActorSelection cached = actorContext.getPrimaryShardActorSelectionCache().getIfPresent("foobar");

                assertNull(cached);

            }};

    }


}
