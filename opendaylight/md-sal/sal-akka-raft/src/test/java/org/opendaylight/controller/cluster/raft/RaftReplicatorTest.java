package org.opendaylight.controller.cluster.raft;

import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.internal.messages.SendHeartBeat;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class RaftReplicatorTest extends AbstractActorTest {

    @Test
    public void testThatHeartBeatIsGenerated () throws Exception {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    getSystem().actorOf(RaftReplicator.props(
                        new FollowerLogInformationImpl("test",
                            new AtomicLong(100), new AtomicLong(100)),
                        getRef()));

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof SendHeartBeat) {
                                return "match";
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
