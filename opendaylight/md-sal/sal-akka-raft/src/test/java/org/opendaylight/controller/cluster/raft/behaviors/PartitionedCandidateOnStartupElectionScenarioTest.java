/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.SimpleReplicatedLog;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;

/**
 * A leader election scenario test that partitions a candidate when trying to join a cluster on startup.
 *
 * @author Thomas Pantelis
 */
public class PartitionedCandidateOnStartupElectionScenarioTest extends AbstractLeaderElectionScenarioTest {

    private final int numCandidateElections = 5;
    private long candidateElectionTerm;

    @Test
    public void runTest() {
        testLog.info("PartitionedCandidateOnStartupElectionScenarioTest starting");

        setupInitialMember1AndMember2Behaviors();

        setupPartitionedCandidateMember3AndSendElectionTimeouts();

        resolvePartitionAndSendElectionTimeoutsToCandidateMember3();

        sendElectionTimeoutToFollowerMember1();

        testLog.info("PartitionedCandidateOnStartupElectionScenarioTest ending");
    }

    private void sendElectionTimeoutToFollowerMember1() {
        testLog.info("sendElectionTimeoutToFollowerMember1 starting");

        // At this point we have no leader. Candidate member 3 would continue to start new elections
        // but wouldn't be granted a vote. One of the 2 followers would eventually time out from
        // not having received a heartbeat from a leader and switch to candidate and start a new
        // election. We'll simulate that here by sending an ElectionTimeout to member 1.

        member1Actor.clear();
        member1Actor.expectMessageClass(RequestVoteReply.class, 1);
        member2Actor.clear();
        member2Actor.expectMessageClass(RequestVote.class, 1);
        member3Actor.clear();
        member3Actor.expectMessageClass(RequestVote.class, 1);
        member3Actor.expectBehaviorStateChange();

        member1ActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        member2Actor.waitForExpectedMessages(RequestVote.class);
        member3Actor.waitForExpectedMessages(RequestVote.class);

        // The RequestVoteReply should come from Follower member 2 and the vote should be granted
        // since member 2's last term and index matches member 1's.

        member1Actor.waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member1Actor.getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member1Context.currentTerm(), requestVoteReply.getTerm());
        assertTrue("isVoteGranted", requestVoteReply.isVoteGranted());

        // Candidate member 3 should change to follower as its term should be less than the
        // RequestVote term (member 1 started a new term higher than the other member's terms).

        member3Actor.waitForBehaviorStateChange();

