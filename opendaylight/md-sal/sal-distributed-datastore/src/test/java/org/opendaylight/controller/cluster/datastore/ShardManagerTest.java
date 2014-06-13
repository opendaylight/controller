package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.ActorSystemImpl;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import scala.concurrent.duration.Duration;

import static org.junit.Assert.*;

public class ShardManagerTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setUp(){
        system = ActorSystem.create("test");
    }

    @AfterClass
    public static void tearDown(){
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testOnReceiveFindPrimary() throws Exception {

        new JavaTestKit(system) {{
            final Props props = Props.create(ShardManager.class);
            final TestActorRef<ShardManager> subject = TestActorRef.create(system, props, "test");

            // can also use JavaTestKit “from the outside”
            final JavaTestKit probe = new JavaTestKit(system);

            // the run() method needs to finish within 3 seconds
            new Within(duration("3 seconds")) {
                protected void run() {

                    subject.tell(new FindPrimary("inventory"), getRef());

                    expectMsgEquals(Duration.Zero(), new PrimaryNotFound("inventory"));

                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }
            };
        }};
    }
}