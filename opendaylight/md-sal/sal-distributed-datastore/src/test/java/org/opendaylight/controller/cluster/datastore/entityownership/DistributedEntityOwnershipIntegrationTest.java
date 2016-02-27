/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

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
import static org.opendaylight.controller.cluster.datastore.entityownership.AbstractEntityOwnershipTest.ownershipChange;
import static org.opendaylight.controller.cluster.datastore.entityownership.DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * End-to-end integration tests for the entity ownership functionality.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipIntegrationTest {
    private static final String MODULE_SHARDS_CONFIG = "module-shards-default.conf";
    private static final String MODULE_SHARDS_MEMBER_1_CONFIG = "module-shards-default-member-1.conf";
    private static final String ENTITY_TYPE1 = "entityType1";
    private static final String ENTITY_TYPE2 = "entityType2";
    private static final Entity ENTITY1 = new Entity(ENTITY_TYPE1, "entity1");
    private static final Entity ENTITY1_2 = new Entity(ENTITY_TYPE2, "entity1");
    private static final Entity ENTITY2 = new Entity(ENTITY_TYPE1, "entity2");
    private static final Entity ENTITY3 = new Entity(ENTITY_TYPE1, "entity3");
    private static final Entity ENTITY4 = new Entity(ENTITY_TYPE1, "entity4");
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5).
                    shardIsolatedLeaderCheckIntervalInMillis(1000000);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10000);

    private final List<MemberNode> memberNodes = new ArrayList<>();

    @Mock
    private EntityOwnershipListener leaderMockListener;

    @Mock
    private EntityOwnershipListener leaderMockListener2;

    @Mock
    private EntityOwnershipListener follower1MockListener;

    @Mock
    private EntityOwnershipListener follower2MockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @After
    public void tearDown() {
        for(MemberNode m: memberNodes) {
            m.cleanup();
        }
    }

    private static DistributedEntityOwnershipService newOwnershipService(final DistributedDataStore datastore) {
        return DistributedEntityOwnershipService.start(datastore.getActorContext(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    @Test
    public void testFunctionalityWithThreeNodes() throws Exception {
        String name = "test";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        DistributedDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        EntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        EntityOwnershipService follower1EntityOwnershipService = newOwnershipService(follower1Node.configDataStore());
        EntityOwnershipService follower2EntityOwnershipService = newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);
        leaderEntityOwnershipService.registerListener(ENTITY_TYPE2, leaderMockListener2);
        follower1EntityOwnershipService.registerListener(ENTITY_TYPE1, follower1MockListener);

        // Register leader candidate for entity1 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY1);
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, true, true));
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));
        reset(leaderMockListener, follower1MockListener);

        verifyGetOwnershipState(leaderEntityOwnershipService, ENTITY1, true, true);
        verifyGetOwnershipState(follower1EntityOwnershipService, ENTITY1, false, true);

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
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(leaderMockListener, never()).ownershipChanged(ownershipChange(ENTITY1));
        verify(follower1MockListener, never()).ownershipChanged(ownershipChange(ENTITY1));

        // Register follower1 candidate for entity2 and verify it becomes owner

        EntityOwnershipCandidateRegistration follower1Entity2Reg = follower1EntityOwnershipService.registerCandidate(ENTITY2);
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, true, true));
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, true));
        reset(leaderMockListener, follower1MockListener);

        // Register follower2 candidate for entity2 and verify it gets added but doesn't become owner

        follower2EntityOwnershipService.registerListener(ENTITY_TYPE1, follower2MockListener);
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, true));
        verify(follower2MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, false, true));

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

        // Shutdown follower2 and verify it's owned entities (entity 2 & 4) get re-assigned

        reset(leaderMockListener, follower1MockListener);
        follower2Node.cleanup();

        verify(follower1MockListener, timeout(15000).times(2)).ownershipChanged(or(ownershipChange(ENTITY4, false, true, true),
                ownershipChange(ENTITY2, false, false, false)));
        verify(leaderMockListener, timeout(15000).times(2)).ownershipChanged(or(ownershipChange(ENTITY4, false, false, true),
                ownershipChange(ENTITY2, false, false, false)));
        verifyOwner(leaderDistributedDataStore, ENTITY2, ""); // no other candidate

        // Register leader candidate for entity2 and verify it becomes owner

        EntityOwnershipCandidateRegistration leaderEntity2Reg = leaderEntityOwnershipService.registerCandidate(ENTITY2);
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, true, true));
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Unregister leader candidate for entity2 and verify the owner is cleared

        leaderEntity2Reg.close();
        verifyOwner(leaderDistributedDataStore, ENTITY2, "");
        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, true, false, false));
        verify(follower1MockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY2, false, false, false));
    }

    @Test
    public void testLeaderCandidatesRemovedAfterShutdown() throws Exception {
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(5).
                    customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        String name = "test";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        DistributedDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        EntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        EntityOwnershipService follower1EntityOwnershipService = newOwnershipService(follower1Node.configDataStore());
        EntityOwnershipService follower2EntityOwnershipService = newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);

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

        // Shutdown the leader and verify its removed from the candidate list

        leaderNode.cleanup();
        follower1Node.waitForMemberDown("member-1");

        // Re-enable elections on folower1 so it becomes the leader

        ActorRef follower1Shard = IntegrationTestKit.findLocalShard(follower1Node.configDataStore().
                getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);
        follower1Shard.tell(DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build()).
                customRaftPolicyImplementation(null).build(), ActorRef.noSender());

        MemberNode.verifyRaftState(follower1Node.configDataStore(), ENTITY_OWNERSHIP_SHARD_NAME,
            raftState -> assertEquals("Raft state", RaftState.Leader.toString(), raftState.getRaftState()));

        // Verify the prior leader's candidates are removed

        verifyCandidates(follower1Node.configDataStore(), ENTITY1, "member-2");
        verifyCandidates(follower1Node.configDataStore(), ENTITY2, "member-3");
        verifyOwner(follower1Node.configDataStore(), ENTITY1, "member-2");
        verifyOwner(follower1Node.configDataStore(), ENTITY2, "member-3");
    }

    /**
     * Reproduces bug <a href="https://bugs.opendaylight.org/show_bug.cgi?id=4554">4554</a>
     *
     * @throws CandidateAlreadyRegisteredException
     */
    @Test
    public void testCloseCandidateRegistrationInQuickSuccession() throws CandidateAlreadyRegisteredException {
        String name = "testCloseCandidateRegistrationInQuickSuccession";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name ).
                moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        DistributedDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        EntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        EntityOwnershipService follower1EntityOwnershipService = newOwnershipService(follower1Node.configDataStore());
        EntityOwnershipService follower2EntityOwnershipService = newOwnershipService(follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);
        follower1EntityOwnershipService.registerListener(ENTITY_TYPE1, follower1MockListener);
        follower2EntityOwnershipService.registerListener(ENTITY_TYPE1, follower2MockListener);

        final EntityOwnershipCandidateRegistration candidate1 = leaderEntityOwnershipService.registerCandidate(ENTITY1);
        final EntityOwnershipCandidateRegistration candidate2 = follower1EntityOwnershipService.registerCandidate(ENTITY1);
        final EntityOwnershipCandidateRegistration candidate3 = follower2EntityOwnershipService.registerCandidate(ENTITY1);

        verify(leaderMockListener, timeout(5000)).ownershipChanged(ownershipChange(ENTITY1, false, true, true));

        Mockito.reset(leaderMockListener);

        ArgumentCaptor<EntityOwnershipChange> leaderChangeCaptor = ArgumentCaptor.forClass(EntityOwnershipChange.class);
        ArgumentCaptor<EntityOwnershipChange> follower1ChangeCaptor = ArgumentCaptor.forClass(EntityOwnershipChange.class);
        ArgumentCaptor<EntityOwnershipChange> follower2ChangeCaptor = ArgumentCaptor.forClass(EntityOwnershipChange.class);
        doNothing().when(leaderMockListener).ownershipChanged(leaderChangeCaptor.capture());
        doNothing().when(follower1MockListener).ownershipChanged(follower1ChangeCaptor.capture());
        doNothing().when(follower2MockListener).ownershipChanged(follower2ChangeCaptor.capture());

        candidate1.close();
        candidate2.close();
        candidate3.close();

        boolean passed = false;
        for(int i=0;i<100;i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            if(!leaderEntityOwnershipService.getOwnershipState(ENTITY1).isPresent() ||
                    !leaderEntityOwnershipService.getOwnershipState(ENTITY1).get().hasOwner() &&
                    follower1EntityOwnershipService.getOwnershipState(ENTITY1).isPresent() &&
                    !follower1EntityOwnershipService.getOwnershipState(ENTITY1).get().hasOwner() &&
                    follower2EntityOwnershipService.getOwnershipState(ENTITY1).isPresent() &&
                    !follower2EntityOwnershipService.getOwnershipState(ENTITY1).get().hasOwner() &&
                    leaderChangeCaptor.getAllValues().size() > 0 && !leaderChangeCaptor.getValue().hasOwner() &&
                    leaderChangeCaptor.getAllValues().size() > 0 && !follower1ChangeCaptor.getValue().hasOwner() &&
                    leaderChangeCaptor.getAllValues().size() > 0 && !follower2ChangeCaptor.getValue().hasOwner()) {
                passed = true;
                break;
            }
        }

        assertTrue("No ownership change message was sent with hasOwner=false", passed);
    }

    /**
     * Tests bootstrapping the entity-ownership shard when there's no shards initially configured for local
     * member. The entity-ownership shard is initially created as inactive (ie remains a follower), requiring
     * an AddShardReplica request to join it to an existing leader.
     */
    @Test
    public void testEntityOwnershipShardBootstrapping() throws Throwable {
        String name = "testEntityOwnershipShardBootstrapping";
        String moduleShardsConfig = MODULE_SHARDS_MEMBER_1_CONFIG;
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        DistributedDataStore leaderDistributedDataStore = leaderNode.configDataStore();
        EntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name ).
                moduleShardsConfig(moduleShardsConfig).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();

        DistributedDataStore follower1DistributedDataStore = follower1Node.configDataStore();
        follower1DistributedDataStore.waitTillReady();

        leaderNode.waitForMembersUp("member-2");
        follower1Node.waitForMembersUp("member-1");

        EntityOwnershipService follower1EntityOwnershipService = newOwnershipService(follower1DistributedDataStore);

        leaderEntityOwnershipService.registerListener(ENTITY_TYPE1, leaderMockListener);

        // Register a candidate for follower1 - should get queued since follower1 has no leader
        EntityOwnershipCandidateRegistration candidateReg = follower1EntityOwnershipService.registerCandidate(ENTITY1);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(leaderMockListener, never()).ownershipChanged(ownershipChange(ENTITY1));

        // Add replica in follower1
        AddShardReplica addReplica = new AddShardReplica(ENTITY_OWNERSHIP_SHARD_NAME);
        follower1DistributedDataStore.getActorContext().getShardManager().tell(addReplica , follower1Node.kit().getRef());
        Object reply = follower1Node.kit().expectMsgAnyClassOf(JavaTestKit.duration("5 sec"), Success.class, Failure.class);
        if(reply instanceof Failure) {
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

        follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name ).
                moduleShardsConfig(moduleShardsConfig).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false).
                datastoreContextBuilder(followerDatastoreContextBuilder).build();
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

    private static void verifyGetOwnershipState(final EntityOwnershipService service, final Entity entity,
            final boolean isOwner, final boolean hasOwner) {
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertEquals("getOwnershipState present", true, state.isPresent());
        assertEquals("isOwner", isOwner, state.get().isOwner());
        assertEquals("hasOwner", hasOwner, state.get().hasOwner());
    }

    private static void verifyCandidates(final DistributedDataStore dataStore, final Entity entity, final String... expCandidates) throws Exception {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 10000) {
            Optional<NormalizedNode<?, ?>> possible = dataStore.newReadOnlyTransaction().read(
                    entityPath(entity.getType(), entity.getId()).node(Candidate.QNAME)).get(5, TimeUnit.SECONDS);
            try {
                assertEquals("Candidates not found for " + entity, true, possible.isPresent());
                Collection<String> actual = new ArrayList<>();
                for(MapEntryNode candidate: ((MapNode)possible.get()).getValue()) {
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

    private static void verifyOwner(final DistributedDataStore dataStore, final Entity entity, final String expOwner) {
        AbstractEntityOwnershipTest.verifyOwner(expOwner, entity.getType(), entity.getId(),
                path -> {
                    try {
                        return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
                    } catch (Exception e) {
                        return null;
                    }
                });
    }
}
