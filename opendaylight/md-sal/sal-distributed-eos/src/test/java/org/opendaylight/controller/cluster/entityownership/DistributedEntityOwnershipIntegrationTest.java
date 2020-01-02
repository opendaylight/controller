/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyRaftState;
import static org.opendaylight.controller.cluster.entityownership.AbstractEntityOwnershipTest.ownershipChange;
import static org.opendaylight.controller.cluster.entityownership.DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.CANDIDATE_NAME_NODE_ID;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityPath;

import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.base.MockitoException;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ChangeShardMembersVotingStatus;
import org.opendaylight.controller.cluster.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * End-to-end integration tests for the entity ownership functionality.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipIntegrationTest {
    private static final String MODULE_SHARDS_CONFIG = "module-shards-default.conf";
    private static final String MODULE_SHARDS_5_NODE_CONFIG = "module-shards-default-5-node.conf";
    private static final String MODULE_SHARDS_MEMBER_1_CONFIG = "module-shards-default-member-1.conf";
    private static final String ENTITY_TYPE1 = "entityType1";
    private static final String ENTITY_TYPE2 = "entityType2";
    private static final DOMEntity ENTITY1 = new DOMEntity(ENTITY_TYPE1, "entity1");
    private static final DOMEntity ENTITY1_2 = new DOMEntity(ENTITY_TYPE2, "entity1");
    private static final DOMEntity ENTITY2 = new DOMEntity(ENTITY_TYPE1, "entity2");
    private static final DOMEntity ENTITY3 = new DOMEntity(ENTITY_TYPE1, "entity3");
    private static final DOMEntity ENTITY4 = new DOMEntity(ENTITY_TYPE1, "entity4");
    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5)
                    .shardIsolatedLeaderCheckIntervalInMillis(1000000);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10000);

    private final List<MemberNode> memberNodes = new ArrayList<>();

    @Mock
    private DOMEntityOwnershipListener leaderMockListener;

    @Mock
    private DOMEntityOwnershipListener leaderMockListener2;

    @Mock
    private DOMEntityOwnershipListener follower1MockListener;

    @Mock
    private DOMEntityOwnershipListener follower2MockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @After
    public void tearDown() {
        for (MemberNode m : Lists.reverse(memberNodes)) {
            m.cleanup();
        }
        memberNodes.clear();
    }

    private static DistributedEntityOwnershipService newOwnershipService(final AbstractDataStore datastore) {
        return DistributedEntityOwnershipService.start(datastore.getActorUtils(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    @Test
    public void testFunctionalityWithThreeNodes() throws Exception {
        String name = "testFunctionalityWithThreeNodes";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        final DOMEntityOwnershipService follower2EntityOwnershipService =
                newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);
        leaderEntityOwnershipService.registerListener(ENTITY_TYPE2, leaderMockListener2);
        follower1EntityOwnershipService.registerListener(ENTITY_TYPE1, follower1MockListener);

        // Register leader candidate for entity1 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY1);
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, true, true));
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));
        reset(leaderMockListener, follower1MockListener);

        verifyGetOwnershipState(leaderEntityOwnershipService, ENTITY1, EntityOwnershipState.IS_OWNER);
        verifyGetOwnershipState(follower1EntityOwnershipService, ENTITY1, EntityOwnershipState.OWNED_BY_OTHER);

        // Register leader candidate for entity1_2 (same id, different type) and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY1_2);
        verify(leaderMockListener2, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1_2, false, true, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(leaderMockListener, never()).ownershipChanged(ownershipChange(ENTITY1_2));
        reset(leaderMockListener2);

        // Register follower1 candidate for entity1 and verify it gets added but doesn't become owner

        follower1EntityOwnershipService.registerCandidate(ENTITY1);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-1", "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-1");
        verifyOwner(follower2Node.configDataStore(), ENTITY1, "member-1");
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(leaderMockListener, never()).ownershipChanged(ownershipChange(ENTITY1));
        verify(follower1MockListener, never()).ownershipChanged(ownershipChange(ENTITY1));

        // Register follower1 candidate for entity2 and verify it becomes owner

        final DOMEntityOwnershipCandidateRegistration follower1Entity2Reg =
                follower1EntityOwnershipService.registerCandidate(ENTITY2);
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, true, true));
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, true));
        verifyOwner(follower2Node.configDataStore(), ENTITY2, "member-2");
        reset(leaderMockListener, follower1MockListener);

        // Register follower2 candidate for entity2 and verify it gets added but doesn't become owner

        follower2EntityOwnershipService.registerListener(ENTITY_TYPE1, follower2MockListener);
        verify(follower2MockListener, timeout(5000).times(2)).ownershipChanged(or(
                ownershipChange(ENTITY1, false, false, true), ownershipChange(ENTITY2, false, false, true)));

        follower2EntityOwnershipService.registerCandidate(ENTITY2);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-2", "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-2");

        // Unregister follower1 candidate for entity2 and verify follower2 becomes owner

        follower1Entity2Reg.close();
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-3");
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, true, false, true));
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, true));

        // Depending on timing, follower2MockListener could get ownershipChanged with "false, false, true" if
        // if the original ownership change with "member-2 is replicated to follower2 after the listener is
        // registered.
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, true, true));

        // Register follower1 candidate for entity3 and verify it becomes owner

        follower1EntityOwnershipService.registerCandidate(ENTITY3);
        verifyOwner(leaderDistributedDataStore, ENTITY3, "member-2");
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY3, false, true, true));
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY3, false, false, true));
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY3, false, false, true));

        // Register follower2 candidate for entity4 and verify it becomes owner

        follower2EntityOwnershipService.registerCandidate(ENTITY4);
        verifyOwner(leaderDistributedDataStore, ENTITY4, "member-3");
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY4, false, true, true));
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY4, false, false, true));
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY4, false, false, true));
        reset(follower1MockListener, follower2MockListener);

        // Register follower1 candidate for entity4 and verify it gets added but doesn't become owner

        follower1EntityOwnershipService.registerCandidate(ENTITY4);
        verifyCandidates(leaderDistributedDataStore, ENTITY4, "member-3", "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY4, "member-3");

        // Shutdown follower2 and verify it's owned entities (entity 4) get re-assigned

        reset(leaderMockListener, follower1MockListener);
        follower2Node.cleanup();

        verify(follower1MockListener, timeout(15000)).ownershipChanged(ownershipChange(ENTITY4, false, true, true));
        verify(leaderMockListener, timeout(15000)).ownershipChanged(ownershipChange(ENTITY4, false, false, true));

        // Register leader candidate for entity2 and verify it becomes owner

        DOMEntityOwnershipCandidateRegistration leaderEntity2Reg =
                leaderEntityOwnershipService.registerCandidate(ENTITY2);
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, true, true));

        // Unregister leader candidate for entity2 and verify the owner is cleared

        leaderEntity2Reg.close();
        verifyOwner(leaderDistributedDataStore, ENTITY2, "");
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, true, false, false));
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, false));
    }

    @Test
    public void testLeaderEntityOwnersReassignedAfterShutdown() throws Exception {
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(5)
                    .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        String name = "testLeaderEntityOwnersReassignedAfterShutdown";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        follower1Node.waitForMembersUp("member-1", "member-3");

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        final DOMEntityOwnershipService follower2EntityOwnershipService =
                newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        // Register follower1 candidate for entity1 and verify it becomes owner

        follower1EntityOwnershipService.registerCandidate(ENTITY1);
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");

        // Register leader candidate for entity1

        leaderEntityOwnershipService.registerCandidate(ENTITY1);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-2", "member-1");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");

        // Register leader candidate for entity2 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY2);
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Register follower2 candidate for entity2

        follower2EntityOwnershipService.registerCandidate(ENTITY2);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-1", "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Re-enable elections on all remaining followers so one becomes the new leader

        ActorRef follower1Shard = IntegrationTestKit.findLocalShard(follower1Node.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);
        follower1Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build())
                .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        ActorRef follower2Shard = IntegrationTestKit.findLocalShard(follower2Node.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);
        follower2Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build())
                .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        // Shutdown the leader and verify its removed from the candidate list

        leaderNode.cleanup();
        follower1Node.waitForMemberDown("member-1");
        follower2Node.waitForMemberDown("member-1");

        // Verify the prior leader's entity owners are re-assigned.

        verifyCandidates(follower1Node.configDataStore(), ENTITY1, "member-2", "member-1");
        verifyCandidates(follower1Node.configDataStore(), ENTITY2, "member-1", "member-3");
        verifyOwner(follower1Node.configDataStore(), ENTITY1, "member-2");
        verifyOwner(follower1Node.configDataStore(), ENTITY2, "member-3");
    }

    @Test
    public void testLeaderAndFollowerEntityOwnersReassignedAfterShutdown() throws Exception {
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(5)
                .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        String name = "testLeaderAndFollowerEntityOwnersReassignedAfterShutdown";
        final MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        final MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode follower3Node = MemberNode.builder(memberNodes).akkaConfig("Member4")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode follower4Node = MemberNode.builder(memberNodes).akkaConfig("Member5")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();
        follower3Node.configDataStore().waitTillReady();
        follower4Node.configDataStore().waitTillReady();

        leaderNode.waitForMembersUp("member-2", "member-3", "member-4", "member-5");
        follower1Node.waitForMembersUp("member-1", "member-3", "member-4", "member-5");

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        final DOMEntityOwnershipService follower2EntityOwnershipService =
                newOwnershipService(follower2Node.configDataStore());
        final DOMEntityOwnershipService follower3EntityOwnershipService =
                newOwnershipService(follower3Node.configDataStore());
        newOwnershipService(follower4Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        // Register follower1 candidate for entity1 and verify it becomes owner

        follower1EntityOwnershipService.registerCandidate(ENTITY1);
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");

        // Register leader candidate for entity1

        leaderEntityOwnershipService.registerCandidate(ENTITY1);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-2", "member-1");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");

        // Register leader candidate for entity2 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY2);
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Register follower2 candidate for entity2

        follower2EntityOwnershipService.registerCandidate(ENTITY2);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-1", "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Register follower3 as a candidate for entity2 as well

        follower3EntityOwnershipService.registerCandidate(ENTITY2);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-1", "member-3", "member-4");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Re-enable elections on all remaining followers so one becomes the new leader

        ActorRef follower1Shard = IntegrationTestKit.findLocalShard(follower1Node.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);
        follower1Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build())
                .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        ActorRef follower2Shard = IntegrationTestKit.findLocalShard(follower2Node.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);
        follower2Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build())
                .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        ActorRef follower4Shard = IntegrationTestKit.findLocalShard(follower4Node.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);
        follower4Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build())
                .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        // Shutdown the leader and follower3

        leaderNode.cleanup();
        follower3Node.cleanup();

        follower1Node.waitForMemberDown("member-1");
        follower1Node.waitForMemberDown("member-4");
        follower2Node.waitForMemberDown("member-1");
        follower2Node.waitForMemberDown("member-4");
        follower4Node.waitForMemberDown("member-1");
        follower4Node.waitForMemberDown("member-4");

        // Verify the prior leader's and follower3 entity owners are re-assigned.

        verifyCandidates(follower1Node.configDataStore(), ENTITY1, "member-2", "member-1");
        verifyCandidates(follower1Node.configDataStore(), ENTITY2, "member-1", "member-3", "member-4");
        verifyOwner(follower1Node.configDataStore(), ENTITY1, "member-2");
        verifyOwner(follower1Node.configDataStore(), ENTITY2, "member-3");
    }

    /**
     * Reproduces bug <a href="https://bugs.opendaylight.org/show_bug.cgi?id=4554">4554</a>.
     */
    @Test
    public void testCloseCandidateRegistrationInQuickSuccession() throws Exception {
        String name = "testCloseCandidateRegistrationInQuickSuccession";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        final DOMEntityOwnershipService follower2EntityOwnershipService =
                newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);
        follower1EntityOwnershipService.registerListener(ENTITY_TYPE1, follower1MockListener);
        follower2EntityOwnershipService.registerListener(ENTITY_TYPE1, follower2MockListener);

        final DOMEntityOwnershipCandidateRegistration candidate1 =
                leaderEntityOwnershipService.registerCandidate(ENTITY1);
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, true, true));

        final DOMEntityOwnershipCandidateRegistration candidate2 =
                follower1EntityOwnershipService.registerCandidate(ENTITY1);
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));

        final DOMEntityOwnershipCandidateRegistration candidate3 =
                follower2EntityOwnershipService.registerCandidate(ENTITY1);
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));

        Mockito.reset(leaderMockListener, follower1MockListener, follower2MockListener);

        ArgumentCaptor<DOMEntityOwnershipChange> leaderChangeCaptor =
                ArgumentCaptor.forClass(DOMEntityOwnershipChange.class);
        ArgumentCaptor<DOMEntityOwnershipChange> follower1ChangeCaptor =
                ArgumentCaptor.forClass(DOMEntityOwnershipChange.class);
        ArgumentCaptor<DOMEntityOwnershipChange> follower2ChangeCaptor =
                ArgumentCaptor.forClass(DOMEntityOwnershipChange.class);
        doNothing().when(leaderMockListener).ownershipChanged(leaderChangeCaptor.capture());
        doNothing().when(follower1MockListener).ownershipChanged(follower1ChangeCaptor.capture());
        doNothing().when(follower2MockListener).ownershipChanged(follower2ChangeCaptor.capture());

        candidate1.close();
        candidate2.close();
        candidate3.close();

        boolean passed = false;
        for (int i = 0; i < 100; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            final Optional<EntityOwnershipState> leaderState = leaderEntityOwnershipService.getOwnershipState(ENTITY1);
            final Optional<EntityOwnershipState> follower1State =
                    follower1EntityOwnershipService.getOwnershipState(ENTITY1);
            final Optional<EntityOwnershipState> follower2State =
                    follower2EntityOwnershipService.getOwnershipState(ENTITY1);
            final Optional<DOMEntityOwnershipChange> leaderChange = getValueSafely(leaderChangeCaptor);
            final Optional<DOMEntityOwnershipChange> follower1Change = getValueSafely(follower1ChangeCaptor);
            final Optional<DOMEntityOwnershipChange> follower2Change = getValueSafely(follower2ChangeCaptor);
            if (!leaderState.isPresent() || leaderState.get() == EntityOwnershipState.NO_OWNER
                    && follower1State.isPresent() && follower1State.get() == EntityOwnershipState.NO_OWNER
                    && follower2State.isPresent() && follower2State.get() == EntityOwnershipState.NO_OWNER
                    && leaderChange.isPresent() && !leaderChange.get().getState().hasOwner()
                    && follower1Change.isPresent() && !follower1Change.get().getState().hasOwner()
                    && follower2Change.isPresent() && !follower2Change.get().getState().hasOwner()) {
                passed = true;
                break;
            }
        }

        assertTrue("No ownership change message was sent with hasOwner=false", passed);
    }

    private static Optional<DOMEntityOwnershipChange> getValueSafely(ArgumentCaptor<DOMEntityOwnershipChange> captor) {
        try {
            return Optional.ofNullable(captor.getValue());
        } catch (MockitoException e) {
            // No value was captured
            return Optional.empty();
        }
    }

    /**
     * Tests bootstrapping the entity-ownership shard when there's no shards initially configured for local
     * member. The entity-ownership shard is initially created as inactive (ie remains a follower), requiring
     * an AddShardReplica request to join it to an existing leader.
     */
    @Test
    public void testEntityOwnershipShardBootstrapping() throws Exception {
        String name = "testEntityOwnershipShardBootstrapping";
        String moduleShardsConfig = MODULE_SHARDS_MEMBER_1_CONFIG;
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();
        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore follower1DistributedDataStore = follower1Node.configDataStore();
        follower1DistributedDataStore.waitTillReady();

        leaderNode.waitForMembersUp("member-2");
        follower1Node.waitForMembersUp("member-1");

        DOMEntityOwnershipService follower1EntityOwnershipService = newOwnershipService(follower1DistributedDataStore);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);

        // Register a candidate for follower1 - should get queued since follower1 has no leader
        final DOMEntityOwnershipCandidateRegistration candidateReg =
                follower1EntityOwnershipService.registerCandidate(ENTITY1);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(leaderMockListener, never()).ownershipChanged(ownershipChange(ENTITY1));

        // Add replica in follower1
        AddShardReplica addReplica = new AddShardReplica(ENTITY_OWNERSHIP_SHARD_NAME);
        follower1DistributedDataStore.getActorUtils().getShardManager().tell(addReplica,
                follower1Node.kit().getRef());
        Object reply = follower1Node.kit().expectMsgAnyClassOf(follower1Node.kit().duration("5 sec"),
                Success.class, Failure.class);
        if (reply instanceof Failure) {
            throw new AssertionError("AddShardReplica failed", ((Failure)reply).cause());
        }

        // The queued candidate registration should proceed
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));
        reset(leaderMockListener);

        candidateReg.close();
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, false));
        reset(leaderMockListener);

        // Restart follower1 and verify the entity ownership shard is re-instated by registering.
        Cluster.get(leaderNode.kit().getSystem()).down(Cluster.get(follower1Node.kit().getSystem()).selfAddress());
        follower1Node.cleanup();

        follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();
        follower1EntityOwnershipService = newOwnershipService(follower1Node.configDataStore());

        follower1EntityOwnershipService.registerCandidate(ENTITY1);
        verify(leaderMockListener, timeout(20000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));

        verifyRaftState(follower1Node.configDataStore(), ENTITY_OWNERSHIP_SHARD_NAME, raftState -> {
            assertNull("Custom RaftPolicy class name", raftState.getCustomRaftPolicyClassName());
            assertEquals("Peer count", 1, raftState.getPeerAddresses().keySet().size());
            assertThat("Peer Id", Iterables.<String>getLast(raftState.getPeerAddresses().keySet()),
                    org.hamcrest.CoreMatchers.containsString("member-1"));
        });
    }

    @Test
    public void testOwnerSelectedOnRapidUnregisteringAndRegisteringOfCandidates() throws Exception {
        String name = "testOwnerSelectedOnRapidUnregisteringAndRegisteringOfCandidates";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        // Register leader candidate for entity1 and verify it becomes owner

        DOMEntityOwnershipCandidateRegistration leaderEntity1Reg =
                leaderEntityOwnershipService.registerCandidate(ENTITY1);

        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-1");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-1");

        leaderEntity1Reg.close();
        follower1EntityOwnershipService.registerCandidate(ENTITY1);

        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");
    }

    @Test
    public void testOwnerSelectedOnRapidRegisteringAndUnregisteringOfCandidates() throws Exception {
        String name = "testOwnerSelectedOnRapidRegisteringAndUnregisteringOfCandidates";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService =
                newOwnershipService(follower1Node.configDataStore());
        newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorUtils(), ENTITY_OWNERSHIP_SHARD_NAME);

        // Register leader candidate for entity1 and verify it becomes owner

        final DOMEntityOwnershipCandidateRegistration leaderEntity1Reg =
                leaderEntityOwnershipService.registerCandidate(ENTITY1);

        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-1");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-1");

        follower1EntityOwnershipService.registerCandidate(ENTITY1);
        leaderEntity1Reg.close();

        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-2");
    }

    @Test
    public void testEntityOwnershipWithNonVotingMembers() throws Exception {
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(5)
                .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        String name = "testEntityOwnershipWithNonVotingMembers";
        final MemberNode member1LeaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        final MemberNode member2FollowerNode = MemberNode.builder(memberNodes).akkaConfig("Member2")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode member3FollowerNode = MemberNode.builder(memberNodes).akkaConfig("Member3")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode member4FollowerNode = MemberNode.builder(memberNodes).akkaConfig("Member4")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        final MemberNode member5FollowerNode = MemberNode.builder(memberNodes).akkaConfig("Member5")
                .useAkkaArtery(false).testName(name)
                .moduleShardsConfig(MODULE_SHARDS_5_NODE_CONFIG).schemaContext(EOSTestUtils.SCHEMA_CONTEXT)
                .createOperDatastore(false).datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = member1LeaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        member2FollowerNode.configDataStore().waitTillReady();
        member3FollowerNode.configDataStore().waitTillReady();
        member4FollowerNode.configDataStore().waitTillReady();
        member5FollowerNode.configDataStore().waitTillReady();

        member1LeaderNode.waitForMembersUp("member-2", "member-3", "member-4", "member-5");

        final DOMEntityOwnershipService member3EntityOwnershipService =
                newOwnershipService(member3FollowerNode.configDataStore());
        final DOMEntityOwnershipService member4EntityOwnershipService =
                newOwnershipService(member4FollowerNode.configDataStore());
        final DOMEntityOwnershipService member5EntityOwnershipService =
                newOwnershipService(member5FollowerNode.configDataStore());

        newOwnershipService(member1LeaderNode.configDataStore());
        member1LeaderNode.kit().waitUntilLeader(member1LeaderNode.configDataStore().getActorUtils(),
                ENTITY_OWNERSHIP_SHARD_NAME);

        // Make member4 and member5 non-voting

        Future<Object> future = Patterns.ask(leaderDistributedDataStore.getActorUtils().getShardManager(),
                new ChangeShardMembersVotingStatus(ENTITY_OWNERSHIP_SHARD_NAME,
                        ImmutableMap.of("member-4", Boolean.FALSE, "member-5", Boolean.FALSE)),
                new Timeout(10, TimeUnit.SECONDS));
        Object response = Await.result(future, FiniteDuration.apply(10, TimeUnit.SECONDS));
        if (response instanceof Throwable) {
            throw new AssertionError("ChangeShardMembersVotingStatus failed", (Throwable)response);
        }

        assertNull("Expected null Success response. Actual " + response, response);

        // Register member4 candidate for entity1 - it should not become owner since it's non-voting

        member4EntityOwnershipService.registerCandidate(ENTITY1);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-4");

        // Register member5 candidate for entity2 - it should not become owner since it's non-voting

        member5EntityOwnershipService.registerCandidate(ENTITY2);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-5");

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyOwner(leaderDistributedDataStore, ENTITY1, "");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "");

        // Register member3 candidate for entity1 - it should become owner since it's voting

        member3EntityOwnershipService.registerCandidate(ENTITY1);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-4", "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-3");

        // Switch member4 and member5 back to voting and member3 non-voting. This should result in member4 and member5
        // to become entity owners.

        future = Patterns.ask(leaderDistributedDataStore.getActorUtils().getShardManager(),
                new ChangeShardMembersVotingStatus(ENTITY_OWNERSHIP_SHARD_NAME,
                        ImmutableMap.of("member-3", Boolean.FALSE, "member-4", Boolean.TRUE, "member-5", Boolean.TRUE)),
                new Timeout(10, TimeUnit.SECONDS));
        response = Await.result(future, FiniteDuration.apply(10, TimeUnit.SECONDS));
        if (response instanceof Throwable) {
            throw new AssertionError("ChangeShardMembersVotingStatus failed", (Throwable)response);
        }

        assertNull("Expected null Success response. Actual " + response, response);

        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-4");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-5");
    }

    private static void verifyGetOwnershipState(final DOMEntityOwnershipService service, final DOMEntity entity,
            final EntityOwnershipState expState) {
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertTrue("getOwnershipState present", state.isPresent());
        assertEquals("EntityOwnershipState", expState, state.get());
    }

    private static void verifyCandidates(final AbstractDataStore dataStore, final DOMEntity entity,
            final String... expCandidates) throws Exception {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.MILLISECONDS) <= 10000) {
            Optional<NormalizedNode<?, ?>> possible = dataStore.newReadOnlyTransaction()
                    .read(entityPath(entity.getType(), entity.getIdentifier()).node(Candidate.QNAME))
                    .get(5, TimeUnit.SECONDS);
            try {
                assertTrue("Candidates not found for " + entity, possible.isPresent());
                Collection<String> actual = new ArrayList<>();
                for (MapEntryNode candidate: ((MapNode)possible.get()).getValue()) {
                    actual.add(candidate.getChild(CANDIDATE_NAME_NODE_ID).get().getValue().toString());
                }

                assertEquals("Candidates for " + entity, Arrays.asList(expCandidates), actual);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void verifyOwner(final AbstractDataStore dataStore, final DOMEntity entity,
            final String expOwner) {
        AbstractEntityOwnershipTest.verifyOwner(expOwner, entity.getType(), entity.getIdentifier(), path -> {
            try {
                return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
            } catch (Exception e) {
                return null;
            }
        });
    }
}
