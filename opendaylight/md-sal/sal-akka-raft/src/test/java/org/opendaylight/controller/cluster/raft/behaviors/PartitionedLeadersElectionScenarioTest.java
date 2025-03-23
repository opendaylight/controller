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
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.raft.api.RaftRoles;

/**
 * A leader election scenario test that causes partitioned leaders by dropping messages between 2 members.
 *
 * @author Thomas Pantelis
 */
public class PartitionedLeadersElectionScenarioTest extends AbstractLeaderElectionScenarioTest {

    /**
     * This test sets up a scenario with partitioned leaders member 2 and 3 where partitioned leader 3
     * sends a heartbeat first when connectivity is re-established.
     */
    @Test
    public void runTest1() {
        testLog.info("PartitionedLeadersElectionScenarioTest 1 starting");

        setupInitialMemberBehaviors();

        sendInitialElectionTimeoutToFollowerMember2();

        sendInitialElectionTimeoutToFollowerMember3();

        sendElectionTimeoutToNowCandidateMember2();

        resolvePartitionedLeadersWithLeaderMember3SendingHeartbeatFirst();

        testLog.info("PartitionedLeadersElectionScenarioTest 1 ending");
    }

    /**
     * This test sets up a scenario with partitioned leaders member 2 and 3 where partitioned leader 2
     * sends a heartbeat first when connectivity is re-established.
     */
    @Test
    public void runTest2() {
        testLog.info("PartitionedLeadersElectionScenarioTest 2 starting");

        setupInitialMemberBehaviors();

        sendInitialElectionTimeoutToFollowerMember2();

        sendInitialElectionTimeoutToFollowerMember3();

        sendElectionTimeoutToNowCandidateMember2();

        resolvePartitionedLeadersWithLeaderMember2SendingHeartbeatFirst();

        testLog.info("PartitionedLeadersElectionScenarioTest 2 ending");
    }

    private void resolvePartitionedLeadersWithLeaderMember2SendingHeartbeatFirst() {
        testLog.info("resolvePartitionedLeadersWithLeaderMember2SendingHeartbeatFirst starting");

        // Re-establish connectivity between member 2 and 3, ie stop dropping messages between
        // the 2. Send heartbeats (AppendEntries) from partitioned leader member 2. Follower member 1 should
        // return a successful AppendEntriesReply b/c its term matches member 2's. member 3 should switch to
        // Follower as its term is less than member 2's.

        member1Actor.clear();
        member1Actor.expectMessageClass(AppendEntries.class, 1);

        member2Actor.clear();
        member2Actor.expectMessageClass(AppendEntriesReply.class, 1);

        member3Actor.clear();
        member3Actor.expectMessageClass(AppendEntries.class, 1);

        sendHeartbeat(member2ActorRef);

        member1Actor.waitForExpectedMessages(AppendEntries.class);
        member3Actor.waitForExpectedMessages(AppendEntries.class);

        member2Actor.waitForExpectedMessages(AppendEntriesReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftRoles.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftRoles.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftRoles.Follower);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", 3, member3Context.currentTerm());

        testLog.info("resolvePartitionedLeadersWithLeaderMember2SendingHeartbeatFirst ending");
    }

    private void resolvePartitionedLeadersWithLeaderMember3SendingHeartbeatFirst() {
        testLog.info("resolvePartitionedLeadersWithLeaderMember3SendingHeartbeatFirst starting");

        // Re-establish connectivity between member 2 and 3, ie stop dropping messages between
        // the 2. Send heartbeats (AppendEntries) from now leader member 3. Both member 1 and 2 should send
        // back an unsuccessful AppendEntriesReply b/c their term (3) is greater than member 3's term (2).
        // This should cause member 3 to switch to Follower.

        member1Actor.clear();
        member1Actor.expectMessageClass(AppendEntries.class, 1);

        member2Actor.clear();
        member2Actor.expectMessageClass(AppendEntries.class, 1);

        member3Actor.clear();
        member3Actor.expectMessageClass(AppendEntriesReply.class, 1);

        sendHeartbeat(member3ActorRef);

        member3Actor.waitForExpectedMessages(AppendEntriesReply.class);

        AppendEntriesReply appendEntriesReply = member3Actor.getCapturedMessage(AppendEntriesReply.class);
        assertFalse("isSuccess", appendEntriesReply.isSuccess());
        assertEquals("getTerm", 3, appendEntriesReply.getTerm());

        verifyBehaviorState("member 1", member1Actor, RaftRoles.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftRoles.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftRoles.Follower);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", 3, member3Context.currentTerm());

        testLog.info("resolvePartitionedLeadersWithLeaderMember3SendingHeartbeatFirst ending");
    }

