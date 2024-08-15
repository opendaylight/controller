/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyNoShardPresent;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyRaftPeersPresent;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyRaftState;

import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Status.Success;
import org.apache.pekko.cluster.Cluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.datastore.MemberNode.RaftStateVerifier;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForShardInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.FlipMemberVotingStatesForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.MakeLeaderLocalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.member.voting.states.input.MemberVotingStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Unit tests for ClusterAdminRpcService.
 *
 * @author Thomas Pantelis
 */
public class ClusterAdminRpcServiceTest {
    record ExpState(String name, boolean voting) {
        ExpState {
            requireNonNull(name);
        }
    }

    private static final MemberName MEMBER_1 = MemberName.forName("member-1");
    private static final MemberName MEMBER_2 = MemberName.forName("member-2");
    private static final MemberName MEMBER_3 = MemberName.forName("member-3");
    private final List<MemberNode> memberNodes = new ArrayList<>();

    @Before
    public void setUp() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @After
    public void tearDown() {
        for (var member : Lists.reverse(memberNodes)) {
            member.cleanup();
        }
        memberNodes.clear();
    }

    @Test
    public void testBackupDatastore() throws Exception {
        final var node = MemberNode.builder(memberNodes)
            .akkaConfig("Member1")
            .moduleShardsConfig("module-shards-member1.conf")
            .waitForShardLeader("cars", "people")
            .testName("testBackupDatastore")
            .build();

        final var fileName = "target/testBackupDatastore";
        final var file = new File(fileName);
        file.delete();

        final var service = new ClusterAdminRpcService(node.configDataStore(), node.operDataStore(), null);

        var rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().setFilePath(fileName).build())
            .get(5, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        try (var fis = Files.newInputStream(file.toPath())) {
            final List<DatastoreSnapshot> snapshots = SerializationUtils.deserialize(fis);
            assertEquals("DatastoreSnapshot size", 2, snapshots.size());

            final var map = Map.of(
                snapshots.get(0).getType(), snapshots.get(0),
                snapshots.get(1).getType(), snapshots.get(1));
            verifyDatastoreSnapshot(node.configDataStore().getActorUtils().getDataStoreName(),
                    map.get(node.configDataStore().getActorUtils().getDataStoreName()), "cars", "people");
        } finally {
            new File(fileName).delete();
        }

        // Test failure by killing a shard.

        node.configDataStore().getActorUtils().getShardManager().tell(node.datastoreContextBuilder()
                .shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(), ActorRef.noSender());

        final var carsShardActor = node.configDataStore().getActorUtils().findLocalShard("cars").orElseThrow();
        node.kit().watch(carsShardActor);
        carsShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        node.kit().expectTerminated(carsShardActor);

        rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().setFilePath(fileName).build())
                .get(5, TimeUnit.SECONDS);
        assertFalse("isSuccessful", rpcResult.isSuccessful());
        assertEquals("getErrors", 1, rpcResult.getErrors().size());
    }

    private static void verifyDatastoreSnapshot(final String type, final DatastoreSnapshot datastoreSnapshot,
            final String... expShardNames) {
        assertNotNull("Missing DatastoreSnapshot for type " + type, datastoreSnapshot);
        var shardNames = new HashSet<String>();
        for (var snapshot : datastoreSnapshot.getShardSnapshots()) {
            shardNames.add(snapshot.getName());
        }

        assertEquals("DatastoreSnapshot shard names", Set.of(expShardNames), shardNames);
    }

    @Test
    public void testGetPrefixShardRole() throws Exception {
        String name = "testGetPrefixShardRole";
        String moduleShardsConfig = "module-shards-default-member-1.conf";

        final var member1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        member1.kit().waitUntilLeader(member1.configDataStore().getActorUtils(), "default");
    }

    @Test
    public void testModuleShardLeaderMovement() throws Exception {
        String name = "testModuleShardLeaderMovement";
        String moduleShardsConfig = "module-shards-member1.conf";

        final var member1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .waitForShardLeader("cars").moduleShardsConfig(moduleShardsConfig).build();
        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();
        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        member1.waitForMembersUp("member-2", "member-3");
        replicaNode2.waitForMembersUp("member-1");
        replicaNode3.waitForMembersUp("member-1", "member-2");

        doAddShardReplica(replicaNode2, "cars", "member-1");
        doAddShardReplica(replicaNode3, "cars", "member-1", "member-2");

        verifyRaftPeersPresent(member1.configDataStore(), "cars", "member-2", "member-3");

        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");

        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        doMakeShardLeaderLocal(member1, "cars", "member-1");
        verifyRaftState(replicaNode2.configDataStore(), "cars",
            raftState -> assertThat(raftState.getLeader(),containsString("member-1")));
        verifyRaftState(replicaNode3.configDataStore(), "cars",
            raftState -> assertThat(raftState.getLeader(),containsString("member-1")));

        doMakeShardLeaderLocal(replicaNode2, "cars", "member-2");
        verifyRaftState(member1.configDataStore(), "cars",
            raftState -> assertThat(raftState.getLeader(),containsString("member-2")));
        verifyRaftState(replicaNode3.configDataStore(), "cars",
            raftState -> assertThat(raftState.getLeader(),containsString("member-2")));

        replicaNode2.waitForMembersUp("member-3");
        doMakeShardLeaderLocal(replicaNode3, "cars", "member-3");
    }

    @Test
    public void testAddShardReplica() throws Exception {
        String name = "testAddShardReplica";
        String moduleShardsConfig = "module-shards-cars-member-1.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).waitForShardLeader("cars").build();

        final var newReplicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-2");

        doAddShardReplica(newReplicaNode2, "cars", "member-1");

        var newReplicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-3");
        newReplicaNode2.waitForMembersUp("member-3");

        doAddShardReplica(newReplicaNode3, "cars", "member-1", "member-2");

        verifyRaftPeersPresent(newReplicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(newReplicaNode2.operDataStore(), "cars", "member-1", "member-3");

        // Write data to member-2's config datastore and read/verify via member-3
        final var configCarsNode = writeCarsNodeAndVerify(newReplicaNode2.configDataStore(),
                newReplicaNode3.configDataStore());

        // Write data to member-3's oper datastore and read/verify via member-2
        writeCarsNodeAndVerify(newReplicaNode3.operDataStore(), newReplicaNode2.operDataStore());

        // Verify all data has been replicated. We expect 4 log entries and thus last applied index of 3 -
        // 2 ServerConfigurationPayload entries, the transaction payload entry plus a purge payload.

        RaftStateVerifier verifier = raftState -> {
            assertEquals("Commit index", 3, raftState.getCommitIndex());
            assertEquals("Last applied index", 3, raftState.getLastApplied());
        };

        verifyRaftState(leaderNode1.configDataStore(), "cars", verifier);
        verifyRaftState(leaderNode1.operDataStore(), "cars", verifier);

        verifyRaftState(newReplicaNode2.configDataStore(), "cars", verifier);
        verifyRaftState(newReplicaNode2.operDataStore(), "cars", verifier);

        verifyRaftState(newReplicaNode3.configDataStore(), "cars", verifier);
        verifyRaftState(newReplicaNode3.operDataStore(), "cars", verifier);

        // Restart member-3 and verify the cars config shard is re-instated.

        Cluster.get(leaderNode1.kit().getSystem()).down(Cluster.get(newReplicaNode3.kit().getSystem()).selfAddress());
        newReplicaNode3.cleanup();

        newReplicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).createOperDatastore(false).build();

        verifyRaftState(newReplicaNode3.configDataStore(), "cars", verifier);
        readCarsNodeAndVerify(newReplicaNode3.configDataStore(), configCarsNode);
    }

    @Test
    public void testAddShardReplicaFailures() throws Exception {
        String name = "testAddShardReplicaFailures";
        final var memberNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig("module-shards-cars-member-1.conf").build();

        final var service = new ClusterAdminRpcService(memberNode.configDataStore(), memberNode.operDataStore(), null);

        var rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder()
                .setDataStoreType(DataStoreType.Config)
                .build())
            .get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName("cars").build())
            .get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName("people")
                .setDataStoreType(DataStoreType.Config)
                .build())
            .get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);
    }

    private static ContainerNode writeCarsNodeAndVerify(final AbstractDataStore writeToStore,
            final AbstractDataStore readFromStore) throws Exception {
        final var writeTx = writeToStore.newWriteOnlyTransaction();
        final var carsNode = CarsModel.create();
        writeTx.write(CarsModel.BASE_PATH, carsNode);

        final var cohort = writeTx.ready();
        assertEquals("canCommit", TRUE, cohort.canCommit().get(7, TimeUnit.SECONDS));
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);

        readCarsNodeAndVerify(readFromStore, carsNode);
        return carsNode;
    }

    private static void readCarsNodeAndVerify(final AbstractDataStore readFromStore,
            final ContainerNode expCarsNode) throws Exception {
        assertEquals(Optional.of(expCarsNode),
            readFromStore.newReadOnlyTransaction().read(CarsModel.BASE_PATH).get(15, TimeUnit.SECONDS));
    }

    private static void doAddShardReplica(final MemberNode memberNode, final String shardName,
            final String... peerMemberNames) throws Exception {
        memberNode.waitForMembersUp(peerMemberNames);

        final var service = new ClusterAdminRpcService(memberNode.configDataStore(), memberNode.operDataStore(), null);

        var rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder()
            .setShardName(shardName)
            .setDataStoreType(DataStoreType.Config)
            .build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(memberNode.configDataStore(), shardName, peerMemberNames);

        assertEquals(Optional.empty(), memberNode.operDataStore().getActorUtils().findLocalShard(shardName));

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder()
            .setShardName(shardName)
            .setDataStoreType(DataStoreType.Operational)
            .build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(memberNode.operDataStore(), shardName, peerMemberNames);
    }

    private static void doMakeShardLeaderLocal(final MemberNode memberNode, final String shardName,
            final String newLeader) throws Exception {
        final var service = new ClusterAdminRpcService(memberNode.configDataStore(), memberNode.operDataStore(), null);

        final var rpcResult = service.makeLeaderLocal(new MakeLeaderLocalInputBuilder()
            .setDataStoreType(DataStoreType.Config)
            .setShardName(shardName)
            .build()).get(10, TimeUnit.SECONDS);

        verifySuccessfulRpcResult(rpcResult);

        verifyRaftState(memberNode.configDataStore(), shardName, raftState -> assertThat(raftState.getLeader(),
                containsString(newLeader)));
    }

    private static <T> T verifySuccessfulRpcResult(final RpcResult<T> rpcResult) {
        if (!rpcResult.isSuccessful()) {
            final var errors = rpcResult.getErrors();
            if (errors.size() > 0) {
                final var error = errors.get(0);
                throw new AssertionError("Rpc failed with error: " + error, error.getCause());
            }

            fail("Rpc failed with no error");
        }

        return rpcResult.getResult();
    }

    private static void verifyFailedRpcResult(final RpcResult<?> rpcResult) {
        assertFalse("RpcResult", rpcResult.isSuccessful());
        final var errors = rpcResult.getErrors();
        assertEquals("RpcResult errors size", 1, errors.size());
        final var error = errors.get(0);
        assertNotNull("RpcResult error message null", error.getMessage());
    }

    @Test
    public void testRemoveShardReplica() throws Exception {
        String name = "testRemoveShardReplica";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        replicaNode3.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        // Invoke RPC service on member-3 to remove it's local shard

        final var service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(), replicaNode3.operDataStore(),
            null);

        var rpcResult = service3.removeShardReplica(new RemoveShardReplicaInputBuilder()
            .setShardName("cars").setMemberName("member-3")
            .setDataStoreType(DataStoreType.Config)
            .build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1");
        verifyNoShardPresent(replicaNode3.configDataStore(), "cars");

        // Restart member-2 and verify member-3 isn't present.

        Cluster.get(leaderNode1.kit().getSystem()).down(Cluster.get(replicaNode2.kit().getSystem()).selfAddress());
        replicaNode2.cleanup();

        final var newPeplicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        newPeplicaNode2.configDataStore().waitTillReady();
        verifyRaftPeersPresent(newPeplicaNode2.configDataStore(), "cars", "member-1");

        // Invoke RPC service on member-1 to remove member-2

        final var service1 = new ClusterAdminRpcService(leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            null);

        rpcResult = service1.removeShardReplica(new RemoveShardReplicaInputBuilder()
            .setShardName("cars")
            .setMemberName("member-2")
            .setDataStoreType(DataStoreType.Config)
            .build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars");
        verifyNoShardPresent(newPeplicaNode2.configDataStore(), "cars");
    }

    @Test
    public void testRemoveShardLeaderReplica() throws Exception {
        String name = "testRemoveShardLeaderReplica";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        replicaNode2.waitForMembersUp("member-1", "member-3");
        replicaNode3.waitForMembersUp("member-1", "member-2");

        // Invoke RPC service on leader member-1 to remove it's local shard

        final var service1 = new ClusterAdminRpcService(leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            null);

        final var rpcResult = service1.removeShardReplica(new RemoveShardReplicaInputBuilder()
            .setShardName("cars")
            .setMemberName("member-1")
            .setDataStoreType(DataStoreType.Config)
            .build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftState(replicaNode2.configDataStore(), "cars", raftState ->
                assertThat("Leader Id", raftState.getLeader(), anyOf(containsString("member-2"),
                        containsString("member-3"))));

        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-2");
        verifyNoShardPresent(leaderNode1.configDataStore(), "cars");
    }

    @Test
    public void testAddReplicasForAllShards() throws Exception {
        String name = "testAddReplicasForAllShards";
        String moduleShardsConfig = "module-shards-member1.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).waitForShardLeader("cars", "people").build();

        final var petsModuleConfig = new ModuleShardConfiguration(XMLNamespace.of("pets-ns"), "pets-module", "pets",
            null, List.of(MEMBER_1));
        leaderNode1.configDataStore().getActorUtils().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), leaderNode1.kit().getRef());
        leaderNode1.kit().expectMsgClass(Success.class);
        leaderNode1.kit().waitUntilLeader(leaderNode1.configDataStore().getActorUtils(), "pets");

        final var newReplicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-2");
        newReplicaNode2.waitForMembersUp("member-1");

        newReplicaNode2.configDataStore().getActorUtils().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), newReplicaNode2.kit().getRef());
        newReplicaNode2.kit().expectMsgClass(Success.class);

        newReplicaNode2.operDataStore().getActorUtils().getShardManager()
            .tell(new CreateShard(new ModuleShardConfiguration(XMLNamespace.of("no-leader-ns"), "no-leader-module",
                "no-leader", null, List.of(MEMBER_1)),
                Shard.builder(), null), newReplicaNode2.kit().getRef());
        newReplicaNode2.kit().expectMsgClass(Success.class);

        final var service = new ClusterAdminRpcService(newReplicaNode2.configDataStore(),
            newReplicaNode2.operDataStore(), null);

        var rpcResult = service.addReplicasForAllShards(new AddReplicasForAllShardsInputBuilder().build())
            .get(10, TimeUnit.SECONDS);
        final var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("pets", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational),
                failedShardResult("no-leader", DataStoreType.Operational));

        verifyRaftPeersPresent(newReplicaNode2.configDataStore(), "cars", "member-1");
        verifyRaftPeersPresent(newReplicaNode2.configDataStore(), "people", "member-1");
        verifyRaftPeersPresent(newReplicaNode2.configDataStore(), "pets", "member-1");
        verifyRaftPeersPresent(newReplicaNode2.operDataStore(), "cars", "member-1");
        verifyRaftPeersPresent(newReplicaNode2.operDataStore(), "people", "member-1");
    }

    @Test
    public void testRemoveAllShardReplicas() throws Exception {
        String name = "testRemoveAllShardReplicas";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        final var petsModuleConfig = new ModuleShardConfiguration(XMLNamespace.of("pets-ns"), "pets-module", "pets",
            null, List.of(MEMBER_1, MEMBER_2, MEMBER_3));
        leaderNode1.configDataStore().getActorUtils().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), leaderNode1.kit().getRef());
        leaderNode1.kit().expectMsgClass(Success.class);

        replicaNode2.configDataStore().getActorUtils().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), replicaNode2.kit().getRef());
        replicaNode2.kit().expectMsgClass(Success.class);

        replicaNode3.configDataStore().getActorUtils().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), replicaNode3.kit().getRef());
        replicaNode3.kit().expectMsgClass(Success.class);

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "pets", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "pets", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "pets", "member-1", "member-2");

        final var service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(), replicaNode3.operDataStore(),
            null);

        var rpcResult = service3.removeAllShardReplicas(
                new RemoveAllShardReplicasInputBuilder().setMemberName("member-3").build())
            .get(10, TimeUnit.SECONDS);
        final var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("pets", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2");
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "people", "member-2");
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "pets", "member-2");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "people", "member-1");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "pets", "member-1");
        verifyNoShardPresent(replicaNode3.configDataStore(), "cars");
        verifyNoShardPresent(replicaNode3.configDataStore(), "people");
        verifyNoShardPresent(replicaNode3.configDataStore(), "pets");
    }

    @Test
    public void testChangeMemberVotingStatesForShard() throws Exception {
        String name = "testChangeMemberVotingStatusForShard";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        replicaNode3.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        // Invoke RPC service on member-3 to change voting status

        final var service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(), replicaNode3.operDataStore(),
            null);

        var rpcResult = service3.changeMemberVotingStatesForShard(new ChangeMemberVotingStatesForShardInputBuilder()
            .setShardName("cars").setDataStoreType(DataStoreType.Config)
            .setMemberVotingState(List.of(
                new MemberVotingStateBuilder().setMemberName("member-2").setVoting(FALSE).build(),
                new MemberVotingStateBuilder().setMemberName("member-3").setVoting(FALSE).build()))
            .build())
            .get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyVotingStates(leaderNode1.configDataStore(), "cars",
            new ExpState("member-1", true), new ExpState("member-2", false), new ExpState("member-3", false));
        verifyVotingStates(replicaNode2.configDataStore(), "cars",
            new ExpState("member-1", true), new ExpState("member-2", false), new ExpState("member-3", false));
        verifyVotingStates(replicaNode3.configDataStore(), "cars",
            new ExpState("member-1", true), new ExpState("member-2", false), new ExpState("member-3", false));
    }

    @Test
    public void testChangeMemberVotingStatesForSingleNodeShard() throws Exception {
        String name = "testChangeMemberVotingStatesForSingleNodeShard";
        String moduleShardsConfig = "module-shards-member1.conf";
        final var leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        leaderNode.configDataStore().waitTillReady();

        // Invoke RPC service on member-3 to change voting status

        final var service = new ClusterAdminRpcService(leaderNode.configDataStore(), leaderNode.operDataStore(), null);

        final var rpcResult = service.changeMemberVotingStatesForShard(
            new ChangeMemberVotingStatesForShardInputBuilder()
                .setShardName("cars").setDataStoreType(DataStoreType.Config)
                .setMemberVotingState(List.of(new MemberVotingStateBuilder()
                    .setMemberName("member-1")
                    .setVoting(FALSE)
                    .build()))
                .build())
            .get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        verifyVotingStates(leaderNode.configDataStore(), "cars", new ExpState("member-1", true));
    }

    @Test
    public void testChangeMemberVotingStatesForAllShards() throws Exception {
        String name = "testChangeMemberVotingStatesForAllShards";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes)
            .akkaConfig("Member1")
            .testName(name)
            .moduleShardsConfig(moduleShardsConfig)
            .datastoreContextBuilder(DatastoreContext.newBuilder()
                .shardHeartbeatIntervalInMillis(300)
                .shardElectionTimeoutFactor(1))
            .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        leaderNode1.operDataStore().waitTillReady();
        replicaNode3.configDataStore().waitTillReady();
        replicaNode3.operDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        // Invoke RPC service on member-3 to change voting status

        final var service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(),
                replicaNode3.operDataStore(), null);

        final var rpcResult = service3.changeMemberVotingStatesForAllShards(
            new ChangeMemberVotingStatesForAllShardsInputBuilder()
                .setMemberVotingState(List.of(
                        new MemberVotingStateBuilder().setMemberName("member-2").setVoting(FALSE).build(),
                        new MemberVotingStateBuilder().setMemberName("member-3").setVoting(FALSE).build()))
                .build())
                .get(10, TimeUnit.SECONDS);
        final var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        verifyVotingStates(new ClientBackedDataStore[] {
            leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            replicaNode2.configDataStore(), replicaNode2.operDataStore(),
            replicaNode3.configDataStore(), replicaNode3.operDataStore()
        }, new String[] { "cars", "people" },
            new ExpState("member-1", true), new ExpState("member-2", false), new ExpState("member-3", false));
    }

    @Test
    public void testFlipMemberVotingStates() throws Exception {
        String name = "testFlipMemberVotingStates";

        final var persistedServerConfig = new ServerConfigurationPayload(List.of(
            new ServerInfo("member-1", true), new ServerInfo("member-2", true), new ServerInfo("member-3", false)));

        setupPersistedServerConfigPayload(persistedServerConfig, "member-1", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-2", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-3", name, "cars", "people");

        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(DatastoreContext.newBuilder()
                        .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        leaderNode1.operDataStore().waitTillReady();
        replicaNode3.configDataStore().waitTillReady();
        replicaNode3.operDataStore().waitTillReady();
        verifyVotingStates(leaderNode1.configDataStore(), "cars",
            new ExpState("member-1", true), new ExpState("member-2", true), new ExpState("member-3", false));

        final var service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(), replicaNode3.operDataStore(),
            null);

        var rpcResult = service3.flipMemberVotingStatesForAllShards(
            new FlipMemberVotingStatesForAllShardsInputBuilder().build())
            .get(10, TimeUnit.SECONDS);
        var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        verifyVotingStates(new ClientBackedDataStore[] {
            leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            replicaNode2.configDataStore(), replicaNode2.operDataStore(),
            replicaNode3.configDataStore(), replicaNode3.operDataStore()
        }, new String[] { "cars", "people" },
            new ExpState("member-1", false), new ExpState("member-2", false), new ExpState("member-3", true));

        // Leadership should have transferred to member 3 since it is the only remaining voting member.
        verifyRaftState(leaderNode1.configDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-3. Actual: " + raftState.getLeader(),
                    raftState.getLeader().contains("member-3"));
        });

        verifyRaftState(leaderNode1.operDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-3. Actual: " + raftState.getLeader(),
                    raftState.getLeader().contains("member-3"));
        });

        // Flip the voting states back to the original states.

        rpcResult = service3.flipMemberVotingStatesForAllShards(
            new FlipMemberVotingStatesForAllShardsInputBuilder().build())
            .get(10, TimeUnit.SECONDS);
        result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        verifyVotingStates(new ClientBackedDataStore[] {
            leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            replicaNode2.configDataStore(), replicaNode2.operDataStore(),
            replicaNode3.configDataStore(), replicaNode3.operDataStore()
        }, new String[] { "cars", "people" },
            new ExpState("member-1", true), new ExpState("member-2", true), new ExpState("member-3", false));

        // Leadership should have transferred to member 1 or 2.
        verifyRaftState(leaderNode1.configDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-1 or member-2. Actual: " + raftState.getLeader(),
                    raftState.getLeader().contains("member-1") || raftState.getLeader().contains("member-2"));
        });
    }

    @Test
    public void testFlipMemberVotingStatesWithNoInitialLeader() throws Exception {
        String name = "testFlipMemberVotingStatesWithNoInitialLeader";

        // Members 1, 2, and 3 are initially started up as non-voting. Members 4, 5, and 6 are initially
        // non-voting and simulated as down by not starting them up.
        final var persistedServerConfig = new ServerConfigurationPayload(List.of(
                new ServerInfo("member-1", false), new ServerInfo("member-2", false),
                new ServerInfo("member-3", false), new ServerInfo("member-4", true),
                new ServerInfo("member-5", true), new ServerInfo("member-6", true)));

        setupPersistedServerConfigPayload(persistedServerConfig, "member-1", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-2", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-3", name, "cars", "people");

        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var replicaNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        // Initially there won't be a leader b/c all the up nodes are non-voting.

        replicaNode1.waitForMembersUp("member-2", "member-3");

        verifyVotingStates(replicaNode1.configDataStore(), "cars",
            new ExpState("member-1", false), new ExpState("member-2", false), new ExpState("member-3", false),
            new ExpState("member-4", true), new ExpState("member-5", true), new ExpState("member-6", true));

        verifyRaftState(replicaNode1.configDataStore(), "cars", raftState ->
            assertEquals("Expected raft state", RaftState.Follower.toString(), raftState.getRaftState()));

        final var service1 = new ClusterAdminRpcService(replicaNode1.configDataStore(), replicaNode1.operDataStore(),
            null);

        final var rpcResult = service1.flipMemberVotingStatesForAllShards(
            new FlipMemberVotingStatesForAllShardsInputBuilder().build())
            .get(10, TimeUnit.SECONDS);
        final var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        verifyVotingStates(new ClientBackedDataStore[] {
            replicaNode1.configDataStore(), replicaNode1.operDataStore(),
            replicaNode2.configDataStore(), replicaNode2.operDataStore(),
            replicaNode3.configDataStore(), replicaNode3.operDataStore()
        }, new String[] { "cars", "people" },
            new ExpState("member-1", true), new ExpState("member-2", true), new ExpState("member-3", true),
            new ExpState("member-4", false), new ExpState("member-5", false), new ExpState("member-6", false));

        // Since member 1 was changed to voting and there was no leader, it should've started and election
        // and become leader
        verifyRaftState(replicaNode1.configDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-1. Actual: " + raftState.getLeader(),
                    raftState.getLeader().contains("member-1"));
        });

        verifyRaftState(replicaNode1.operDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-1. Actual: " + raftState.getLeader(),
                    raftState.getLeader().contains("member-1"));
        });
    }

    @Test
    public void testFlipMemberVotingStatesWithVotingMembersDown() throws Exception {
        String name = "testFlipMemberVotingStatesWithVotingMembersDown";

        // Members 4, 5, and 6 are initially non-voting and simulated as down by not starting them up.
        final var persistedServerConfig = new ServerConfigurationPayload(List.of(
                new ServerInfo("member-1", true), new ServerInfo("member-2", true),
                new ServerInfo("member-3", true), new ServerInfo("member-4", false),
                new ServerInfo("member-5", false), new ServerInfo("member-6", false)));

        setupPersistedServerConfigPayload(persistedServerConfig, "member-1", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-2", name, "cars", "people");
        setupPersistedServerConfigPayload(persistedServerConfig, "member-3", name, "cars", "people");

        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        final var leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(
                        DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1))
                .build();

        final var replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        final var replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        leaderNode1.operDataStore().waitTillReady();
        verifyVotingStates(leaderNode1.configDataStore(), "cars",
            new ExpState("member-1", true), new ExpState("member-2", true), new ExpState("member-3", true),
            new ExpState("member-4", false), new ExpState("member-5", false), new ExpState("member-6", false));

        final var service1 = new ClusterAdminRpcService(leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            null);

        final var rpcResult = service1.flipMemberVotingStatesForAllShards(
            new FlipMemberVotingStatesForAllShardsInputBuilder().build())
            .get(10, TimeUnit.SECONDS);
        final var result = verifySuccessfulRpcResult(rpcResult);
        verifyShardResults(result.getShardResult(), successShardResult("cars", DataStoreType.Config),
                successShardResult("people", DataStoreType.Config),
                successShardResult("cars", DataStoreType.Operational),
                successShardResult("people", DataStoreType.Operational));

        // Members 2 and 3 are now non-voting but should get replicated with the new new server config.
        verifyVotingStates(new ClientBackedDataStore[] {
            leaderNode1.configDataStore(), leaderNode1.operDataStore(),
            replicaNode2.configDataStore(), replicaNode2.operDataStore(),
            replicaNode3.configDataStore(), replicaNode3.operDataStore()
        }, new String[] { "cars", "people" },
            new ExpState("member-1", false), new ExpState("member-2", false), new ExpState("member-3", false),
            new ExpState("member-4", true), new ExpState("member-5", true), new ExpState("member-6", true));

        // The leader (member 1) was changed to non-voting but it shouldn't be able to step down as leader yet
        // b/c it can't get a majority consensus with all voting members down. So verify it remains the leader.
        verifyRaftState(leaderNode1.configDataStore(), "cars", raftState -> {
            assertNotNull("Expected non-null leader Id", raftState.getLeader());
            assertTrue("Expected leader member-1", raftState.getLeader().contains("member-1"));
        });
    }

    private static void setupPersistedServerConfigPayload(final ServerConfigurationPayload serverConfig,
            final String member, final String datastoreTypeSuffix, final String... shards) {
        String[] datastoreTypes = { "config_", "oper_" };
        for (String type : datastoreTypes) {
            for (String shard : shards) {
                final var newServerInfo = new ArrayList<ServerInfo>(serverConfig.getServerConfig().size());
                for (var info : serverConfig.getServerConfig()) {
                    newServerInfo.add(new ServerInfo(ShardIdentifier.create(shard, MemberName.forName(info.peerId()),
                            type + datastoreTypeSuffix).toString(), info.isVoting()));
                }

                final String shardID = ShardIdentifier.create(shard, MemberName.forName(member),
                        type + datastoreTypeSuffix).toString();
                InMemoryJournal.addEntry(shardID, 1, new UpdateElectionTerm(1, null));
                InMemoryJournal.addEntry(shardID, 2, new SimpleReplicatedLogEntry(0, 1,
                        new ServerConfigurationPayload(newServerInfo)));
            }
        }
    }

    private static void verifyVotingStates(final ClientBackedDataStore[] datastores, final String[] shards,
            final ExpState... expStates) throws Exception {
        for (var datastore : datastores) {
            for (String shard : shards) {
                verifyVotingStates(datastore, shard, expStates);
            }
        }
    }

    private static void verifyVotingStates(final ClientBackedDataStore datastore, final String shardName,
            final ExpState... expStates) throws Exception {
        String localMemberName = datastore.getActorUtils().getCurrentMemberName().getName();
        var expStateMap = new HashMap<String, Boolean>();
        for (var expState : expStates) {
            expStateMap.put(ShardIdentifier.create(shardName, MemberName.forName(expState.name),
                datastore.getActorUtils().getDataStoreName()).toString(), expState.voting);
        }

        verifyRaftState(datastore, shardName, raftState -> {
            String localPeerId = ShardIdentifier.create(shardName, MemberName.forName(localMemberName),
                    datastore.getActorUtils().getDataStoreName()).toString();
            assertEquals("Voting state for " + localPeerId, expStateMap.get(localPeerId), raftState.isVoting());
            for (var entry : raftState.getPeerVotingStates().entrySet()) {
                assertEquals("Voting state for " + entry.getKey(), expStateMap.get(entry.getKey()), entry.getValue());
            }
        });
    }

    private static void verifyShardResults(final Map<ShardResultKey, ShardResult> shardResults,
            final ShardResult... expShardResults) {
        var expResultsMap = new HashMap<String, ShardResult>();
        for (var r : expShardResults) {
            expResultsMap.put(r.getShardName() + "-" + r.getDataStoreType(), r);
        }

        for (var result : shardResults.values()) {
            var exp = expResultsMap.remove(result.getShardName() + "-" + result.getDataStoreType());
            assertNotNull(String.format("Unexpected result for shard %s, type %s", result.getShardName(),
                    result.getDataStoreType()), exp);
            assertEquals("isSucceeded", exp.getSucceeded(), result.getSucceeded());
            if (exp.getSucceeded()) {
                assertNull("Expected null error message", result.getErrorMessage());
            } else {
                assertNotNull("Expected error message", result.getErrorMessage());
            }
        }

        if (!expResultsMap.isEmpty()) {
            fail("Missing shard results for " + expResultsMap.keySet());
        }
    }

    private static ShardResult successShardResult(final String shardName, final DataStoreType type) {
        return new ShardResultBuilder().setDataStoreType(type).setShardName(shardName).setSucceeded(TRUE).build();
    }

    private static ShardResult failedShardResult(final String shardName, final DataStoreType type) {
        return new ShardResultBuilder().setDataStoreType(type).setShardName(shardName).setSucceeded(FALSE).build();
    }
}
