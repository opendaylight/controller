/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.ForwardMessageToBehaviorActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for RaftActorServerConfigurationSupport.
 *
 * @author Thomas Pantelis
 */
public class RaftActorServerConfigurationSupportTest extends AbstractActorTest {
    static final String LEADER_ID = "leader";
    static final String FOLLOWER_ID = "follower";
    static final String NEW_SERVER_ID = "new-server";
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupportTest.class);
    private static final DataPersistenceProvider NO_PERSISTENCE = new NonPersistentDataProvider();

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final TestActorRef<ForwardMessageToBehaviorActor> followerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(FOLLOWER_ID));

    private final TestActorRef<ForwardMessageToBehaviorActor> newServerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(NEW_SERVER_ID));

    private RaftActorContext newServerActorContext;
    private final JavaTestKit testKit = new JavaTestKit(getSystem());

    @Before
    public void setup() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        newServerActorContext = new RaftActorContextImpl(newServerActor, newServerActor.underlyingActor().getContext(),
                NEW_SERVER_ID, new ElectionTermImpl(NO_PERSISTENCE, NEW_SERVER_ID, LOG), -1, -1,
                Maps.<String, String>newHashMap(), configParams, NO_PERSISTENCE, LOG);
        newServerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
    }

    @After
    public void tearDown() throws Exception {
        actorFactory.close();
    }

    @Test
    public void testAddServerWithFollower() throws Exception {
        RaftActorContext followerActorContext = newFollowerContext(FOLLOWER_ID, followerActor);
        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(
                0, 3, 1).build());
        followerActorContext.setCommitIndex(3);
        followerActorContext.setLastApplied(3);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);

        Follower newServer = new Follower(newServerActorContext);
        newServerActor.underlyingActor().setBehavior(newServer);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActor.path().toString()),
                        followerActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        // Expect initial heartbeat from the leader.
        expectFirstMatching(followerActor, AppendEntries.class);
        clearMessages(followerActor);

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newServerActor.path().toString(), true), testKit.getRef());

        // leader should install snapshot - capture and verify ApplySnapshot contents
//        ApplySnapshot applySnapshot = expectFirstMatching(followerActor, ApplySnapshot.class);
//        List<Object> snapshotState = (List<Object>) MockRaftActor.toObject(applySnapshot.getSnapshot().getState());
//        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        // leader should replicate new server config to both followers
//         expectFirstMatching(followerActor, AppendEntries.class);
//         expectFirstMatching(newServerActor, AppendEntries.class);

        // verify ServerConfigurationPayload entry in leader's log
//        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
//        assertEquals("Leader journal log size", 4, leaderActorContext.getReplicatedLog().size());
//        assertEquals("Leader journal last index", 3, leaderActorContext.getReplicatedLog().lastIndex());
//        ReplicatedLogEntry logEntry = leaderActorContext.getReplicatedLog().get(
//                leaderActorContext.getReplicatedLog().lastIndex());
        // verify logEntry contents

        // Also verify ServerConfigurationPayload entry in both followers

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());
    }

    @Test
    public void testAddServerWithNoLeader() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> noLeaderActor = actorFactory.createTestActor(
                MockRaftActor.props(LEADER_ID, ImmutableMap.<String,String>of(FOLLOWER_ID, followerActor.path().toString()),
                        Optional.<ConfigParams>of(configParams), NO_PERSISTENCE).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        noLeaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        noLeaderActor.tell(new AddServer(NEW_SERVER_ID, newServerActor.path().toString(), true), testKit.getRef());
        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());
    }

    @Test
    public void testAddServerForwardedToLeader() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.props(FOLLOWER_ID, ImmutableMap.<String,String>of(LEADER_ID, leaderActor.path().toString()),
                        Optional.<ConfigParams>of(configParams), NO_PERSISTENCE).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, (short)0), leaderActor);

        followerRaftActor.tell(new AddServer(NEW_SERVER_ID, newServerActor.path().toString(), true), testKit.getRef());
        expectFirstMatching(leaderActor, AddServer.class);
    }

    private RaftActorContext newFollowerContext(String id, TestActorRef<? extends UntypedActor> actor) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        RaftActorContext followerActorContext = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                id, new ElectionTermImpl(NO_PERSISTENCE, id, LOG), -1, -1,
                ImmutableMap.of(LEADER_ID, ""), configParams, NO_PERSISTENCE, LOG);

        return followerActorContext;
    }

    public static class MockLeaderRaftActor extends MockRaftActor {
        public MockLeaderRaftActor(Map<String, String> peerAddresses, ConfigParams config,
                RaftActorContext fromContext) {
            super(LEADER_ID, peerAddresses, Optional.of(config), NO_PERSISTENCE);

            RaftActorContext context = getRaftActorContext();
            for(int i = 0; i < fromContext.getReplicatedLog().size(); i++) {
                ReplicatedLogEntry entry = fromContext.getReplicatedLog().get(i);
                getState().add(entry.getData());
                context.getReplicatedLog().append(entry);
            }

            context.setCommitIndex(fromContext.getCommitIndex());
            context.setLastApplied(fromContext.getLastApplied());
        }

        @Override
        protected void initializeBehavior() {
            changeCurrentBehavior(new Leader(getRaftActorContext()));
            initializeBehaviorComplete.countDown();
        }

        @Override
        public void createSnapshot(ActorRef actorRef) {
            try {
                actorRef.tell(new CaptureSnapshotReply(RaftActorTest.fromObject(getState()).toByteArray()), actorRef);
            } catch (Exception e) {
                LOG.error("createSnapshot failed", e);
            }
        }

        static Props props(Map<String, String> peerAddresses, RaftActorContext fromContext) {
            DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
            configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
            configParams.setElectionTimeoutFactor(100000);
            return Props.create(MockLeaderRaftActor.class, peerAddresses, configParams, fromContext);
        }
    }
}
