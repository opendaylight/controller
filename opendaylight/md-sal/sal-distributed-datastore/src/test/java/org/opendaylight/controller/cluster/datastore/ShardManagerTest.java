package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import scala.concurrent.duration.Duration;

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
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager.props("config");
            final TestActorRef<ShardManager> subject = TestActorRef.create(system, props);

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(new FindPrimary("inventory"), getRef());

                    expectMsgEquals(Duration.Zero(), new PrimaryNotFound("inventory"));

                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }
            };
        }};
    }

  @Test
  public void testOnReceiveFindPrimaryForExistentShard() throws Exception {

    new JavaTestKit(system) {{
      final Props props = ShardManager.props("config");
      final TestActorRef<ShardManager> subject = TestActorRef.create(system, props);

      // the run() method needs to finish within 3 seconds
      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new FindPrimary(Shard.DEFAULT_NAME), getRef());

          expectMsgClass(PrimaryFound.class);

          expectNoMsg();
        }
      };
    }};
  }
}