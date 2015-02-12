/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.SimpleReplicatedLog;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import scala.concurrent.duration.FiniteDuration;

/**
 * Tests various leader election scenarios.
 *
 * @author Thomas Pantelis
 */
public class LeaderElectionScenariosTest {

    private static final int HEARTBEAT_INTERVAL = 50;

    public static class MemberActor extends MessageCollectorActor {

        volatile RaftActorBehavior behavior;
        Map<Class<?>, CountDownLatch> messagesReceivedLatches = new ConcurrentHashMap<>();
        Map<Class<?>, Boolean> dropMessagesToBehavior = new ConcurrentHashMap<>();
        CountDownLatch behaviorStateChangeLatch;

        public static Props props() {
            return Props.create(MemberActor.class).withDispatcher(Dispatchers.DefaultDispatcherId());
        }

        @Override
        public void onReceive(Object message) throws Exception {
            // Ignore scheduled SendHeartBeat messages.
            if(message instanceof SendHeartBeat) {
                return;
            }

            try {
                if(behavior != null && !dropMessagesToBehavior.containsKey(message.getClass())) {
                    RaftActorBehavior oldBehavior = behavior;
                    behavior = behavior.handleMessage(getSender(), message);
                    if(behavior != oldBehavior && behaviorStateChangeLatch != null) {
                        behaviorStateChangeLatch.countDown();
                    }
                }
            } finally {
                super.onReceive(message);

                CountDownLatch latch = messagesReceivedLatches.get(message.getClass());
                if(latch != null) {
                    latch.countDown();
                }
            }
        }

        void expectBehaviorStateChange() {
            behaviorStateChangeLatch = new CountDownLatch(1);
        }

        void waitForBehaviorStateChange() {
            assertTrue("Expected behavior state change",
                    Uninterruptibles.awaitUninterruptibly(behaviorStateChangeLatch, 5, TimeUnit.SECONDS));
        }

        void expectMessageClass(Class<?> expClass, int expCount) {
            messagesReceivedLatches.put(expClass, new CountDownLatch(expCount));
        }

        void waitForExpectedMessages(Class<?> expClass) {
            CountDownLatch latch = messagesReceivedLatches.get(expClass);
            assertNotNull("No messages received for " + expClass, latch);
            assertTrue("Missing messages of type " + expClass,
                    Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS));
        }

        void dropMessagesToBehavior(Class<?> msgClass) {
            dropMessagesToBehavior(msgClass, 1);
        }

        void dropMessagesToBehavior(Class<?> msgClass, int expCount) {
            expectMessageClass(msgClass, expCount);
            dropMessagesToBehavior.put(msgClass, Boolean.TRUE);
        }

        void clearDropMessagesToBehavior() {
            dropMessagesToBehavior.clear();
        }

        @Override
        public void clear() {
            behaviorStateChangeLatch = null;
            clearDropMessagesToBehavior();
            messagesReceivedLatches.clear();
            super.clear();
        }

        void forwardCapturedMessageToBehavior(Class<?> msgClass, ActorRef sender) throws Exception {
            Object message = getFirstMatching(getSelf(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            getSelf().tell(message, sender);
        }

        void forwardCapturedMessagesToBehavior(Class<?> msgClass, ActorRef sender) throws Exception {
            for(Object m: getAllMatching(getSelf(), msgClass)) {
                getSelf().tell(m, sender);
            }
        }

        <T> T getCapturedMessage(Class<T> msgClass) throws Exception {
            Object message = getFirstMatching(getSelf(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            return (T) message;
        }
    }

    static {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + MockRaftActorContext.class.getName(), "trace");
    }

    private final Logger testLog = LoggerFactory.getLogger(MockRaftActorContext.class);
    private final ActorSystem system = ActorSystem.create("test");

    @After
    public void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
    }

    private DefaultConfigParamsImpl newConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        return configParams;
    }

    private MockRaftActorContext newRaftActorContext(String id, ActorRef actor,
            Map<String, String> peerAddresses) {
        MockRaftActorContext context = new MockRaftActorContext(id, system, actor);
        context.setPeerAddresses(peerAddresses);
        context.getTermInformation().updateAndPersist(1, "");
        return context;
    }

    private void verifyBehaviorState(String name, TestActorRef<MemberActor> actor, RaftState expState) {
        assertEquals(name + " behavior state", expState, actor.underlyingActor().behavior.state());
    }