    private void sendElectionTimeoutToNowCandidateMember2() {
        testLog.info("sendElectionTimeoutToNowCandidateMember2 starting");

        // member 2, now a candidate, is partitioned from the Leader (now member 3) and hasn't received any
        // messages. It would get another ElectionTimeout so simulate that. member 1 should send back a reply
        // granting the vote. Messages (RequestVote and AppendEntries) from member 2 to member 3
        // are dropped to simulate loss of network connectivity. Note member 2 will increment its
        // election term to 3.

        member1Actor.clear();
        member1Actor.expectMessageClass(AppendEntries.class, 1);

        member2Actor.clear();
        member2Actor.expectMessageClass(RequestVoteReply.class, 1);
        member2Actor.expectMessageClass(AppendEntriesReply.class, 1);

        member3Actor.clear();
        member3Actor.dropMessagesToBehavior(AppendEntries.class);
        member3Actor.dropMessagesToBehavior(RequestVote.class);

        member2ActorRef.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());

        member2Actor.waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member2Actor.getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member2Context.currentTerm(), requestVoteReply.getTerm());
        assertTrue("isVoteGranted", requestVoteReply.isVoteGranted());

        member3Actor.waitForExpectedMessages(RequestVote.class);

        member1Actor.waitForExpectedMessages(AppendEntries.class);
        member3Actor.waitForExpectedMessages(AppendEntries.class);
        member2Actor.waitForExpectedMessages(AppendEntriesReply.class);

        // We end up with 2 partitioned leaders both leading member 1. The term for member 1 and 3
        // is 3 and member 3's term is 2.

        verifyBehaviorState("member 1", member1Actor, RaftRoles.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftRoles.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftRoles.Leader);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", 2, member3Context.currentTerm());

        testLog.info("sendElectionTimeoutToNowCandidateMember2 ending");
    }

    private void sendInitialElectionTimeoutToFollowerMember3() {
        testLog.info("sendInitialElectionTimeoutToFollowerMember3 starting");

        // Send ElectionTimeout to member 3 to simulate no heartbeat from a Leader (originally member 1).
        // member 3 should switch to Candidate and send out RequestVote messages. member 1, now a follower,
        // should reply and grant the vote but member 2 will drop the message to simulate loss of network
        // connectivity between members 2 and 3. member 3 should switch to leader.

        member1Actor.clear();
        member1Actor.expectMessageClass(RequestVote.class, 1);
        member1Actor.expectMessageClass(AppendEntries.class, 1);

        member2Actor.clear();
        member2Actor.dropMessagesToBehavior(RequestVote.class);
        member2Actor.dropMessagesToBehavior(AppendEntries.class);

        member3Actor.clear();
        member3Actor.expectMessageClass(RequestVoteReply.class, 1);
        member3Actor.expectMessageClass(AppendEntriesReply.class, 1);

        member3ActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        member1Actor.waitForExpectedMessages(RequestVote.class);
        member2Actor.waitForExpectedMessages(RequestVote.class);
        member3Actor.waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member3Actor.getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member3Context.currentTerm(), requestVoteReply.getTerm());
        assertTrue("isVoteGranted", requestVoteReply.isVoteGranted());

        // when member 3 switches to Leader it will immediately send out heartbeat AppendEntries to
        // the followers. Wait for AppendEntries to member 1 and its AppendEntriesReply. The
        // AppendEntries message to member 2 is dropped.

        member1Actor.waitForExpectedMessages(AppendEntries.class);
        member2Actor.waitForExpectedMessages(AppendEntries.class);
        member3Actor.waitForExpectedMessages(AppendEntriesReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftRoles.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftRoles.Candidate);
        verifyBehaviorState("member 3", member3Actor, RaftRoles.Leader);

        assertEquals("member 1 election term", 2, member1Context.currentTerm());
        assertEquals("member 2 election term", 2, member2Context.currentTerm());
        assertEquals("member 3 election term", 2, member3Context.currentTerm());

        testLog.info("sendInitialElectionTimeoutToFollowerMember3 ending");
    }

    private void sendInitialElectionTimeoutToFollowerMember2() {
        testLog.info("sendInitialElectionTimeoutToFollowerMember2 starting");

        // Send ElectionTimeout to member 2 to simulate no heartbeat from the Leader (member 1).
        // member 2 should switch to Candidate, start new term 2 and send out RequestVote messages.
        // member 1 will switch to Follower b/c its term is less than the member 2's RequestVote term, also it
        // won't send back a reply. member 3 will drop the message (ie won't forward it to its behavior) to
        // simulate loss of network connectivity between members 2 and 3.

        member1Actor.expectMessageClass(RequestVote.class, 1);

        member2Actor.expectBehaviorStateChange();

        member3Actor.dropMessagesToBehavior(RequestVote.class);

        member2ActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        member1Actor.waitForExpectedMessages(RequestVote.class);
        member3Actor.waitForExpectedMessages(RequestVote.class);

        // Original leader member 1 should switch to Follower as the RequestVote term is greater than its
        // term. It won't send back a RequestVoteReply in this case.

        verifyBehaviorState("member 1", member1Actor, RaftRoles.Follower);

        // member 2 should switch to Candidate since it didn't get a RequestVoteReply from the other 2 members.

        member2Actor.waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftRoles.Candidate);

        assertEquals("member 1 election term", 2, member1Context.currentTerm());
        assertEquals("member 2 election term", 2, member2Context.currentTerm());
        assertEquals("member 3 election term", 1, member3Context.currentTerm());

        testLog.info("sendInitialElectionTimeoutToFollowerMember2 ending");
    }

    private void setupInitialMemberBehaviors() {
        testLog.info("setupInitialMemberBehaviors starting");

        // Create member 2's behavior initially as Follower

        member2Context = newRaftActorContext("member2", member2ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member1", member1ActorRef.path().toString())
                    .put("member3", member3ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member2ConfigParams = newConfigParams();
        member2Context.setConfigParams(member2ConfigParams);

        member2Actor.self().tell(new SetBehavior(new Follower(member2Context), member2Context),
                ActorRef.noSender());

        // Create member 3's behavior initially as Follower

        member3Context = newRaftActorContext("member3", member3ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member1", member1ActorRef.path().toString())
                    .put("member2", member2ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member3ConfigParams = newConfigParams();
        member3Context.setConfigParams(member3ConfigParams);

        member3Actor.self().tell(new SetBehavior(new Follower(member3Context), member3Context),
                ActorRef.noSender());

        // Create member 1's behavior initially as Leader

        member1Context = newRaftActorContext("member1", member1ActorRef,
                ImmutableMap.<String,String>builder()
                    .put("member2", member2ActorRef.path().toString())
                    .put("member3", member3ActorRef.path().toString()).build());

        DefaultConfigParamsImpl member1ConfigParams = newConfigParams();
        member1Context.setConfigParams(member1ConfigParams);

        initializeLeaderBehavior(member1Actor, member1Context, 2);

        member2Actor.clear();
        member3Actor.clear();

        testLog.info("setupInitialMemberBehaviors ending");
    }
}
