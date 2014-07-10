package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import scala.concurrent.duration.Duration;

public class ShardManagerTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setUp() {
        system = ActorSystem.create("test");
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(new FindPrimary("inventory"), getRef());

                    expectMsgEquals(Duration.Zero(),
                        new PrimaryNotFound("inventory"));

                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

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

    @Test
    public void testOnReceiveMemberUp() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("1 seconds")) {
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts"), getRef());

                    final String out = new ExpectMsg<String>("primary found") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof PrimaryFound) {
                                PrimaryFound f = (PrimaryFound) in;
                                return f.getPrimaryPath();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    Assert.assertTrue(out, out.contains("member-2-shard-astronauts-config"));

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testOnReceiveMemberDown() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("1 seconds")) {
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts"), getRef());

                    expectMsgClass(PrimaryFound.class);

                    MockClusterWrapper.sendMemberRemoved(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts"), getRef());

                    expectMsgClass(PrimaryNotFound.class);

                    expectNoMsg();
                }
            };
        }};
    }


}