    private void initializeLeaderBehavior(TestActorRef<MemberActor> actor, RaftActorContext context,
            int numActiveFollowers) throws Exception {
        // Leader sends immediate heartbeats - we don't care about it so ignore it.

        actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, numActiveFollowers);
        Leader leader = new Leader(context);
        actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);
        actor.underlyingActor().behavior = leader;

        actor.underlyingActor().forwardCapturedMessagesToBehavior(AppendEntriesReply.class, ActorRef.noSender());
        actor.underlyingActor().clear();
    }

    private TestActorRef<MemberActor> newMemberActor(String name) throws Exception {
        TestActorRef<MemberActor> actor = TestActorRef.create(system, MemberActor.props(), name);
        MessageCollectorActor.waitUntilReady(actor);
        return actor;
    }

    private void sendHeartbeat(TestActorRef<MemberActor> leaderActor) {
        Uninterruptibles.sleepUninterruptibly(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        leaderActor.underlyingActor().behavior.handleMessage(leaderActor, new SendHeartBeat());
    }

    @Test
    public void testDelayedMessagesScenario() throws Exception {
        testLog.info("Starting testDelayedMessagesScenario");

        TestActorRef<MemberActor> member1Actor = newMemberActor("member1");
        TestActorRef<MemberActor> member2Actor = newMemberActor("member2");
        TestActorRef<MemberActor> member3Actor = newMemberActor("member3");

        // Create member 2's behavior initially as Follower

        MockRaftActorContext member2Context = newRaftActorContext("member2", member2Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member2ConfigParams = newConfigParams();
        member2Context.setConfigParams(member2ConfigParams);

        Follower member2Behavior = new Follower(member2Context);
        member2Actor.underlyingActor().behavior = member2Behavior;

        // Create member 3's behavior initially as Follower

        MockRaftActorContext member3Context = newRaftActorContext("member3", member3Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member2", member2Actor.path().toString()).build());

        DefaultConfigParamsImpl member3ConfigParams = newConfigParams();
        member3Context.setConfigParams(member3ConfigParams);

        Follower member3Behavior = new Follower(member3Context);
        member3Actor.underlyingActor().behavior = member3Behavior;

        // Create member 1's behavior initially as Leader

        MockRaftActorContext member1Context = newRaftActorContext("member1", member1Actor,
                ImmutableMap.<String,String>builder().
                    put("member2", member2Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member1ConfigParams = newConfigParams();
        member1Context.setConfigParams(member1ConfigParams);

        initializeLeaderBehavior(member1Actor, member1Context, 2);

        member2Actor.underlyingActor().clear();
        member3Actor.underlyingActor().clear();

        // Send ElectionTimeout to member 2 to simulate missing heartbeat from the Leader. member 2
        // should switch to Candidate and send out RequestVote messages. Set member 1 and 3 actors
        // to capture RequestVote but not to forward to the behavior just yet as we want to
        // control the order of RequestVote messages to member 1 and 3.

        member1Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class);

        member2Actor.underlyingActor().expectBehaviorStateChange();

        member3Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class);

        member2Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member3Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        member2Actor.underlyingActor().waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftState.Candidate);

        assertEquals("member 1 election term", 1, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 2, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 1, member3Context.getTermInformation().getCurrentTerm());

        // At this point member 1 and 3 actors have captured the RequestVote messages. First
        // forward the RequestVote message to member 1's behavior. Since the RequestVote term
        // is greater than member 1's term, member 1 should switch to Follower without replying
        // to RequestVote and update its term to 2.

        member1Actor.underlyingActor().clearDropMessagesToBehavior();
        member1Actor.underlyingActor().expectBehaviorStateChange();
        member1Actor.underlyingActor().forwardCapturedMessageToBehavior(RequestVote.class, member2Actor);
        member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        member1Actor.underlyingActor().waitForBehaviorStateChange();
        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);

        // Now forward member 3's captured RequestVote message to its behavior. Since member 3 is
        // already a Follower, it should update its term to 2 and send a RequestVoteReply back to
        // member 2 granting the vote b/c the RequestVote's term, lastLogTerm, and lastLogIndex
        // should satisfy the criteria for granting the vote. However, we'll delay sending the
        // RequestVoteReply to member 2's behavior to simulate network latency.

        member2Actor.underlyingActor().dropMessagesToBehavior(RequestVoteReply.class);

        member3Actor.underlyingActor().clearDropMessagesToBehavior();
        member3Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
        member3Actor.underlyingActor().forwardCapturedMessageToBehavior(RequestVote.class, member2Actor);
        member3Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        verifyBehaviorState("member 3", member3Actor, RaftState.Follower);

        assertEquals("member 1 election term", 2, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 2, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 2, member3Context.getTermInformation().getCurrentTerm());

        // Send ElectionTimeout to member 3 to simulate missing heartbeat from a Leader. member 3
        // should switch to Candidate and send out RequestVote messages. member 1 should grant the
        // vote and send a reply. After receiving the RequestVoteReply, member 3 should switch to leader.

        member2Actor.underlyingActor().expectBehaviorStateChange();
        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().expectMessageClass(RequestVoteReply.class, 1);
        member3Actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, 2);

        member3Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member3Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member3Actor.underlyingActor().getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member3Context.getTermInformation().getCurrentTerm(), requestVoteReply.getTerm());
        assertEquals("isVoteGranted", true, requestVoteReply.isVoteGranted());

        verifyBehaviorState("member 3", member3Actor, RaftState.Leader);

        // member 2 should've switched to Follower as member 3's RequestVote term (3) was greater
        // than member 2's term (2).

        member2Actor.underlyingActor().waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);

        // The switch to leader should cause an immediate AppendEntries heartbeat from member 3.

        member3Actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 3, member3Context.getTermInformation().getCurrentTerm());

        // Now forward the original delayed RequestVoteReply from member 3 to member 2 that granted
        // the vote. Since member 2 is now a Follower, the RequestVoteReply should be ignored.

        member2Actor.underlyingActor().clearDropMessagesToBehavior();
        member2Actor.underlyingActor().forwardCapturedMessageToBehavior(RequestVoteReply.class, member3Actor);

        member2Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Leader);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 3, member3Context.getTermInformation().getCurrentTerm());

        testLog.info("testDelayedMessagesScenario done");
    }

    @Test
    public void testPartitionedLeadersScenario() throws Exception {
        testLog.info("Starting testPartitionedLeadersScenario");

        TestActorRef<MemberActor> member1Actor = newMemberActor("member1");
        TestActorRef<MemberActor> member2Actor = newMemberActor("member2");
        TestActorRef<MemberActor> member3Actor = newMemberActor("member3");

        // Create member 2's behavior initially as Follower

        MockRaftActorContext member2Context = newRaftActorContext("member2", member2Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member2ConfigParams = newConfigParams();
        member2Context.setConfigParams(member2ConfigParams);

        Follower member2Behavior = new Follower(member2Context);
        member2Actor.underlyingActor().behavior = member2Behavior;

        // Create member 3's behavior initially as Follower

        MockRaftActorContext member3Context = newRaftActorContext("member3", member3Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member2", member2Actor.path().toString()).build());

        DefaultConfigParamsImpl member3ConfigParams = newConfigParams();
        member3Context.setConfigParams(member3ConfigParams);

        Follower member3Behavior = new Follower(member3Context);
        member3Actor.underlyingActor().behavior = member3Behavior;

        // Create member 1's behavior initially as Leader

        MockRaftActorContext member1Context = newRaftActorContext("member1", member1Actor,
                ImmutableMap.<String,String>builder().
                    put("member2", member2Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member1ConfigParams = newConfigParams();
        member1Context.setConfigParams(member1ConfigParams);

        initializeLeaderBehavior(member1Actor, member1Context, 2);

        member2Actor.underlyingActor().clear();
        member3Actor.underlyingActor().clear();

        // Send ElectionTimeout to member 2 to simulate no heartbeat from the Leader (member 1).
        // member 2 should switch to Candidate, start new term 2 and send out RequestVote messages.
        // member 1 will switch to Follower b/c its term is less than the RequestVote term, also it
        // won't send back a reply. member 3 will drop the message (ie won't forward it to its behavior) to
        // simulate loss of network connectivity between member 2 and 3.

        member1Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);

        member2Actor.underlyingActor().expectBehaviorStateChange();

        member3Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class);

        member2Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member3Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        // member 1 should switch to Follower as the RequestVote term is greater than its term. It
        // won't send back a RequestVoteReply in this case.

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);

        // member 2 should switch to Candidate since member 1 didn't reply.

        member2Actor.underlyingActor().waitForBehaviorStateChange();
        verifyBehaviorState("member 2", member2Actor, RaftState.Candidate);

        assertEquals("member 1 election term", 2, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 2, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 1, member3Context.getTermInformation().getCurrentTerm());

        // Send ElectionTimeout to member 3 to simulate no heartbeat from the Leader (member 1).
        // member 2 should switch to Candidate and send out RequestVote messages. member 1 will reply and
        // grant the vote but member 2 will drop the message to simulate loss of network connectivity.

        member1Actor.underlyingActor().clear();
        member1Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
        member1Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        member2Actor.underlyingActor().clear();
        member2Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class);
        member2Actor.underlyingActor().dropMessagesToBehavior(AppendEntries.class);

        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().expectMessageClass(RequestVoteReply.class, 1);
        member3Actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, 1);

        member3Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member2Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member3Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member3Actor.underlyingActor().getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member3Context.getTermInformation().getCurrentTerm(), requestVoteReply.getTerm());
        assertEquals("isVoteGranted", true, requestVoteReply.isVoteGranted());

        // when member 3 switches to Leader it will immediately send out heartbeat AppendEntries to
        // the followers. Wait for AppendEntries to member 1 and its AppendEntriesReply. The
        // AppendEntries message to member 2 is dropped.

        member1Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);
        member2Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);
        member3Actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Candidate);
        verifyBehaviorState("member 3", member3Actor, RaftState.Leader);

        assertEquals("member 1 election term", 2, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 2, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 2, member3Context.getTermInformation().getCurrentTerm());

        // member 2 is partitioned from the Leader (member 3) and hasn't received any messages. It
        // would get another ElectionTimeout so simulate that. member 1 should send back a reply
        // granting the vote. Messages (RequestVote and AppendEntries) from member 2 to member 3
        // are dropped to simulate loss of network connectivity. Note member 2 will increment its
        // election term to 3.

        member1Actor.underlyingActor().clear();
        member1Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        member2Actor.underlyingActor().clear();
        member2Actor.underlyingActor().expectMessageClass(RequestVoteReply.class, 1);
        member2Actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, 1);

        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().dropMessagesToBehavior(AppendEntries.class);
        member3Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class);

        member2Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member2Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

        requestVoteReply = member2Actor.underlyingActor().getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member2Context.getTermInformation().getCurrentTerm(), requestVoteReply.getTerm());
        assertEquals("isVoteGranted", true, requestVoteReply.isVoteGranted());

        member3Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        member1Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);
        member3Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);
        member2Actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);

        // We end up with 2 partitioned leaders both leading member 1. The term for member 1 and 3
        // is 3 and member 3's term is 2.

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftState.Leader);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 2, member3Context.getTermInformation().getCurrentTerm());

        // Re-establish connectivity between member 2 and 3, ie stop dropping messages between
        // the 2. Send heartbeats (AppendEntries) from member 3. Both member 1 and 2 should send back
        // an unsuccessful AppendEntriesReply b/c their term (3) is greater than member 3's term (2).
        // This should cause member 3 to switch to Follower.

        RaftActorBehavior savedMember1Behavior = member1Actor.underlyingActor().behavior;
        RaftActorBehavior savedMember2Behavior = member2Actor.underlyingActor().behavior;
        RaftActorBehavior savedMember3Behavior = member3Actor.underlyingActor().behavior;
        long savedMember3Term = member3Context.getTermInformation().getCurrentTerm();
        String savedMember3VoterFor = member3Context.getTermInformation().getVotedFor();

        member1Actor.underlyingActor().clear();
        member1Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        member2Actor.underlyingActor().clear();
        member2Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, 1);

        sendHeartbeat(member3Actor);

        member3Actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);

        AppendEntriesReply appendEntriesReply = member3Actor.underlyingActor().
                getCapturedMessage(AppendEntriesReply.class);
        assertEquals("isSuccess", false, appendEntriesReply.isSuccess());
        assertEquals("getTerm", 3, appendEntriesReply.getTerm());

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftState.Follower);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 3, member3Context.getTermInformation().getCurrentTerm());

        // Revert back to the partitioned leaders state to test the other sequence where member 2
        // sends heartbeats first before member 3. member 1 should return a successful
        // AppendEntriesReply b/c his term matches member 2's. member 3 should switch to Follower
        // as his term is less than member 2's.

        member1Actor.underlyingActor().behavior = savedMember1Behavior;
        member2Actor.underlyingActor().behavior = savedMember2Behavior;
        member3Actor.underlyingActor().behavior = savedMember3Behavior;

        member3Context.getTermInformation().update(savedMember3Term, savedMember3VoterFor);

        member1Actor.underlyingActor().clear();
        member1Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        member2Actor.underlyingActor().clear();
        member2Actor.underlyingActor().expectMessageClass(AppendEntriesReply.class, 1);

        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().expectMessageClass(AppendEntries.class, 1);

        sendHeartbeat(member2Actor);

        member1Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);
        member3Actor.underlyingActor().waitForExpectedMessages(AppendEntries.class);

        member2Actor.underlyingActor().waitForExpectedMessages(AppendEntriesReply.class);

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Leader);
        verifyBehaviorState("member 3", member3Actor, RaftState.Follower);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", 3, member3Context.getTermInformation().getCurrentTerm());

        testLog.info("testPartitionedLeadersScenario done");
    }

    @Test
    public void testPartitionedCandidateOnStartupScenario() throws Exception {
        testLog.info("Starting testPartitionedCandidateOnStartupScenario");

        TestActorRef<MemberActor> member1Actor = newMemberActor("member1") ;
        TestActorRef<MemberActor> member2Actor = newMemberActor("member2");
        TestActorRef<MemberActor> member3Actor = newMemberActor("member3");

        // Create member 2's behavior as Follower.

        MockRaftActorContext member2Context = newRaftActorContext("member2", member2Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member2ConfigParams = newConfigParams();
        member2Context.setConfigParams(member2ConfigParams);

        Follower member2Behavior = new Follower(member2Context);
        member2Actor.underlyingActor().behavior = member2Behavior;

        // Create member 1's behavior as Leader.

        MockRaftActorContext member1Context = newRaftActorContext("member1", member1Actor,
                ImmutableMap.<String,String>builder().
                    put("member2", member2Actor.path().toString()).
                    put("member3", member3Actor.path().toString()).build());

        DefaultConfigParamsImpl member1ConfigParams = newConfigParams();
        member1Context.setConfigParams(member1ConfigParams);

        initializeLeaderBehavior(member1Actor, member1Context, 1);

        member2Actor.underlyingActor().clear();
        member3Actor.underlyingActor().clear();

        // Initialize the ReplicatedLog and election term info for member 1 and 2. The current term
        // will be 3 and the last term will be 2.

        SimpleReplicatedLog replicatedLog = new SimpleReplicatedLog();
        replicatedLog.append(new MockReplicatedLogEntry(2, 1, new MockPayload("")));
        replicatedLog.append(new MockReplicatedLogEntry(3, 1, new MockPayload("")));

        member1Context.setReplicatedLog(replicatedLog);
        member1Context.getTermInformation().update(3, "");

        member2Context.setReplicatedLog(replicatedLog);
        member2Context.getTermInformation().update(3, member1Context.getId());

        // Create member 3's behavior initially as a Candidate.

        MockRaftActorContext member3Context = newRaftActorContext("member3", member3Actor,
                ImmutableMap.<String,String>builder().
                    put("member1", member1Actor.path().toString()).
                    put("member2", member2Actor.path().toString()).build());

        DefaultConfigParamsImpl member3ConfigParams = newConfigParams();
        member3Context.setConfigParams(member3ConfigParams);

        // Initialize the ReplicatedLog and election term info for Candidate member 3. The current term
        // will be 2 and the last term will be 1 so it is behind the leader's log.

        SimpleReplicatedLog candidateReplicatedLog = new SimpleReplicatedLog();
        candidateReplicatedLog.append(new MockReplicatedLogEntry(1, 1, new MockPayload("")));
        candidateReplicatedLog.append(new MockReplicatedLogEntry(2, 1, new MockPayload("")));

        member3Context.setReplicatedLog(candidateReplicatedLog);
        member3Context.getTermInformation().update(2, member1Context.getId());

        // The member 3 Candidate will start a new term and send RequestVotes. However it will be
        // partitioned from the cluster by having member 1 and 2 drop its RequestVote messages.

        int numCandidateElections = 5;
        long candidateElectionTerm = member3Context.getTermInformation().getCurrentTerm() + numCandidateElections;

        member1Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class, numCandidateElections);

        member2Actor.underlyingActor().dropMessagesToBehavior(RequestVote.class, numCandidateElections);

        Candidate member3Behavior = new Candidate(member3Context);
        member3Actor.underlyingActor().behavior = member3Behavior;

        // Send several additional ElectionTimeouts to Candidate member 3. Each ElectionTimeout will
        // start a new term so Candidate member 3's current term will be greater than the leader's
        // current term.

        for(int i = 0; i < numCandidateElections - 1; i++) {
            member3Actor.tell(new ElectionTimeout(), ActorRef.noSender());
        }

        member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member2Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        verifyBehaviorState("member 1", member1Actor, RaftState.Leader);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Candidate);

        assertEquals("member 1 election term", 3, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", 3, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", candidateElectionTerm,
                member3Context.getTermInformation().getCurrentTerm());

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
        for(int i = 0; i < 2; i++) {
            member1Actor.underlyingActor().clear();
            member1Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
            member2Actor.underlyingActor().clear();
            member2Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
            member3Actor.underlyingActor().clear();
            member3Actor.underlyingActor().expectMessageClass(RequestVoteReply.class, 1);

            member3Actor.tell(new ElectionTimeout(), ActorRef.noSender());

            member1Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
            member2Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

            member3Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

            RequestVoteReply requestVoteReply = member3Actor.underlyingActor().getCapturedMessage(RequestVoteReply.class);
            assertEquals("getTerm", member3Context.getTermInformation().getCurrentTerm(), requestVoteReply.getTerm());
            assertEquals("isVoteGranted", false, requestVoteReply.isVoteGranted());
        }

        verifyBehaviorState("member 1", member1Actor, RaftState.Follower);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Candidate);

        // Even though member 3 didn't get voted for, member 1 and 2 should have updated their term
        // to member 3's.

        assertEquals("member 1 election term", candidateElectionTerm,
                member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", candidateElectionTerm,
                member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", candidateElectionTerm,
                member3Context.getTermInformation().getCurrentTerm());

        // At this point we have no leader. Candidate member 3 would continue to start new elections
        // but wouldn't be granted a vote. One of the 2 followers would eventually time out from
        // not having received a heartbeat from a leader and switch to candidate and start a new
        // election. We'll simulate that here by sending an ElectionTimeout to member 1.

        member1Actor.underlyingActor().clear();
        member1Actor.underlyingActor().expectMessageClass(RequestVoteReply.class, 1);
        member2Actor.underlyingActor().clear();
        member2Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
        member3Actor.underlyingActor().clear();
        member3Actor.underlyingActor().expectMessageClass(RequestVote.class, 1);
        member3Actor.underlyingActor().expectBehaviorStateChange();

        member1Actor.tell(new ElectionTimeout(), ActorRef.noSender());

        member2Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);
        member3Actor.underlyingActor().waitForExpectedMessages(RequestVote.class);

        // The RequestVoteReply should come from Follower member 2 and the vote should be granted
        // since member 2's last term and index matches member 1's.

        member1Actor.underlyingActor().waitForExpectedMessages(RequestVoteReply.class);

        RequestVoteReply requestVoteReply = member1Actor.underlyingActor().getCapturedMessage(RequestVoteReply.class);
        assertEquals("getTerm", member1Context.getTermInformation().getCurrentTerm(), requestVoteReply.getTerm());
        assertEquals("isVoteGranted", true, requestVoteReply.isVoteGranted());

        // Candidate member 3 should change to follower as its term should be less than the
        // RequestVote term (member 1 started a new term higher than the other member's terms).

        member3Actor.underlyingActor().waitForBehaviorStateChange();

        verifyBehaviorState("member 1", member1Actor, RaftState.Leader);
        verifyBehaviorState("member 2", member2Actor, RaftState.Follower);
        verifyBehaviorState("member 3", member3Actor, RaftState.Follower);

        // newTerm should be 10.

        long newTerm = candidateElectionTerm + 1;
        assertEquals("member 1 election term", newTerm, member1Context.getTermInformation().getCurrentTerm());
        assertEquals("member 2 election term", newTerm, member2Context.getTermInformation().getCurrentTerm());
        assertEquals("member 3 election term", newTerm, member3Context.getTermInformation().getCurrentTerm());

        testLog.info("testPartitionedCandidateOnStartupScenario done");
    }
}
