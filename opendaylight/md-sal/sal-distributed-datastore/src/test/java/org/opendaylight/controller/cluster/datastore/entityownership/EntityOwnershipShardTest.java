/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for EntityOwnershipShard.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipShardTest extends AbstractEntityOwnershipTest {
    private static final String ENTITY_TYPE = "test type";
    private static final YangInstanceIdentifier ENTITY_ID1 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity2"));
    private static final YangInstanceIdentifier ENTITY_ID3 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity3"));
    private static final YangInstanceIdentifier ENTITY_ID4 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity4"));
    private static final YangInstanceIdentifier ENTITY_ID5 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity5"));
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();
    private static final String LOCAL_MEMBER_NAME = "local-member-1";
    private static final String PEER_MEMBER_1_NAME = "peer-member-1";
    private static final String PEER_MEMBER_2_NAME = "peer-member-2";

    private Builder dataStoreContextBuilder = DatastoreContext.newBuilder().persistent(false);
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    //@Test
    public void testOnRegisterCandidateLocal() throws Exception {
        testLog.info("testOnRegisterCandidateLocal starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newLocalShardProps());

        ShardTestKit.waitUntilLeader(shard);

        YangInstanceIdentifier entityId = ENTITY_ID1;
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        testLog.info("testOnRegisterCandidateLocal ending");
    }

    //@Test
    public void testOnRegisterCandidateLocalWithNoInitialLeader() throws Exception {
        testLog.info("testOnRegisterCandidateLocalWithNoInitialLeader starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId = newShardId(PEER_MEMBER_1_NAME);

        TestActorRef<TestEntityOwnershipShard> peer = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId, peerMap(leaderId.toString()), PEER_MEMBER_1_NAME)), peerId.toString());
        TestEntityOwnershipShard peerShard = peer.underlyingActor();
        peerShard.startDroppingMessagesOfType(RequestVote.class);
        peerShard.startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId.toString()), LOCAL_MEMBER_NAME), leaderId.toString());

        YangInstanceIdentifier entityId = ENTITY_ID1;
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Now allow RequestVotes to the peer so the shard becomes the leader. This should retry the commit.
        peerShard.stopDroppingMessagesOfType(RequestVote.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        testLog.info("testOnRegisterCandidateLocalWithNoInitialLeader ending");
    }

    //@Test
    public void testOnRegisterCandidateLocalWithNoInitialConsensus() throws Exception {
        testLog.info("testOnRegisterCandidateLocalWithNoInitialConsensus starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2).
                shardTransactionCommitTimeoutInSeconds(1);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId = newShardId(PEER_MEMBER_1_NAME);

        TestActorRef<TestEntityOwnershipShard> peer = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId, peerMap(leaderId.toString()), PEER_MEMBER_1_NAME)), peerId.toString());
        TestEntityOwnershipShard peerShard = peer.underlyingActor();
        peerShard.startDroppingMessagesOfType(ElectionTimeout.class);

        // Drop AppendEntries so consensus isn't reached.
        peerShard.startDroppingMessagesOfType(AppendEntries.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId.toString()), LOCAL_MEMBER_NAME), leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        YangInstanceIdentifier entityId = ENTITY_ID1;
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        leader.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Wait enough time for the commit to timeout.
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        // Resume AppendEntries - the follower should ack the commit which should then result in the candidate
        // write being applied to the state.
        peerShard.stopDroppingMessagesOfType(AppendEntries.class);

        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        testLog.info("testOnRegisterCandidateLocalWithNoInitialConsensus ending");
    }

    //@Test
    public void testOnRegisterCandidateLocalWithIsolatedLeader() throws Exception {
        testLog.info("testOnRegisterCandidateLocalWithIsolatedLeader starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2).
                shardIsolatedLeaderCheckIntervalInMillis(50);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId = newShardId(PEER_MEMBER_1_NAME);

        TestActorRef<TestEntityOwnershipShard> peer = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId, peerMap(leaderId.toString()), PEER_MEMBER_1_NAME)), peerId.toString());
        TestEntityOwnershipShard peerShard = peer.underlyingActor();
        peerShard.startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId.toString()), LOCAL_MEMBER_NAME));

        ShardTestKit.waitUntilLeader(leader);

        // Drop AppendEntries and wait enough time for the shard to switch to IsolatedLeader.
        peerShard.startDroppingMessagesOfType(AppendEntries.class);
        verifyRaftState(leader, state ->
                assertEquals("getRaftState", RaftState.IsolatedLeader.toString(), state.getRaftState()));

        YangInstanceIdentifier entityId = ENTITY_ID1;
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        leader.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Resume AppendEntries - the candidate write should now be committed.
        peerShard.stopDroppingMessagesOfType(AppendEntries.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        testLog.info("testOnRegisterCandidateLocalWithIsolatedLeader ending");
    }

    //@Test
    public void testOnRegisterCandidateLocalWithRemoteLeader() throws Exception {
        testLog.info("testOnRegisterCandidateLocalWithRemoteLeader starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2).
                shardBatchedModificationCount(5);

        ShardIdentifier leaderId = newShardId(PEER_MEMBER_1_NAME);
        ShardIdentifier localId = newShardId(LOCAL_MEMBER_NAME);
        TestActorRef<TestEntityOwnershipShard> leader = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(leaderId, peerMap(localId.toString()), PEER_MEMBER_1_NAME),
                actorFactory.createTestActor(MessageCollectorActor.props())), leaderId.toString());
        TestEntityOwnershipShard leaderShard = leader.underlyingActor();

        TestActorRef<TestEntityOwnershipShard> local = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(localId, peerMap(leaderId.toString()),LOCAL_MEMBER_NAME)), localId.toString());
        local.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        local.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Test with initial commit timeout and subsequent retry.

        local.tell(dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1).build(), ActorRef.noSender());
        leaderShard.startDroppingMessagesOfType(BatchedModifications.class);

        local.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        MessageCollectorActor.expectFirstMatching(leaderShard.collectorActor(), BatchedModifications.class);

        // Send a bunch of registration messages quickly and verify.

        leaderShard.stopDroppingMessagesOfType(BatchedModifications.class);
        MessageCollectorActor.clearMessages(leaderShard.collectorActor());

        int max = 100;
        List<YangInstanceIdentifier> entityIds = new ArrayList<>();
        for(int i = 1; i <= max; i++) {
            YangInstanceIdentifier id = YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "test" + i));
            entityIds.add(id);
            local.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, id)), kit.getRef());
        }

        for(int i = 0; i < max; i++) {
            verifyCommittedEntityCandidate(local, ENTITY_TYPE, entityIds.get(i), LOCAL_MEMBER_NAME);
        }

        testLog.info("testOnRegisterCandidateLocalWithRemoteLeader ending");
    }

    //@Test
    public void testOnUnregisterCandidateLocal() throws Exception {
        testLog.info("testOnUnregisterCandidateLocal starting");

        ShardTestKit kit = new ShardTestKit(getSystem());
        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newLocalShardProps());
        ShardTestKit.waitUntilLeader(shard);

        DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);

        // Register

        shard.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Unregister

        shard.tell(new UnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, "");

        // Register again

        shard.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        testLog.info("testOnUnregisterCandidateLocal ending");
    }

    //@Test
    public void testOwnershipChanges() throws Exception {
        testLog.info("testOwnershipChanges starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId1 = newShardId(PEER_MEMBER_1_NAME);
        ShardIdentifier peerId2 = newShardId(PEER_MEMBER_2_NAME);

        TestActorRef<TestEntityOwnershipShard> peer1 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId1, peerMap(leaderId.toString(), peerId2.toString()), PEER_MEMBER_1_NAME)),
                    peerId1.toString());
        peer1.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<TestEntityOwnershipShard> peer2 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId2, peerMap(leaderId.toString(), peerId1.toString()), PEER_MEMBER_2_NAME)),
                    peerId2.toString());
        peer2.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId1.toString(), peerId2.toString()), LOCAL_MEMBER_NAME),
                    leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);

        // Add a remote candidate

        peer1.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);

        // Register local

        leader.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the remote candidate becomes owner

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);

        // Add another remote candidate and verify ownership doesn't change

        peer2.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_2_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);

        // Remove the second remote candidate and verify ownership doesn't change

        peer2.tell(new UnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyEntityCandidateRemoved(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_2_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);

        // Remove the first remote candidate and verify the local candidate becomes owner

        peer1.tell(new UnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyEntityCandidateRemoved(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);

        // Add the second remote candidate back and verify ownership doesn't change

        peer2.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_2_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);

        // Unregister the local candidate and verify the second remote candidate becomes owner

        leader.tell(new UnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyEntityCandidateRemoved(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_2_NAME);

        testLog.info("testOwnershipChanges ending");
    }

    @Test
    public void testOwnerChangesOnPeerAvailabilityChanges() throws Exception {
        testLog.info("testOwnerChangesOnPeerAvailabilityChanges starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(4).
                shardIsolatedLeaderCheckIntervalInMillis(100000);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId1 = newShardId(PEER_MEMBER_1_NAME);
        ShardIdentifier peerId2 = newShardId(PEER_MEMBER_2_NAME);

        TestActorRef<TestEntityOwnershipShard> peer1 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId1, peerMap(leaderId.toString(), peerId2.toString()), PEER_MEMBER_1_NAME)),
                    peerId1.toString());
        peer1.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<TestEntityOwnershipShard> peer2 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId2, peerMap(leaderId.toString(), peerId1.toString()), PEER_MEMBER_2_NAME)),
                    peerId2.toString());
        peer2.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId1.toString(), peerId2.toString()), LOCAL_MEMBER_NAME),
                    leaderId.toString());

        verifyRaftState(leader, state ->
                assertEquals("getRaftState", RaftState.Leader.toString(), state.getRaftState()));

        // Send PeerDown and PeerUp with no entities

        leader.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        // Add candidates for entity1 with the local leader as the owner

        leader.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);

        peer1.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Add candidates for entity2 with peerMember2 as the owner

        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);

        peer1.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);

        // Add candidates for entity3 with peerMember2 as the owner.

        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID3)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);

        leader.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID3)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);

        peer1.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID3)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);

        // Add only candidate peerMember2 for entity4.

        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID4)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID4, PEER_MEMBER_2_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, PEER_MEMBER_2_NAME);

        // Add only candidate peerMember1 for entity5.

        peer1.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID5)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID5, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID5, PEER_MEMBER_1_NAME);

        // Kill peerMember2 and send PeerDown - the entities (2, 3, 4) owned by peerMember2 should get a new
        // owner selected

        kit.watch(peer2);
        peer2.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);
        kit.unwatch(peer2);

        leader.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        // Send PeerDown again - should be noop
        leader.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        peer1.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        // no other candidates for entity4 so peerMember2 should remain owner.
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, PEER_MEMBER_2_NAME);

        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID4, PEER_MEMBER_2_NAME);

        // Reinstate peerMember2

        peer2 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId2, peerMap(leaderId.toString(), peerId1.toString()), PEER_MEMBER_2_NAME)),
                    peerId2.toString());
        peer2.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        // Send PeerUp again - should be noop
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        peer1.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        // peerMember2's candidates should be removed on startup.
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID4, PEER_MEMBER_2_NAME);

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, "");

        // Add back candidate peerMember2 for entities 1, 2, & 3.

        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        peer2.tell(new RegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID3)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyCommittedEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID4, "");

        // Kill peerMember1 and send PeerDown - entity 2 should get a new owner selected

        kit.watch(peer1);
        peer1.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);
        kit.unwatch(peer1);
        leader.tell(new PeerDown(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);

        // Verify the reinstated peerMember2 is fully synced.

        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID4, "");

        // Reinstate peerMember1 and verify no owner changes

        peer1 = actorFactory.createTestActor(TestEntityOwnershipShard.props(newShardBuilder(
                peerId1, peerMap(leaderId.toString(), peerId2.toString()), PEER_MEMBER_1_NAME)), peerId1.toString());
        peer1.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);
        leader.tell(new PeerUp(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, "");

        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_1_NAME);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_1_NAME);

        verifyNoEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_1_NAME);
        verifyNoEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_1_NAME);
        verifyNoEntityCandidate(peer2, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_1_NAME);

        // Verify the reinstated peerMember1 is fully synced.

        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID4, "");

        // Kill the local leader and elect peer2 the leader. This should cause a new owner to be selected for
        // the entities (1 and 3) previously owned by the local leader member.

        peer2.tell(new PeerAddressResolved(peerId1.toString(), peer1.path().toString()), ActorRef.noSender());
        peer2.tell(new PeerUp(leaderId.getMemberName(), leaderId.toString()), ActorRef.noSender());
        peer2.tell(new PeerUp(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        kit.watch(leader);
        leader.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);
        kit.unwatch(leader);
        peer2.tell(new PeerDown(leaderId.getMemberName(), leaderId.toString()), ActorRef.noSender());
        peer2.tell(TimeoutNow.INSTANCE, peer2);

        verifyRaftState(peer2, state ->
                assertEquals("getRaftState", RaftState.Leader.toString(), state.getRaftState()));

        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID1, PEER_MEMBER_2_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID2, PEER_MEMBER_2_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID3, PEER_MEMBER_2_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID4, "");

        testLog.info("testOwnerChangesOnPeerAvailabilityChanges ending");
    }

    //@Test
    public void testLeaderIsolation() throws Exception {
        testLog.info("testLeaderIsolation starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId1 = newShardId(PEER_MEMBER_1_NAME);
        ShardIdentifier peerId2 = newShardId(PEER_MEMBER_2_NAME);

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(4).
            shardIsolatedLeaderCheckIntervalInMillis(100000);

        TestActorRef<TestEntityOwnershipShard> peer1 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId1, peerMap(leaderId.toString(), peerId2.toString()), PEER_MEMBER_1_NAME)),
                    peerId1.toString());
        peer1.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<TestEntityOwnershipShard> peer2 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId2, peerMap(leaderId.toString(), peerId1.toString()), PEER_MEMBER_2_NAME)),
                    peerId2.toString());
        peer2.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        dataStoreContextBuilder = DatastoreContext.newBuilderFrom(dataStoreContextBuilder.build()).
                shardIsolatedLeaderCheckIntervalInMillis(500);

        TestActorRef<TestEntityOwnershipShard> leader = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(leaderId, peerMap(peerId1.toString(), peerId2.toString()), LOCAL_MEMBER_NAME)),
                    leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        // Add entity1 candidates for all members with the leader as the owner

        DOMEntity entity1 = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        leader.tell(new RegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity1.getType(), entity1.getIdentifier(), LOCAL_MEMBER_NAME);

        peer1.tell(new RegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity1.getType(), entity1.getIdentifier(), PEER_MEMBER_1_NAME);

        peer2.tell(new RegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity1.getType(), entity1.getIdentifier(), PEER_MEMBER_2_NAME);

        verifyOwner(leader, entity1.getType(), entity1.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(peer1, entity1.getType(), entity1.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(peer2, entity1.getType(), entity1.getIdentifier(), LOCAL_MEMBER_NAME);

        // Add entity2 candidates for all members with peer1 as the owner

        DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, ENTITY_ID2);
        peer1.tell(new RegisterCandidateLocal(entity2), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_1_NAME);

        peer2.tell(new RegisterCandidateLocal(entity2), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_2_NAME);

        leader.tell(new RegisterCandidateLocal(entity2), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity2.getType(), entity2.getIdentifier(), LOCAL_MEMBER_NAME);

        verifyOwner(leader, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyOwner(peer1, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyOwner(peer2, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_1_NAME);

        // Add entity3 candidates for all members with peer2 as the owner

        DOMEntity entity3 = new DOMEntity(ENTITY_TYPE, ENTITY_ID3);
        peer2.tell(new RegisterCandidateLocal(entity3), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_2_NAME);

        leader.tell(new RegisterCandidateLocal(entity3), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity3.getType(), entity3.getIdentifier(), LOCAL_MEMBER_NAME);

        peer1.tell(new RegisterCandidateLocal(entity3), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_1_NAME);

        verifyOwner(leader, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_2_NAME);
        verifyOwner(peer1, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_2_NAME);
        verifyOwner(peer2, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_2_NAME);

        // Add listeners on all members

        DOMEntityOwnershipListener leaderListener = mock(DOMEntityOwnershipListener.class);
        leader.tell(new RegisterListenerLocal(leaderListener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verify(leaderListener, timeout(5000).times(3)).ownershipChanged(or(or(ownershipChange(entity1, false, true, true),
                ownershipChange(entity2, false, false, true)), ownershipChange(entity3, false, false, true)));
        reset(leaderListener);

        DOMEntityOwnershipListener peer1Listener = mock(DOMEntityOwnershipListener.class);
        peer1.tell(new RegisterListenerLocal(peer1Listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verify(peer1Listener, timeout(5000).times(3)).ownershipChanged(or(or(ownershipChange(entity1, false, false, true),
                ownershipChange(entity2, false, true, true)), ownershipChange(entity3, false, false, true)));
        reset(peer1Listener);

        DOMEntityOwnershipListener peer2Listener = mock(DOMEntityOwnershipListener.class);
        peer2.tell(new RegisterListenerLocal(peer2Listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verify(peer2Listener, timeout(5000).times(3)).ownershipChanged(or(or(ownershipChange(entity1, false, false, true),
                ownershipChange(entity2, false, false, true)), ownershipChange(entity3, false, true, true)));
        reset(peer2Listener);

        // Isolate the leader by dropping AppendEntries to the followers and incoming messages from the followers.

        leader.underlyingActor().startDroppingMessagesOfType(RequestVote.class);
        leader.underlyingActor().startDroppingMessagesOfType(AppendEntries.class);

        peer2.underlyingActor().startDroppingMessagesOfType(AppendEntries.class,
                ae -> ae.getLeaderId().equals(leaderId.toString()));
        peer1.underlyingActor().startDroppingMessagesOfType(AppendEntries.class);

        // Make peer1 start an election and become leader by enabling the ElectionTimeout message.

        peer1.underlyingActor().stopDroppingMessagesOfType(ElectionTimeout.class);

        // Send PeerDown to the isolated leader so it tries to re-assign ownership for the entities owned by the
        // isolated peers.

        leader.tell(new PeerDown(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());
        leader.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        verifyRaftState(leader, state ->
                assertEquals("getRaftState", RaftState.IsolatedLeader.toString(), state.getRaftState()));

        // Expect inJeopardy notification on the isolated leader.

        verify(leaderListener, timeout(5000).times(3)).ownershipChanged(or(or(ownershipChange(entity1, true, true, true, true),
                ownershipChange(entity2, false, false, true, true)), ownershipChange(entity3, false, false, true, true)));
        reset(leaderListener);

        verifyRaftState(peer1, state ->
                assertEquals("getRaftState", RaftState.Leader.toString(), state.getRaftState()));

        // Send PeerDown to the new leader peer1 so it re-assigns ownership for the entities owned by the
        // isolated leader.

        peer1.tell(new PeerDown(leaderId.getMemberName(), leaderId.toString()), ActorRef.noSender());

        verifyOwner(peer1, entity1.getType(), entity1.getIdentifier(), PEER_MEMBER_1_NAME);

        verify(peer1Listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, true, true));
        reset(peer1Listener);

        verify(peer2Listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, false, true));
        reset(peer2Listener);

        // Remove the isolation.

        leader.underlyingActor().stopDroppingMessagesOfType(RequestVote.class);
        leader.underlyingActor().stopDroppingMessagesOfType(AppendEntries.class);
        peer2.underlyingActor().stopDroppingMessagesOfType(AppendEntries.class);
        peer1.underlyingActor().stopDroppingMessagesOfType(AppendEntries.class);

        // Previous leader should switch to Follower and send inJeopardy cleared notifications for all entities.

        verifyRaftState(leader, state ->
                assertEquals("getRaftState", RaftState.Follower.toString(), state.getRaftState()));

        verify(leaderListener, timeout(5000).times(3)).ownershipChanged(or(or(ownershipChange(entity1, true, true, true),
                ownershipChange(entity2, false, false, true)), ownershipChange(entity3, false, false, true)));

        verifyOwner(leader, entity1.getType(), entity1.getIdentifier(), PEER_MEMBER_1_NAME);
        verify(leaderListener, timeout(5000)).ownershipChanged(ownershipChange(entity1, true, false, true));

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(leader, entity2.getType(), entity2.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyOwner(leader, entity3.getType(), entity3.getIdentifier(), PEER_MEMBER_2_NAME);

        verifyNoMoreInteractions(leaderListener);
        verifyNoMoreInteractions(peer1Listener);
        verifyNoMoreInteractions(peer2Listener);

        testLog.info("testLeaderIsolation ending");
    }

    //@Test
    public void testListenerRegistration() throws Exception {
        testLog.info("testListenerRegistration starting");

        ShardTestKit kit = new ShardTestKit(getSystem());

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId = newShardId(PEER_MEMBER_1_NAME);

        TestActorRef<TestEntityOwnershipShard> peer = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId, peerMap(leaderId.toString()), PEER_MEMBER_1_NAME)), peerId.toString());
        peer.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId.toString()), LOCAL_MEMBER_NAME), leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        String otherEntityType = "otherEntityType";
        DOMEntity entity1 = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, ENTITY_ID2);
        DOMEntity entity3 = new DOMEntity(ENTITY_TYPE, ENTITY_ID3);
        DOMEntity entity4 = new DOMEntity(otherEntityType, ENTITY_ID3);
        DOMEntityOwnershipListener listener = mock(DOMEntityOwnershipListener.class);

        // Register listener

        leader.tell(new RegisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Register a couple candidates for the desired entity type and verify listener is notified.

        leader.tell(new RegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, true, true));

        leader.tell(new RegisterCandidateLocal(entity2), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity2, false, true, true));
        reset(listener);

        // Register another candidate for another entity type and verify listener is not notified.

        leader.tell(new RegisterCandidateLocal(entity4), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(ownershipChange(entity4));

        // Register remote candidate for entity1

        peer.tell(new RegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, entity1.getIdentifier(), PEER_MEMBER_1_NAME);

        // Unregister the local candidate for entity1 and verify listener is notified

        leader.tell(new UnregisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, true, false, true));
        reset(listener);

        // Unregister the listener, add a candidate for entity3 and verify listener isn't notified

        leader.tell(new UnregisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        leader.tell(new RegisterCandidateLocal(entity3), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyOwner(leader, ENTITY_TYPE, entity3.getIdentifier(), LOCAL_MEMBER_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));

        // Re-register the listener and verify it gets notified of currently owned entities

        reset(listener);

        leader.tell(new RegisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000).times(2)).ownershipChanged(or(ownershipChange(entity2, false, true, true),
                ownershipChange(entity3, false, true, true)));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(ownershipChange(entity4));
        verify(listener, times(1)).ownershipChanged(ownershipChange(entity1));

        testLog.info("testListenerRegistration ending");
    }

    //@Test
    public void testDelayedEntityOwnerSelectionWhenMaxPeerRequestsReceived() throws Exception {
        testLog.info("testDelayedEntityOwnerSelectionWhenMaxPeerRequestsReceived starting");

        ShardTestKit kit = new ShardTestKit(getSystem());
        EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder().
                addStrategy(ENTITY_TYPE, LastCandidateSelectionStrategy.class, 500);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId = newShardId(PEER_MEMBER_1_NAME);

        TestActorRef<TestEntityOwnershipShard> peer = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId, peerMap(leaderId.toString()), PEER_MEMBER_1_NAME)), peerId.toString());
        peer.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId.toString()), LOCAL_MEMBER_NAME, builder.build()), leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);

        // Add a remote candidate

        peer.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Register local

        leader.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the local candidate becomes owner

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);

        testLog.info("testDelayedEntityOwnerSelectionWhenMaxPeerRequestsReceived ending");
    }

    //@Test
    public void testDelayedEntityOwnerSelection() throws Exception {
        testLog.info("testDelayedEntityOwnerSelection starting");

        ShardTestKit kit = new ShardTestKit(getSystem());
        EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder().
                addStrategy(ENTITY_TYPE, LastCandidateSelectionStrategy.class, 500);

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        ShardIdentifier peerId1 = newShardId(PEER_MEMBER_1_NAME);
        ShardIdentifier peerId2 = newShardId(PEER_MEMBER_2_NAME);

        TestActorRef<TestEntityOwnershipShard> peer1 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId1, peerMap(leaderId.toString(), peerId2.toString()), PEER_MEMBER_1_NAME)),
                    peerId1.toString());
        peer1.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<TestEntityOwnershipShard> peer2 = actorFactory.createTestActor(TestEntityOwnershipShard.props(
                newShardBuilder(peerId2, peerMap(leaderId.toString(), peerId1.toString()), PEER_MEMBER_2_NAME)),
                    peerId2.toString());
        peer2.underlyingActor().startDroppingMessagesOfType(ElectionTimeout.class);

        TestActorRef<EntityOwnershipShard> leader = actorFactory.createTestActor(
                newShardProps(leaderId, peerMap(peerId1.toString(), peerId2.toString()), LOCAL_MEMBER_NAME, builder.build()),
                    leaderId.toString());

        ShardTestKit.waitUntilLeader(leader);

        DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);

        // Add a remote candidate

        peer1.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Register local

        leader.tell(new RegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the local candidate becomes owner

        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), PEER_MEMBER_1_NAME);
        verifyCommittedEntityCandidate(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verifyOwner(leader, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);

        testLog.info("testDelayedEntityOwnerSelection ending");
    }

    private Props newLocalShardProps() {
        return newShardProps(newShardId(LOCAL_MEMBER_NAME), Collections.<String,String>emptyMap(), LOCAL_MEMBER_NAME);
    }

    private Props newShardProps(ShardIdentifier shardId, Map<String,String> peers, String memberName) {
        return newShardProps(shardId, peers, memberName, EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    private Props newShardProps(ShardIdentifier shardId, Map<String,String> peers, String memberName,
                                EntityOwnerSelectionStrategyConfig config) {
        return newShardBuilder(shardId, peers, memberName).ownerSelectionStrategyConfig(config).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId());
    }

    private EntityOwnershipShard.Builder newShardBuilder(ShardIdentifier shardId, Map<String,String> peers,
            String memberName) {
        return EntityOwnershipShard.newBuilder().id(shardId).peerAddresses(peers).datastoreContext(
                dataStoreContextBuilder.build()).schemaContext(SCHEMA_CONTEXT).localMemberName(
                        MemberName.forName(memberName)).ownerSelectionStrategyConfig(
                                EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    private Map<String, String> peerMap(String... peerIds) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        for(String peerId: peerIds) {
            builder.put(peerId, actorFactory.createTestActorPath(peerId)).build();
        }

        return builder.build();
    }

    private static class TestEntityOwnershipShard extends EntityOwnershipShard {
        private final TestActorRef<MessageCollectorActor> collectorActor;
        private final Map<Class<?>, Predicate<?>> dropMessagesOfType = new ConcurrentHashMap<>();

        TestEntityOwnershipShard(Builder builder, TestActorRef<MessageCollectorActor> collectorActor) {
            super(builder);
            this.collectorActor = collectorActor;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void handleCommand(Object message) {
            if(collectorActor != null) {
                collectorActor.tell(message, ActorRef.noSender());
            }

            Predicate drop = dropMessagesOfType.get(message.getClass());
            if(drop == null || !drop.test(message)) {
                super.handleCommand(message);
            }
        }

        void startDroppingMessagesOfType(Class<?> msgClass) {
            dropMessagesOfType.put(msgClass, msg -> true);
        }

        <T> void startDroppingMessagesOfType(Class<T> msgClass, Predicate<T> filter) {
            dropMessagesOfType.put(msgClass, filter);
        }

        void stopDroppingMessagesOfType(Class<?> msgClass) {
            dropMessagesOfType.remove(msgClass);
        }

        TestActorRef<MessageCollectorActor> collectorActor() {
            return collectorActor;
        }

        static Props props(Builder builder) {
            return props(builder, null);
        }

        static Props props(Builder builder, TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(TestEntityOwnershipShard.class, builder, collectorActor).
                    withDispatcher(Dispatchers.DefaultDispatcherId());
        }
    }
}
