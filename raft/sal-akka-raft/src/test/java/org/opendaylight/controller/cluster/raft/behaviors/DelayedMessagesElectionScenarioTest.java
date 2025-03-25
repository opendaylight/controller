/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.raft.api.RaftRole;

/**
 * A leader election scenario test that delays various messages to behaviors to simulate network delays.
 *
 * @author Thomas Pantelis
 */
public class DelayedMessagesElectionScenarioTest extends AbstractLeaderElectionScenarioTest {

    @Test
    public void runTest() {
        testLog.info("DelayedMessagesElectionScenarioTest starting");

        setupInitialMemberBehaviors();

        sendInitialElectionTimeoutToFollowerMember2();

        forwardDelayedRequestVotesToLeaderMember1AndFollowerMember3();

        sendElectionTimeoutToFollowerMember3();

        forwardDelayedRequestVoteReplyFromOriginalFollowerMember3ToMember2();

        testLog.info("DelayedMessagesElectionScenarioTest ending");
    }

    private void forwardDelayedRequestVoteReplyFromOriginalFollowerMember3ToMember2() {
        testLog.info("forwardDelayedRequestVoteReplyFromOriginalFollowerMember3ToMember2 starting");

        // Now forward the original delayed RequestVoteReply from member 3 to member 2 that granted
        // the vote. Since member 2 is now a Follower, the RequestVoteReply should be ignored.

        member2Actor.clearDropMessagesToBehavior();
        member2Actor.forwardCapturedMessageToBehavior(RequestVoteReply.class, member3ActorRef);

        member2Actor.waitForExpectedMessages(RequestVoteReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftRole.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftRole.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftRole.Leader);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", 3, member3Context.currentTerm());

        testLog.info("forwardDelayedRequestVoteReplyFromOriginalFollowerMember3ToMember2 ending");
    }

    private void sendElectionTimeoutToFollowerMember3() {
        testLog.info("sendElectionTimeoutToFollowerMember3 starting");

        // Send ElectionTimeout to member 3 to simulate missing heartbeat from a Leader. member 3
        // should switch to Candidate and send out RequestVote messages. member 1 should grant the
        // vote and send a reply. After receiving the RequestVoteReply, member 3 should switch to leader.

        member2Actor.expectBehaviorStateChange();
        member3Actor.clear();
        member3Actor.expectMessageClass(RequestVoteReply.class, 1);
        member3Actor.expectMessageClass(AppendEntriesReply.class, 2);

        member3ActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        member3Actor.waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member3Actor.getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member3Context.currentTerm(), requestVoteReply.getTerm());
        assertTrue("isVoteGranted", requestVoteReply.isVoteGranted());

        verifyBehaviorState("member 3", member3Actor, RaftRole.Leader);

        // member 2 should've switched to Follower as member 3's RequestVote term (3) was greater
        // than member 2's term (2).

        member2Actor.waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftRole.Follower);

        // The switch to leader should cause an immediate AppendEntries heartbeat from member 3.

        member3Actor.waitForExpectedMessages(AppendEntriesReply.class);

        assertEquals("member 1 election term", 3, member1Context.currentTerm());
        assertEquals("member 2 election term", 3, member2Context.currentTerm());
        assertEquals("member 3 election term", 3, member3Context.currentTerm());

        testLog.info("sendElectionTimeoutToFollowerMember3 ending");
    }

    private void forwardDelayedRequestVotesToLeaderMember1AndFollowerMember3() {
        testLog.info("forwardDelayedRequestVotesToLeaderMember1AndFollowerMember3 starting");

        // At this point member 1 and 3 actors have captured the RequestVote messages. First
        // forward the RequestVote message to member 1's behavior. Since the RequestVote term
        // is greater than member 1's term and member 1 is a Leader, member 1 should switch to Follower
        // without replying to RequestVote and update its term to 2.

        member1Actor.clearDropMessagesToBehavior();
        member1Actor.expectBehaviorStateChange();
        member1Actor.forwardCapturedMessageToBehavior(RequestVote.class, member2ActorRef);
        member1Actor.waitForExpectedMessages(RequestVote.class);

        member1Actor.waitForBehaviorStateChange();
        verifyBehaviorState("member 1", member1Actor, RaftRole.Follower);

        // Now forward member 3's captured RequestVote message to its behavior. Since member 3 is
        // already a Follower, it should update its term to 2 and send a RequestVoteReply back to
        // member 2 granting the vote b/c the RequestVote's term, lastLogTerm, and lastLogIndex
        // should satisfy the criteria for granting the vote. However, we'll delay sending the
        // RequestVoteReply to member 2's behavior to simulate network latency.

        member2Actor.dropMessagesToBehavior(RequestVoteReply.class);

        member3Actor.clearDropMessagesToBehavior();
        member3Actor.expectMessageClass(RequestVote.class, 1);
        member3Actor.forwardCapturedMessageToBehavior(RequestVote.class, member2ActorRef);
        member3Actor.waitForExpectedMessages(RequestVote.class);
        verifyBehaviorState("member 3", member3Actor, RaftRole.Follower);

        assertEquals("member 1 election term", 2, member1Context.currentTerm());
        assertEquals("member 2 election term", 2, member2Context.currentTerm());
        assertEquals("member 3 election term", 2, member3Context.currentTerm());

        testLog.info("forwardDelayedRequestVotesToLeaderMember1AndFollowerMember3 ending");
    }

    private void sendInitialElectionTimeoutToFollowerMember2() {
        testLog.info("sendInitialElectionTimeoutToFollowerMember2 starting");

        // Send ElectionTimeout to member 2 to simulate missing heartbeat from the Leader. member 2
        // should switch to Candidate and send out RequestVote messages. Set member 1 and 3 actors
        // to capture RequestVote but not to forward to the behavior just yet as we want to
        // control the order of RequestVote messages to member 1 and 3.
        member2Actor.expectBehaviorStateChange();

        // member 1 and member 3 may reach consensus to consider leader's initial Noop entry as committed, hence
        // leader would elicit this information to member 2.
        // We do not want that, as member 2 would respond to that request either before it bumps or after it bumps its
        // term -- if it would see that message post-bump, it would leak term 2 back to member 1, hence leader would
        // know about it.
        member2Actor.dropMessagesToBehavior(AppendEntries.class);

        member1Actor.dropMessagesToBehavior(RequestVote.class);
        member3Actor.dropMessagesToBehavior(RequestVote.class);

        member2ActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());
        member1Actor.waitForExpectedMessages(RequestVote.class);
        member3Actor.waitForExpectedMessages(RequestVote.class);

        member2Actor.waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftRole.Candidate);

        assertEquals("member 1 election term", 1, member1Context.currentTerm());
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
