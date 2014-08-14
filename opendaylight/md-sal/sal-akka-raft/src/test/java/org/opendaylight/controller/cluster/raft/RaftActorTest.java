package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;

import java.util.Collections;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class RaftActorTest extends AbstractActorTest {


    public static class MockRaftActor extends RaftActor {

        public MockRaftActor(String id,
            Map<String, String> peerAddresses) {
            super(id, peerAddresses);
        }

        public static Props props(final String id, final Map<String, String> peerAddresses){
            return Props.create(new Creator<MockRaftActor>(){

                @Override public MockRaftActor create() throws Exception {
                    return new MockRaftActor(id, peerAddresses);
                }
            });
        }

        @Override protected void applyState(ActorRef clientActor,
            String identifier,
            Object data) {
        }

        @Override protected Object createSnapshot() {
            throw new UnsupportedOperationException("createSnapshot");
        }

        @Override protected void applySnapshot(Object snapshot) {
            throw new UnsupportedOperationException("applySnapshot");
        }

        @Override protected void onStateChanged() {
        }

        @Override public String persistenceId() {
            return this.getId();
        }

    }


    private static class RaftActorTestKit extends JavaTestKit {
        private final ActorRef raftActor;

        public RaftActorTestKit(ActorSystem actorSystem, String actorName) {
            super(actorSystem);

            raftActor = this.getSystem()
                .actorOf(MockRaftActor.props(actorName,
                    Collections.EMPTY_MAP), actorName);

        }


        public boolean waitForStartup(){
            // Wait for a specific log message to show up
            return
                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                ) {
                    protected Boolean run() {
                        return true;
                    }
                }.from(raftActor.path().toString())
                    .message("Switching from state Candidate to Leader")
                    .occurrences(1).exec();


        }

        public void findLeader(final String expectedLeader){


            new Within(duration("1 seconds")) {
                protected void run() {

                    raftActor.tell(new FindLeader(), getRef());

                    String s = new ExpectMsg<String>(duration("1 seconds"),
                        "findLeader") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof FindLeaderReply) {
                                return ((FindLeaderReply) in).getLeaderActor();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();// this extracts the received message

                    assertEquals(expectedLeader, s);

                }


            };
        }

        public ActorRef getRaftActor() {
            return raftActor;
        }

    }


    @Test
    public void testConstruction() {
        boolean started = new RaftActorTestKit(getSystem(), "testConstruction").waitForStartup();
        assertEquals(true, started);
    }

    @Test
    public void testFindLeaderWhenLeaderIsSelf(){
        RaftActorTestKit kit = new RaftActorTestKit(getSystem(), "testFindLeader");
        kit.waitForStartup();
        kit.findLeader(kit.getRaftActor().path().toString());
    }


}
