package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LeaderTest extends AbstractRaftActorBehaviorTest {

    private ActorRef leaderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));
    private ActorRef senderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));

    @Test
    public void testHandleMessageForUnknownMessage() throws Exception {
        new JavaTestKit(getSystem()) {{
            Leader leader =
                new Leader(createActorContext());

            // handle message should return the Leader state when it receives an
            // unknown message
            RaftState state = leader.handleMessage(senderActor, "foo");
            Assert.assertEquals(RaftState.Leader, state);
        }};
    }


    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    Leader leader = new Leader(actorContext);
                    leader.handleMessage(senderActor, new SendHeartBeat());

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            protected String match(Object in) {
                                Object msg = fromSerializableMessage(in);
                                if (msg instanceof AppendEntries) {
                                    if (((AppendEntries)msg).getTerm() == 0) {
                                        return "match";
                                    }
                                    return null;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertEquals("match", out);

                }


            };
        }};
    }

    @Test
    public void testHandleReplicateMessageSendAppendEntriesToFollower() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    Leader leader = new Leader(actorContext);
                    RaftState raftState = leader
                        .handleMessage(senderActor, new Replicate(null, null,
                            new MockRaftActorContext.MockReplicatedLogEntry(1,
                                100,
                                new MockRaftActorContext.MockPayload("foo"))
                        ));

                    // State should not change
                    assertEquals(RaftState.Leader, raftState);

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            protected String match(Object in) {
                                Object msg = fromSerializableMessage(in);
                                if (msg instanceof AppendEntries) {
                                    if (((AppendEntries)msg).getTerm() == 0) {
                                        return "match";
                                    }
                                    return null;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertEquals("match", out);

                }


            };
        }};
    }

    @Test
    public void testHandleReplicateMessageWhenThereAreNoFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef raftActor = getTestActor();

                    MockRaftActorContext actorContext =
                        new MockRaftActorContext("test", getSystem(), raftActor);

                    actorContext.getReplicatedLog().removeFrom(0);

                    actorContext.getReplicatedLog().append(new ReplicatedLogImplEntry(0, 1,
                        new MockRaftActorContext.MockPayload("foo")));

                    ReplicatedLogImplEntry entry =
                        new ReplicatedLogImplEntry(1, 1,
                            new MockRaftActorContext.MockPayload("foo"));

                    actorContext.getReplicatedLog().append(entry);

                    Leader leader = new Leader(actorContext);
                    RaftState raftState = leader
                        .handleMessage(senderActor, new Replicate(null, "state-id",entry));

                    // State should not change
                    assertEquals(RaftState.Leader, raftState);

                    assertEquals(1, actorContext.getCommitIndex());

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"),
                            "match hint") {
                            // do not put code outside this method, will run afterwards
                            protected String match(Object in) {
                                if (in instanceof ApplyState) {
                                    if (((ApplyState) in).getIdentifier().equals("state-id")) {
                                        return "match";
                                    }
                                    return null;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertEquals("match", out);

                }


            };
        }};
    }

    @Override protected RaftActorBehavior createBehavior(
        RaftActorContext actorContext) {
        return new Leader(actorContext);
    }

    @Override protected RaftActorContext createActorContext() {
        return new MockRaftActorContext("test", getSystem(), leaderActor);
    }
}
