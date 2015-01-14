package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
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

}