        verifyBehaviorState("member 1", member1Actor, RaftState.Leader);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Follower);

        // newTerm should be 10.

        long newTerm = candidateElectionTerm + 1;
        assertEquals("member 1 election term", newTerm, member1Context.currentTerm());
        assertEquals("member 2 election term", newTerm, member2Context.currentTerm());
        assertEquals("member 3 election term", newTerm, member3Context.currentTerm());

        testLog.info("sendElectionTimeoutToFollowerMember1 ending");
    }

    private void resolvePartitionAndSendElectionTimeoutsToCandidateMember3() {
        testLog.info("resolvePartitionAndSendElectionTimeoutsToCandidateMember3 starting");

        // Now send a couple more ElectionTimeouts to Candidate member 3 with the partition resolved.
        //
        // On the first RequestVote, Leader member 1 should switch to Follower as its term (s) is less than
        // the RequestVote's term (8) from member 3. No RequestVoteReply should be sent by member 1.
        // Follower member 2 should update its term since it less than the RequestVote's term and
        // should return a RequestVoteReply but should not grant the vote as its last term and index
        // is greater than the RequestVote's lastLogTerm and lastLogIndex, ie member 2's log is later
        // or more up to date than member 3's.
        //
        // On the second RequestVote, both member 1 and 2 are followers so they should update their
        // term and return a RequestVoteReply but should not grant the vote.

        candidateElectionTerm += 2;
        for (int i = 0; i < 2; i++) {
            member1Actor.clear();
            member1Actor.expectMessageClass(RequestVote.class, 1);
            member2Actor.clear();
            member2Actor.expectMessageClass(RequestVote.class, 1);
            member3Actor.clear();
            member3Actor.expectMessageClass(RequestVoteReply.class, 1);

            member3ActorRef.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());

            member1Actor.waitForExpectedMessages(RequestVote.class);
            member2Actor.waitForExpectedMessages(RequestVote.class);

            member3Actor.waitForExpectedMessages(RequestVoteReply.class);

            RequestVoteReply requestVoteReply = member3Actor.getCapturedMessage(RequestVoteReply.class);
            assertEquals("getTerm", member3Context.currentTerm(), requestVoteReply.getTerm());
            assertFalse("isVoteGranted", requestVoteReply.isVoteGranted());
        }

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Candidate);

        // Even though member 3 didn't get voted for, member 1 and 2 should have updated their term
        // to member 3's.

        assertEquals("member 1 election term", candidateElectionTerm, member1Context.currentTerm());
        assertEquals("member 2 election term", candidateElectionTerm, member2Context.currentTerm());
        assertEquals("member 3 election term", candidateElectionTerm, member3Context.currentTerm());

        testLog.info("resolvePartitionAndSendElectionTimeoutsToCandidateMember3 ending");
    }

    private void setupPartitionedCandidateMember3AndSendElectionTimeouts() {
        testLog.info("setupPartitionedCandidateMember3AndSendElectionTimeouts starting");

        // Create member 3's behavior initially as a Candidate.

        member3Context = newRaftActorContext("member3", member3ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member1", member1ActorRef.path().toString())
                    .put("member2", member2ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member3ConfigParams = newConfigParams();
        member3Context.setConfigParams(member3ConfigParams);

        // Initialize the ReplicatedLog and election term info for Candidate member 3. The current term
        // will be 2 and the last term will be 1 so it is behind the leader's log.

        SimpleReplicatedLog candidateReplicatedLog = new SimpleReplicatedLog();
        candidateReplicatedLog.append(new SimpleReplicatedLogEntry(0, 2, new MockPayload("")));

        candidateReplicatedLog.setCommitIndex(candidateReplicatedLog.lastIndex());
        candidateReplicatedLog.setLastApplied(candidateReplicatedLog.lastIndex());
        member3Context.setReplicatedLog(candidateReplicatedLog);
        member3Context.setTermInfo(new TermInfo(2, member1Context.getId()));

        // The member 3 Candidate will start a new term and send RequestVotes. However it will be
        // partitioned from the cluster by having member 1 and 2 drop its RequestVote messages.

        candidateElectionTerm = member3Context.currentTerm() + numCandidateElections;

        member1Actor.dropMessagesToBehavior(RequestVote.class, numCandidateElections);

        member2Actor.dropMessagesToBehavior(RequestVote.class, numCandidateElections);

        member3Actor.self().tell(new SetBehavior(new Candidate(member3Context), member3Context),
                ActorRef.noSender());

        // Send several additional ElectionTimeouts to Candidate member 3. Each ElectionTimeout will
        // start a new term so Candidate member 3's current term will be greater than the leader's
        // current term.

        for (int i = 0; i < numCandidateElections - 1; i++) {
            member3ActorRef.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());
        }

        member1Actor.waitForExpectedMessages(RequestVote.class);
        member2Actor.waitForExpectedMessages(RequestVote.class);

        verifyBehaviorState("member 1", member1Actor, RaftState.Leader);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Candidate);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", candidateElectionTerm, member3Context.currentTerm());

        testLog.info("setupPartitionedCandidateMember3AndSendElectionTimeouts ending");
    }

    private void setupInitialMember1AndMember2Behaviors() {
        testLog.info("setupInitialMember1AndMember2Behaviors starting");

        // Initialize the ReplicatedLog and election term info for member 1 and 2. The current term
        // will be 3 and the last term will be 2.

        SimpleReplicatedLog replicatedLog = new SimpleReplicatedLog();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, 2, new MockPayload("")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, 3, new MockPayload("")));

        // Create member 2's behavior as Follower.

        member2Context = newRaftActorContext("member2", member2ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member1", member1ActorRef.path().toString())
                    .put("member3", member3ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member2ConfigParams = newConfigParams();
        member2Context.setConfigParams(member2ConfigParams);

        replicatedLog.setCommitIndex(replicatedLog.lastIndex());
        replicatedLog.setLastApplied(replicatedLog.lastIndex());
        member2Context.setReplicatedLog(replicatedLog);
        member2Context.setTermInfo(new TermInfo(3, "member1"));

        member2Actor.self().tell(new SetBehavior(new Follower(member2Context), member2Context),
                ActorRef.noSender());

        // Create member 1's behavior as Leader.

        member1Context = newRaftActorContext("member1", member1ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member2", member2ActorRef.path().toString())
                    .put("member3", member3ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member1ConfigParams = newConfigParams();
        member1Context.setConfigParams(member1ConfigParams);

        replicatedLog.setCommitIndex(replicatedLog.lastIndex());
        replicatedLog.setLastApplied(replicatedLog.lastIndex());
        member1Context.setReplicatedLog(replicatedLog);
        member1Context.setTermInfo(new TermInfo(3, "member1"));

        initializeLeaderBehavior(member1Actor, member1Context, 1);

        member2Actor.clear();
        member3Actor.clear();

        testLog.info("setupInitialMember1AndMember2Behaviors ending");
    }
}
