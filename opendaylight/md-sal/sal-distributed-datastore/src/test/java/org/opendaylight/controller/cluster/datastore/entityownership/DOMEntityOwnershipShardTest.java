/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMRegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMRegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMUnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMUnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

public class DOMEntityOwnershipShardTest extends AbstractEntityOwnershipTest {
    private static final String ENTITY_TYPE = "test type";
    private static final YangInstanceIdentifier ENTITY_ID1 = YangInstanceIdentifier
            .of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 = YangInstanceIdentifier
            .of(QName.create("test", "2015-08-14", "entity2"));
    private static final YangInstanceIdentifier ENTITY_ID3 = YangInstanceIdentifier
            .of(QName.create("test", "2015-08-14", "entity3"));
    private static final YangInstanceIdentifier ENTITY_ID4 = YangInstanceIdentifier
            .of(QName.create("test", "2015-08-14", "entity4"));
    private static final YangInstanceIdentifier ENTITY_ID5 = YangInstanceIdentifier
            .of(QName.create("test", "2015-08-14", "entity5"));
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();
    private static final AtomicInteger NEXT_SHARD_NUM = new AtomicInteger();
    private static final String LOCAL_MEMBER_NAME = "member-1";

    private final Builder dataStoreContextBuilder = DatastoreContext.newBuilder();
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testOnRegisterCandidateLocal() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps());

        ShardTestKit.waitUntilLeader(shard);

        final YangInstanceIdentifier entityId = ENTITY_ID1;
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithNoInitialLeader() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        final String peerId = newShardId("follower").toString();
        final TestActorRef<MockFollower> peer = actorFactory.createTestActor(
                Props.create(MockFollower.class, peerId, false).withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId);

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(
                newShardProps(ImmutableMap.<String, String> builder().put(peerId, peer.path().toString()).build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()));

        final YangInstanceIdentifier entityId = ENTITY_ID1;
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Now grant the vote so the shard becomes the leader. This should retry the commit.
        peer.underlyingActor().grantVote = true;

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithNoInitialConsensus() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2)
                .shardTransactionCommitTimeoutInSeconds(1);

        final String peerId = newShardId("follower").toString();
        final TestActorRef<MockFollower> peer = actorFactory.createTestActor(
                Props.create(MockFollower.class, peerId).withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        final MockFollower follower = peer.underlyingActor();

        // Drop AppendEntries so consensus isn't reached.
        follower.dropAppendEntries = true;

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(
                newShardProps(ImmutableMap.<String, String> builder().put(peerId, peer.path().toString()).build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()));

        ShardTestKit.waitUntilLeader(shard);

        final YangInstanceIdentifier entityId = ENTITY_ID1;
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Wait enough time for the commit to timeout.
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        // Resume AppendEntries - the follower should ack the commit which should then result in the candidate
        // write being applied to the state.
        follower.dropAppendEntries = false;

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithIsolatedLeader() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2)
                .shardIsolatedLeaderCheckIntervalInMillis(50);

        final String peerId = newShardId("follower").toString();
        final TestActorRef<MockFollower> peer = actorFactory.createTestActor(
                Props.create(MockFollower.class, peerId).withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        final MockFollower follower = peer.underlyingActor();

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(
                newShardProps(ImmutableMap.<String, String> builder().put(peerId, peer.path().toString()).build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()));

        ShardTestKit.waitUntilLeader(shard);

        // Drop AppendEntries and wait enough time for the shard to switch to IsolatedLeader.
        follower.dropAppendEntries = true;
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        final YangInstanceIdentifier entityId = ENTITY_ID1;
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Resume AppendEntries - the candidate write should now be committed.
        follower.dropAppendEntries = false;
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithRemoteLeader() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2)
                .shardBatchedModificationCount(5);

        final String peerId = newShardId("leader").toString();
        final TestActorRef<MockLeader> peer = actorFactory.createTestActor(
                Props.create(MockLeader.class).withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory
                .createTestActor(Props.create(TestEntityOwnershipShard.class, newShardId(LOCAL_MEMBER_NAME),
                        ImmutableMap.<String, String> builder().put(peerId, peer.path().toString()).build(),
                        dataStoreContextBuilder.build()).withDispatcher(Dispatchers.DefaultDispatcherId()));

        shard.tell(new AppendEntries(1L, peerId, -1L, -1L, Collections.<ReplicatedLogEntry> emptyList(), -1L, -1L,
                DataStoreVersions.CURRENT_VERSION), peer);

        shard.tell(new DOMRegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        final MockLeader leader = peer.underlyingActor();
        assertEquals("Leader received BatchedModifications", true,
                Uninterruptibles.awaitUninterruptibly(leader.modificationsReceived, 5, TimeUnit.SECONDS));
        verifyBatchedEntityCandidate(leader.getAndClearReceivedModifications(), ENTITY_TYPE, ENTITY_ID1,
                LOCAL_MEMBER_NAME);

        // Test with initial commit timeout and subsequent retry.

        leader.modificationsReceived = new CountDownLatch(1);
        leader.sendReply = false;

        shard.tell(dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1).build(), ActorRef.noSender());

        shard.tell(new DOMRegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        assertEquals("Leader received BatchedModifications", true,
                Uninterruptibles.awaitUninterruptibly(leader.modificationsReceived, 5, TimeUnit.SECONDS));
        verifyBatchedEntityCandidate(leader.getAndClearReceivedModifications(), ENTITY_TYPE, ENTITY_ID2,
                LOCAL_MEMBER_NAME);

        // Send a bunch of registration messages quickly and verify.

        final int max = 100;
        leader.delay = 4;
        leader.modificationsReceived = new CountDownLatch(max);
        final List<YangInstanceIdentifier> entityIds = new ArrayList<>();
        for (int i = 1; i <= max; i++) {
            final YangInstanceIdentifier id = YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "test" + i));
            entityIds.add(id);
            shard.tell(new DOMRegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, id)), kit.getRef());
        }

        assertEquals("Leader received BatchedModifications", true,
                Uninterruptibles.awaitUninterruptibly(leader.modificationsReceived, 10, TimeUnit.SECONDS));

        // Sleep a little to ensure no additional BatchedModifications are received.

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        final List<Modification> receivedMods = leader.getAndClearReceivedModifications();
        for (int i = 0; i < max; i++) {
            verifyBatchedEntityCandidate(receivedMods.get(i), ENTITY_TYPE, entityIds.get(i), LOCAL_MEMBER_NAME);
        }

        assertEquals("# modifications received", max, receivedMods.size());
    }

    @Test
    public void testOnUnregisterCandidateLocal() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());
        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(shard);

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);

        // Register

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Unregister

        shard.tell(new DOMUnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, "");

        // Register again

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOwnershipChanges() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());
        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(shard);

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        final ShardDataTree shardDataTree = shard.underlyingActor().getDataStore();

        // Add a remote candidate

        final String remoteMemberName1 = "remoteMember1";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, remoteMemberName1),
                shardDataTree);

        // Register local

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the remote candidate becomes owner

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);

        // Add another remote candidate and verify ownership doesn't change

        final String remoteMemberName2 = "remoteMember2";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, remoteMemberName2),
                shardDataTree);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName2);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);

        // Remove the second remote candidate and verify ownership doesn't change

        deleteNode(candidatePath(ENTITY_TYPE, ENTITY_ID1, remoteMemberName2), shardDataTree);

        verifyEntityCandidateRemoved(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName2);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);

        // Remove the first remote candidate and verify the local candidate becomes owner

        deleteNode(candidatePath(ENTITY_TYPE, ENTITY_ID1, remoteMemberName1), shardDataTree);

        verifyEntityCandidateRemoved(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Add the second remote candidate back and verify ownership doesn't change

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, remoteMemberName2),
                shardDataTree);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName2);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Unregister the local candidate and verify the second remote candidate becomes owner

        shard.tell(new DOMUnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyEntityCandidateRemoved(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName2);
    }

    @Test
    public void testOwnerChangesOnPeerAvailabilityChanges() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(500).shardElectionTimeoutFactor(10000);

        final String peerMemberName1 = "peerMember1";
        final String peerMemberName2 = "peerMember2";

        final ShardIdentifier leaderId = newShardId(LOCAL_MEMBER_NAME);
        final ShardIdentifier peerId1 = newShardId(peerMemberName1);
        final ShardIdentifier peerId2 = newShardId(peerMemberName2);

        TestActorRef<DOMEntityOwnershipShard> peer1 = actorFactory.createTestActor(newShardProps(peerId1,
                ImmutableMap.<String, String> builder().put(leaderId.toString(), "").put(peerId2.toString(), "")
                        .build(),
                peerMemberName1, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId1.toString());

        TestActorRef<DOMEntityOwnershipShard> peer2 = actorFactory.createTestActor(newShardProps(peerId2,
                ImmutableMap.<String, String> builder().put(leaderId.toString(), "").put(peerId1.toString(), "")
                        .build(),
                peerMemberName2, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId2.toString());

        final TestActorRef<DOMEntityOwnershipShard> leader = actorFactory.createTestActor(newShardProps(leaderId,
                ImmutableMap.<String, String> builder().put(peerId1.toString(), peer1.path().toString())
                        .put(peerId2.toString(), peer2.path().toString()).build(),
                LOCAL_MEMBER_NAME, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                leaderId.toString());
        leader.tell(ElectionTimeout.INSTANCE, leader);

        ShardTestKit.waitUntilLeader(leader);

        // Send PeerDown and PeerUp with no entities

        leader.tell(new PeerDown(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        // Add candidates for entity1 with the local leader as the owner

        leader.tell(new DOMRegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, peerMemberName2), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, peerMemberName2);

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, peerMemberName1), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Add candidates for entity2 with peerMember2 as the owner

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, peerMemberName2), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, peerMemberName1), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);

        // Add candidates for entity3 with peerMember2 as the owner.

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID3, peerMemberName2), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, peerMemberName2);

        leader.tell(new DOMRegisterCandidateLocal(new DOMEntity(ENTITY_TYPE, ENTITY_ID3)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID3, peerMemberName1), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, peerMemberName2);

        // Add only candidate peerMember2 for entity4.

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID4, peerMemberName2), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID4, peerMemberName2);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, peerMemberName2);

        // Add only candidate peerMember1 for entity5.

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID5, peerMemberName1), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID5, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID5, peerMemberName1);

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

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, ""); // no other candidates so should clear
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, peerMemberName2);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, peerMemberName2);
        verifyNoEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID4, peerMemberName2);

        // Reinstate peerMember2 - no owners should change

        peer2 = actorFactory.createTestActor(newShardProps(peerId2,
                ImmutableMap.<String, String> builder().put(leaderId.toString(), "").put(peerId1.toString(), "")
                        .build(),
                peerMemberName2, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId2.toString());
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        // Send PeerUp again - should be noop
        leader.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());
        peer1.tell(new PeerUp(peerId2.getMemberName(), peerId2.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, "");

        // Add back candidate peerMember2 for entities 1, 2, & 3.

        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, peerMemberName2), kit);
        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, peerMemberName2), kit);
        commitModification(leader, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID3, peerMemberName2), kit);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID1, peerMemberName2);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyCommittedEntityCandidate(leader, ENTITY_TYPE, ENTITY_ID3, peerMemberName2);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName1);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);

        // Kill peerMember1 and send PeerDown - entity 2 should get a new owner selected

        peer1.tell(PoisonPill.getInstance(), ActorRef.noSender());
        leader.tell(new PeerDown(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);

        // Verify the reinstated peerMember2 is fully synced.

        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID4, "");
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Reinstate peerMember1 and verify no owner changes

        peer1 = actorFactory.createTestActor(newShardProps(peerId1,
                ImmutableMap.<String, String> builder().put(leaderId.toString(), "").put(peerId2.toString(), "")
                        .build(),
                peerMemberName1, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId1.toString());
        leader.tell(new PeerUp(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID4, "");
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyOwner(leader, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Verify the reinstated peerMember1 is fully synced.

        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID4, "");
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID3, LOCAL_MEMBER_NAME);
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyOwner(peer1, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);

        // Kill the local leader and elect peer2 the leader. This should cause a new owner to be selected for
        // the entities (1 and 3) previously owned by the local leader member.

        peer2.tell(new PeerAddressResolved(peerId1.toString(), peer1.path().toString()), ActorRef.noSender());
        peer2.tell(new PeerUp(leaderId.getMemberName(), leaderId.toString()), ActorRef.noSender());
        peer2.tell(new PeerUp(peerId1.getMemberName(), peerId1.toString()), ActorRef.noSender());

        leader.tell(PoisonPill.getInstance(), ActorRef.noSender());
        peer2.tell(new PeerDown(leaderId.getMemberName(), leaderId.toString()), ActorRef.noSender());
        peer2.tell(ElectionTimeout.INSTANCE, peer2);

        ShardTestKit.waitUntilLeader(peer2);

        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID4, "");
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID3, peerMemberName2);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID2, peerMemberName2);
        verifyOwner(peer2, ENTITY_TYPE, ENTITY_ID1, peerMemberName2);
    }

    @Test
    public void testLocalCandidateRemovedWithCandidateRegistered() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10000);
        final ShardIdentifier leaderId = newShardId("leader");
        final ShardIdentifier localId = newShardId(LOCAL_MEMBER_NAME);

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory
                .createTestActor(Props.create(TestEntityOwnershipShard.class, localId,
                        ImmutableMap.<String, String> builder().put(leaderId.toString(), "".toString()).build(),
                        dataStoreContextBuilder.build()).withDispatcher(Dispatchers.DefaultDispatcherId()));

        final TestActorRef<DOMEntityOwnershipShard> leader = actorFactory
                .createTestActor(newShardProps(leaderId,
                        ImmutableMap.<String, String> builder().put(localId.toString(), shard.path().toString())
                                .build(),
                        LOCAL_MEMBER_NAME, EntityOwnerSelectionStrategyConfig.newBuilder().build())
                                .withDispatcher(Dispatchers.DefaultDispatcherId()),
                        leaderId.toString());
        leader.tell(ElectionTimeout.INSTANCE, leader);

        ShardTestKit.waitUntilLeader(leader);

        shard.tell(new PeerAddressResolved(leaderId.toString(), leader.path().toString()), ActorRef.noSender());

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        final DOMEntityOwnershipListener listener = mock(DOMEntityOwnershipListener.class);

        shard.tell(new DOMRegisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Register local candidate

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);
        verifyCommittedEntityCandidate(shard, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity, false, true, true));
        reset(listener);

        // Simulate a replicated commit from the leader to remove the local candidate that would occur after a
        // network partition is healed.

        leader.tell(new PeerDown(localId.getMemberName(), localId.toString()), ActorRef.noSender());

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity, true, false, false));

        // Since the the shard has a local candidate registered, it should re-add its candidate to the entity.

        verifyCommittedEntityCandidate(shard, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity, false, true, true));

        // Unregister the local candidate and verify it's removed and no re-added.

        shard.tell(new DOMUnregisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyNoEntityCandidate(shard, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyNoEntityCandidate(shard, entity.getType(), entity.getIdentifier(), LOCAL_MEMBER_NAME);
    }

    @Test
    public void testListenerRegistration() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());
        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(shard);
        final ShardDataTree shardDataTree = shard.underlyingActor().getDataStore();

        final String otherEntityType = "otherEntityType";
        final DOMEntity entity1 = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, ENTITY_ID2);
        final DOMEntity entity3 = new DOMEntity(ENTITY_TYPE, ENTITY_ID3);
        final DOMEntity entity4 = new DOMEntity(otherEntityType, ENTITY_ID3);
        final DOMEntityOwnershipListener listener = mock(DOMEntityOwnershipListener.class);

        // Register listener

        shard.tell(new DOMRegisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Register a couple candidates for the desired entity type and verify listener is notified.

        shard.tell(new DOMRegisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, true, true));

        shard.tell(new DOMRegisterCandidateLocal(entity2), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity2, false, true, true));
        reset(listener);

        // Register another candidate for another entity type and verify listener is not notified.

        shard.tell(new DOMRegisterCandidateLocal(entity4), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(ownershipChange(entity4));

        // Register remote candidate for entity1

        final String remoteMemberName = "remoteMember";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getIdentifier(), remoteMemberName),
                shardDataTree);
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entity1.getIdentifier(), remoteMemberName);

        // Unregister the local candidate for entity1 and verify listener is notified

        shard.tell(new DOMUnregisterCandidateLocal(entity1), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000)).ownershipChanged(ownershipChange(entity1, true, false, true));
        reset(listener);

        // Unregister the listener, add a candidate for entity3 and verify listener isn't notified

        shard.tell(new DOMUnregisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        shard.tell(new DOMRegisterCandidateLocal(entity3), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyOwner(shard, ENTITY_TYPE, entity3.getIdentifier(), LOCAL_MEMBER_NAME);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));

        // Re-register the listener and verify it gets notified of currently owned entities

        reset(listener);

        shard.tell(new DOMRegisterListenerLocal(listener, ENTITY_TYPE), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verify(listener, timeout(5000).times(2)).ownershipChanged(
                or(ownershipChange(entity2, false, true, true), ownershipChange(entity3, false, true, true)));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(listener, never()).ownershipChanged(ownershipChange(entity4));
        verify(listener, times(1)).ownershipChanged(ownershipChange(entity1));
    }

    private static void commitModification(final TestActorRef<DOMEntityOwnershipShard> shard,
            final NormalizedNode<?, ?> node, final JavaTestKit sender) {
        final BatchedModifications modifications = newBatchedModifications();
        modifications.addModification(new MergeModification(ENTITY_OWNERS_PATH, node));

        shard.tell(modifications, sender.getRef());
        sender.expectMsgClass(CommitTransactionReply.class);
    }

    private static BatchedModifications newBatchedModifications() {
        final BatchedModifications modifications = new BatchedModifications(nextTransactionId(),
                DataStoreVersions.CURRENT_VERSION);
        modifications.setDoCommitOnReady(true);
        modifications.setReady(true);
        modifications.setTotalMessagesSent(1);
        return modifications;
    }

    private void verifyEntityCandidateRemoved(final TestActorRef<DOMEntityOwnershipShard> shard,
            final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyNodeRemoved(candidatePath(entityType, entityId, candidateName),
                new Function<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
                    @Override
                    public NormalizedNode<?, ?> apply(final YangInstanceIdentifier path) {
                        try {
                            return AbstractShardTest.readStore(shard, path);
                        } catch (final Exception e) {
                            throw new AssertionError("Failed to read " + path, e);
                        }
                    }
                });
    }

    private void verifyCommittedEntityCandidate(final TestActorRef<DOMEntityOwnershipShard> shard,
            final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName,
                new Function<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
                    @Override
                    public NormalizedNode<?, ?> apply(final YangInstanceIdentifier path) {
                        try {
                            return AbstractShardTest.readStore(shard, path);
                        } catch (final Exception e) {
                            throw new AssertionError("Failed to read " + path, e);
                        }
                    }
                });
    }

    private void verifyNoEntityCandidate(final TestActorRef<DOMEntityOwnershipShard> shard, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName,
                new Function<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
                    @Override
                    public NormalizedNode<?, ?> apply(final YangInstanceIdentifier path) {
                        try {
                            return AbstractShardTest.readStore(shard, path);
                        } catch (final Exception e) {
                            throw new AssertionError("Failed to read " + path, e);
                        }
                    }
                }, false);
    }

    private void verifyBatchedEntityCandidate(final List<Modification> mods, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) throws Exception {
        assertEquals("BatchedModifications size", 1, mods.size());
        verifyBatchedEntityCandidate(mods.get(0), entityType, entityId, candidateName);
    }

    private void verifyBatchedEntityCandidate(final Modification mod, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) throws Exception {
        assertEquals("Modification type", MergeModification.class, mod.getClass());
        verifyEntityCandidate(((MergeModification) mod).getData(), entityType, entityId, candidateName, true);
    }

    private static void verifyOwner(final TestActorRef<DOMEntityOwnershipShard> shard, final String entityType,
            final YangInstanceIdentifier entityId, final String localMemberName) {
        verifyOwner(localMemberName, entityType, entityId,
                new Function<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
                    @Override
                    public NormalizedNode<?, ?> apply(final YangInstanceIdentifier path) {
                        try {
                            return AbstractShardTest.readStore(shard, path);
                        } catch (final Exception e) {
                            return null;
                        }
                    }
                });
    }

    private Props newShardProps() {
        return newShardProps(Collections.<String, String> emptyMap());
    }

    private Props newShardProps(final EntityOwnerSelectionStrategyConfig strategyConfig,
            final Map<String, String> peers) {
        return newShardProps(newShardId(LOCAL_MEMBER_NAME), peers, LOCAL_MEMBER_NAME, strategyConfig);
    }

    private Props newShardProps(final Map<String, String> peers) {
        return newShardProps(newShardId(LOCAL_MEMBER_NAME), peers, LOCAL_MEMBER_NAME,
                EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    private Props newShardProps(final ShardIdentifier shardId, final Map<String, String> peers, final String memberName,
            final EntityOwnerSelectionStrategyConfig config) {
        return DOMEntityOwnershipShard.newBuilder().id(shardId).peerAddresses(peers)
                .datastoreContext(dataStoreContextBuilder.build()).schemaContext(SCHEMA_CONTEXT)
                .localMemberName(MemberName.forName(memberName)).ownerSelectionStrategyConfig(config).props()
                .withDispatcher(Dispatchers.DefaultDispatcherId());
    }

    private static ShardIdentifier newShardId(final String memberName) {
        return ShardIdentifier.create("dom-entity-ownership", MemberName.forName(memberName),
                "operational" + NEXT_SHARD_NUM.getAndIncrement());
    }

    private static class TestEntityOwnershipShard extends DOMEntityOwnershipShard {

        TestEntityOwnershipShard(final ShardIdentifier name, final Map<String, String> peerAddresses,
                final DatastoreContext datastoreContext) {
            super(newBuilder().id(name).peerAddresses(peerAddresses).datastoreContext(datastoreContext)
                    .schemaContext(SCHEMA_CONTEXT).localMemberName(MemberName.forName(LOCAL_MEMBER_NAME)));
        }

        @Override
        public void handleCommand(final Object message) {
            if (!(message instanceof ElectionTimeout)) {
                super.handleCommand(message);
            }
        }
    }

    private static class MockFollower extends UntypedActor {
        volatile boolean grantVote;
        volatile boolean dropAppendEntries;
        private final String myId;

        @SuppressWarnings("unused")
        public MockFollower(final String myId) {
            this(myId, true);
        }

        public MockFollower(final String myId, final boolean grantVote) {
            this.myId = myId;
            this.grantVote = grantVote;
        }

        @Override
        public void onReceive(final Object message) {
            if (message instanceof RequestVote) {
                if (grantVote) {
                    getSender().tell(new RequestVoteReply(((RequestVote) message).getTerm(), true), getSelf());
                }
            } else if (message instanceof AppendEntries) {
                if (!dropAppendEntries) {
                    final AppendEntries req = (AppendEntries) message;
                    long lastIndex = req.getLeaderCommit();
                    if (req.getEntries().size() > 0) {
                        for (final ReplicatedLogEntry entry : req.getEntries()) {
                            lastIndex = entry.getIndex();
                        }
                    }

                    getSender().tell(new AppendEntriesReply(myId, req.getTerm(), true, lastIndex, req.getTerm(),
                            DataStoreVersions.CURRENT_VERSION), getSelf());
                }
            }
        }
    }

    @Test
    public void testDelayedEntityOwnerSelectionWhenMaxPeerRequestsReceived() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder()
                .addStrategy(ENTITY_TYPE, LastCandidateSelectionStrategy.class, 500);

        final String peerId = newShardId("follower").toString();
        final TestActorRef<MockFollower> peer = actorFactory.createTestActor(
                Props.create(MockFollower.class, peerId, false).withDispatcher(Dispatchers.DefaultDispatcherId()),
                peerId);

        peer.underlyingActor().grantVote = true;

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory.createTestActor(
                newShardProps(builder.build(), ImmutableMap.of(peerId.toString(), peer.path().toString())));
        ShardTestKit.waitUntilLeader(shard);

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        final ShardDataTree shardDataTree = shard.underlyingActor().getDataStore();

        // Add a remote candidate

        final String remoteMemberName1 = "follower";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, remoteMemberName1),
                shardDataTree);

        // Register local

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the local candidate becomes owner

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testDelayedEntityOwnerSelection() throws Exception {
        final ShardTestKit kit = new ShardTestKit(getSystem());
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder()
                .addStrategy(ENTITY_TYPE, LastCandidateSelectionStrategy.class, 500);

        final String follower1Id = newShardId("follower1").toString();
        final TestActorRef<MockFollower> follower1 = actorFactory.createTestActor(
                Props.create(MockFollower.class, follower1Id, false).withDispatcher(Dispatchers.DefaultDispatcherId()),
                follower1Id);

        follower1.underlyingActor().grantVote = true;

        final String follower2Id = newShardId("follower").toString();
        final TestActorRef<MockFollower> follower2 = actorFactory.createTestActor(
                Props.create(MockFollower.class, follower2Id, false).withDispatcher(Dispatchers.DefaultDispatcherId()),
                follower2Id);

        follower2.underlyingActor().grantVote = true;

        final TestActorRef<DOMEntityOwnershipShard> shard = actorFactory
                .createTestActor(newShardProps(builder.build(), ImmutableMap.of(follower1Id.toString(),
                        follower2.path().toString(), follower2Id.toString(), follower2.path().toString())));
        ShardTestKit.waitUntilLeader(shard);

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
        final ShardDataTree shardDataTree = shard.underlyingActor().getDataStore();

        // Add a remote candidate

        final String remoteMemberName1 = "follower";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, remoteMemberName1),
                shardDataTree);

        // Register local

        shard.tell(new DOMRegisterCandidateLocal(entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Verify the local candidate becomes owner

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, remoteMemberName1);
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
        verifyOwner(shard, ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME);
    }

    private static class MockLeader extends UntypedActor {
        volatile CountDownLatch modificationsReceived = new CountDownLatch(1);
        List<Modification> receivedModifications = new ArrayList<>();
        volatile boolean sendReply = true;
        volatile long delay;

        @Override
        public void onReceive(final Object message) {
            if (message instanceof BatchedModifications) {
                if (delay > 0) {
                    Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
                }

                if (sendReply) {
                    final BatchedModifications mods = (BatchedModifications) message;
                    synchronized (receivedModifications) {
                        for (int i = 0; i < mods.getModifications().size(); i++) {
                            receivedModifications.add(mods.getModifications().get(i));
                            modificationsReceived.countDown();
                        }
                    }

                    getSender().tell(
                            CommitTransactionReply.instance(DataStoreVersions.CURRENT_VERSION).toSerializable(),
                            getSelf());
                } else {
                    sendReply = true;
                }
            }
        }

        List<Modification> getAndClearReceivedModifications() {
            synchronized (receivedModifications) {
                final List<Modification> ret = new ArrayList<>(receivedModifications);
                receivedModifications.clear();
                return ret;
            }
        }
    }
}
