package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import scala.concurrent.duration.Duration;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindPrimary("inventory").toSerializable(), getRef());

                    expectMsgEquals(Duration.Zero(),
                        new PrimaryNotFound("inventory").toSerializable());

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
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindPrimary(Shard.DEFAULT_NAME).toSerializable(), getRef());

                    expectMsgClass(duration("1 seconds"), PrimaryFound.SERIALIZABLE_CLASS);

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindLocalShard("inventory"), getRef());

                    final String out = new ExpectMsg<String>(duration("10 seconds"), "find local") {
                        @Override
                        protected String match(Object in) {
                            if (in instanceof LocalShardNotFound) {
                                return ((LocalShardNotFound) in).getShardName();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("inventory", out);

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {

        final MockClusterWrapper mockClusterWrapper = new MockClusterWrapper();

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", mockClusterWrapper,
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindLocalShard(Shard.DEFAULT_NAME), getRef());

                    final ActorRef out = new ExpectMsg<ActorRef>(duration("10 seconds"), "find local") {
                        @Override
                        protected ActorRef match(Object in) {
                            if (in instanceof LocalShardFound) {
                                return ((LocalShardFound) in).getPath();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out.path().toString(), out.path().toString().contains("member-1-shard-default-config"));


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
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "primary found") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
                                PrimaryFound f = PrimaryFound.fromSerializable(in);
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
                    new MockConfiguration(), new ShardContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    expectMsgClass(duration("1 seconds"), PrimaryFound.SERIALIZABLE_CLASS);

                    MockClusterWrapper.sendMemberRemoved(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    expectMsgClass(duration("1 seconds"), PrimaryNotFound.SERIALIZABLE_CLASS);

                    expectNoMsg();
                }
            };
        }};
    }


}
