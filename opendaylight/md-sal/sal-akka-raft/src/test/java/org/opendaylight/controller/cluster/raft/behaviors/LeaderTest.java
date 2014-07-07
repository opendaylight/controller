package org.opendaylight.controller.cluster.raft.behaviors;

import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LeaderTest extends AbstractActorTest {

    @Test
    public void testHandleMessageForUnknownMessage() throws Exception {
        new JavaTestKit(getSystem()) {{
            Leader leader =
                new Leader(new MockRaftActorContext(), Collections.EMPTY_LIST);

            // handle message should return the Leader state when it receives an
            // unknown message
            RaftState state = leader.handleMessage(getRef(), "foo");
            Assert.assertEquals(RaftState.Leader, state);
        }};
    }


    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    List<String> followers = new ArrayList();

                    followers.add(getTestActor().path().toString());

                    Leader leader = new Leader(new MockRaftActorContext("test", getSystem(), getTestActor()), followers);
                    leader.handleMessage(getRef(), new SendHeartBeat());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof AppendEntries) {
                                if (((AppendEntries) in).getTerm()
                                    == 0) {
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
}
