package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;

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

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(false, null));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Optional<ActorRef> out = actorContext.findLocalShard("default");
                    assertTrue(!out.isPresent());
                    expectNoMsg();
                }
            };
        }};

    }

    @Test
    public void testExecuteRemoteOperation() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(true, shardActorRef));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

                    Object out = actorContext.executeOperation(actor, "hello");

                    assertEquals("hello", out);

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testExecuteRemoteOperationAsync() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

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

                    expectNoMsg();
                }
            };
        }};
    }
}
