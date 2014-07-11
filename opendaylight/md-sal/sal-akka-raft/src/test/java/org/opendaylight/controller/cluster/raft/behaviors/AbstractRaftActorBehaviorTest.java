package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public abstract class AbstractRaftActorBehaviorTest extends AbstractActorTest{
   @Test
    public void testHandlingOfRaftRPCWithNewerTerm() throws Exception {
        new JavaTestKit(getSystem()) {{

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createAppendEntriesWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createAppendEntriesReplyWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createRequestVoteWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createRequestVoteReplyWithNewerTerm());


        }};
    }

    @Test
    public void testHandlingOfAppendEntriesWithNewerCommitIndex() throws Exception{
        new JavaTestKit(getSystem()) {{

            RaftActorContext context =
                createActorContext();

            ((MockRaftActorContext) context).setLastApplied(new AtomicLong(100));

            AppendEntries appendEntries =
                new AppendEntries(100, "leader-1", 0, 0, null, 101);

            RaftState raftState =
                createBehavior(context).handleMessage(getRef(), appendEntries);

            assertEquals(new AtomicLong(101).get(), context.getLastApplied().get());

        }};
    }

    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(
        ActorRef actorRef, RaftRPC rpc){
        RaftState raftState = createBehavior()
            .handleMessage(actorRef, rpc);

        assertEquals(RaftState.Follower, raftState);
    }

    protected abstract RaftActorBehavior createBehavior(RaftActorContext actorContext);

    protected RaftActorBehavior createBehavior(){
        return createBehavior(createActorContext());
    }

    protected RaftActorContext createActorContext(){
        return new MockRaftActorContext();
    }

    protected AppendEntries createAppendEntriesWithNewerTerm(){
        return new AppendEntries(100, "leader-1", 0, 0, null, 1);
    }

    protected AppendEntriesReply createAppendEntriesReplyWithNewerTerm(){
        return new AppendEntriesReply(100, false);
    }

    protected RequestVote createRequestVoteWithNewerTerm(){
        return new RequestVote(100, "candidate-1", 10, 100);
    }

    protected RequestVoteReply createRequestVoteReplyWithNewerTerm(){
        return new RequestVoteReply(100, false);
    }

}
