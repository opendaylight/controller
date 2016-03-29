/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyNoShardPresent;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyRaftPeersPresent;
import static org.opendaylight.controller.cluster.datastore.MemberNode.verifyRaftState;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.datastore.MemberNode.RaftStateVerifier;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Unit tests for ClusterAdminRpcService.
 *
 * @author Thomas Pantelis
 */
public class ClusterAdminRpcServiceTest {
    private final List<MemberNode> memberNodes = new ArrayList<>();

    @Before
    public void setUp() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @After
    public void tearDown() {
        for(MemberNode m: memberNodes) {
            m.cleanup();
        }
    }

    @Test
    public void testBackupDatastore() throws Exception {
        MemberNode node = MemberNode.builder(memberNodes).akkaConfig("Member1").
                moduleShardsConfig("module-shards-member1.conf").
                waitForShardLeader("cars", "people").testName("testBackupDatastore").build();

        String fileName = "target/testBackupDatastore";
        new File(fileName).delete();

        ClusterAdminRpcService service = new ClusterAdminRpcService(node.configDataStore(), node.operDataStore());

        RpcResult<Void> rpcResult = service .backupDatastore(new BackupDatastoreInputBuilder().
                setFilePath(fileName).build()).get(5, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        try(FileInputStream fis = new FileInputStream(fileName)) {
            List<DatastoreSnapshot> snapshots = SerializationUtils.deserialize(fis);
            assertEquals("DatastoreSnapshot size", 2, snapshots.size());

            ImmutableMap<String, DatastoreSnapshot> map = ImmutableMap.of(snapshots.get(0).getType(), snapshots.get(0),
                    snapshots.get(1).getType(), snapshots.get(1));
            verifyDatastoreSnapshot(node.configDataStore().getActorContext().getDataStoreName(),
                    map.get(node.configDataStore().getActorContext().getDataStoreName()), "cars", "people");
        } finally {
            new File(fileName).delete();
        }

        // Test failure by killing a shard.

        node.configDataStore().getActorContext().getShardManager().tell(node.datastoreContextBuilder().
                shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(), ActorRef.noSender());

        ActorRef carsShardActor = node.configDataStore().getActorContext().findLocalShard("cars").get();
        node.kit().watch(carsShardActor);
        carsShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        node.kit().expectTerminated(carsShardActor);

        rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().setFilePath(fileName).build()).
                get(5, TimeUnit.SECONDS);
        assertEquals("isSuccessful", false, rpcResult.isSuccessful());
        assertEquals("getErrors", 1, rpcResult.getErrors().size());

        service.close();
    }

    private static void verifyDatastoreSnapshot(String type, DatastoreSnapshot datastoreSnapshot, String... expShardNames) {
        assertNotNull("Missing DatastoreSnapshot for type " + type, datastoreSnapshot);
        Set<String> shardNames = new HashSet<>();
        for(DatastoreSnapshot.ShardSnapshot s: datastoreSnapshot.getShardSnapshots()) {
            shardNames.add(s.getName());
        }

        assertEquals("DatastoreSnapshot shard names", Sets.newHashSet(expShardNames), shardNames);
    }

    @Test
    public void testAddShardReplica() throws Exception {
        String name = "testAddShardReplica";
        String moduleShardsConfig = "module-shards-cars-member-1.conf";
        MemberNode leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).waitForShardLeader("cars").build();

        MemberNode newReplicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-2");

        doAddShardReplica(newReplicaNode2, "cars", "member-1");

        MemberNode newReplicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-3");
        newReplicaNode2.waitForMembersUp("member-3");

        doAddShardReplica(newReplicaNode3, "cars", "member-1", "member-2");

        verifyRaftPeersPresent(newReplicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(newReplicaNode2.operDataStore(), "cars", "member-1", "member-3");

        // Write data to member-2's config datastore and read/verify via member-3
        NormalizedNode<?, ?> configCarsNode = writeCarsNodeAndVerify(newReplicaNode2.configDataStore(),
                newReplicaNode3.configDataStore());

        // Write data to member-3's oper datastore and read/verify via member-2
        writeCarsNodeAndVerify(newReplicaNode3.operDataStore(), newReplicaNode2.operDataStore());

        // Verify all data has been replicated. We expect 3 log entries and thus last applied index of 2 -
        // 2 ServerConfigurationPayload entries and the transaction payload entry.

        RaftStateVerifier verifier = new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("Commit index", 2, raftState.getCommitIndex());
                assertEquals("Last applied index", 2, raftState.getLastApplied());
            }
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

        newReplicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).createOperDatastore(false).build();

        verifyRaftState(newReplicaNode3.configDataStore(), "cars", verifier);
        readCarsNodeAndVerify(newReplicaNode3.configDataStore(), configCarsNode);
    }

    @Test
    public void testAddShardReplicaFailures() throws Exception {
        String name = "testAddShardReplicaFailures";
        MemberNode memberNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name).
                moduleShardsConfig("module-shards-cars-member-1.conf").build();

        ClusterAdminRpcService service = new ClusterAdminRpcService(memberNode.configDataStore(),
                memberNode.operDataStore());

        RpcResult<Void> rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().
                setDataStoreType(DataStoreType.Config).build()).get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName("cars").
                build()).get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName("people").
                setDataStoreType(DataStoreType.Config).build()).get(10, TimeUnit.SECONDS);
        verifyFailedRpcResult(rpcResult);

        service.close();
    }

    private static NormalizedNode<?, ?> writeCarsNodeAndVerify(DistributedDataStore writeToStore,
            DistributedDataStore readFromStore) throws Exception {
        DOMStoreWriteTransaction writeTx = writeToStore.newWriteOnlyTransaction();
        NormalizedNode<?, ?> carsNode = CarsModel.create();
        writeTx.write(CarsModel.BASE_PATH, carsNode);

        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        Boolean canCommit = cohort .canCommit().get(7, TimeUnit.SECONDS);
        assertEquals("canCommit", true, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);

        readCarsNodeAndVerify(readFromStore, carsNode);
        return carsNode;
    }

    private static void readCarsNodeAndVerify(DistributedDataStore readFromStore,
            NormalizedNode<?, ?> expCarsNode) throws Exception {
        Optional<NormalizedNode<?, ?>> optional = readFromStore.newReadOnlyTransaction().
                read(CarsModel.BASE_PATH).get(15, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", expCarsNode, optional.get());
    }

    private static void doAddShardReplica(MemberNode memberNode, String shardName, String... peerMemberNames)
            throws Exception {
        memberNode.waitForMembersUp(peerMemberNames);

        ClusterAdminRpcService service = new ClusterAdminRpcService(memberNode.configDataStore(),
                memberNode.operDataStore());

        RpcResult<Void> rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName(shardName).
                setDataStoreType(DataStoreType.Config).build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(memberNode.configDataStore(), shardName, peerMemberNames);

        Optional<ActorRef> optional = memberNode.operDataStore().getActorContext().findLocalShard(shardName);
        assertEquals("Oper shard present", false, optional.isPresent());

        rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName(shardName).
                setDataStoreType(DataStoreType.Operational).build()).get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(memberNode.operDataStore(), shardName, peerMemberNames);

        service.close();
    }

    private static <T> T verifySuccessfulRpcResult(RpcResult<T> rpcResult) {
        if(!rpcResult.isSuccessful()) {
            if(rpcResult.getErrors().size() > 0) {
                RpcError error = Iterables.getFirst(rpcResult.getErrors(), null);
                throw new AssertionError("Rpc failed with error: " + error, error.getCause());
            }

            fail("Rpc failed with no error");
        }

        return rpcResult.getResult();
    }

    private static void verifyFailedRpcResult(RpcResult<Void> rpcResult) {
        assertEquals("RpcResult", false, rpcResult.isSuccessful());
        assertEquals("RpcResult errors size", 1, rpcResult.getErrors().size());
        RpcError error = Iterables.getFirst(rpcResult.getErrors(), null);
        assertNotNull("RpcResult error message null", error.getMessage());
    }

    @Test
    public void testRemoveShardReplica() throws Exception {
        String name = "testRemoveShardReplicaLocal";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        MemberNode leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).
                datastoreContextBuilder(DatastoreContext.newBuilder().
                        shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1)).build();

        MemberNode replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        MemberNode replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        // Invoke RPC service on member-3 to remove it's local shard

        ClusterAdminRpcService service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(),
                replicaNode3.operDataStore());

        RpcResult<Void> rpcResult = service3.removeShardReplica(new RemoveShardReplicaInputBuilder().
                setShardName("cars").setMemberName("member-3").setDataStoreType(DataStoreType.Config).build()).
                        get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);
        service3.close();

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1");
        verifyNoShardPresent(replicaNode3.configDataStore(), "cars");

        // Restart member-2 and verify member-3 isn't present.

        Cluster.get(leaderNode1.kit().getSystem()).down(Cluster.get(replicaNode2.kit().getSystem()).selfAddress());
        replicaNode2.cleanup();

        replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1");

        // Invoke RPC service on member-1 to remove member-2

        ClusterAdminRpcService service1 = new ClusterAdminRpcService(leaderNode1.configDataStore(),
                leaderNode1.operDataStore());

        rpcResult = service1.removeShardReplica(new RemoveShardReplicaInputBuilder().
                setShardName("cars").setMemberName("member-2").setDataStoreType(DataStoreType.Config).build()).
                        get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);
        service1.close();

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars");
        verifyNoShardPresent(replicaNode2.configDataStore(), "cars");
    }

    @Test
    public void testRemoveShardLeaderReplica() throws Exception {
        String name = "testRemoveShardLeaderReplica";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        MemberNode leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).
                datastoreContextBuilder(DatastoreContext.newBuilder().
                        shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1)).build();

        MemberNode replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        MemberNode replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        replicaNode2.waitForMembersUp("member-1", "member-3");
        replicaNode2.waitForMembersUp("member-1", "member-2");

        // Invoke RPC service on leader member-1 to remove it's local shard

        ClusterAdminRpcService service1 = new ClusterAdminRpcService(leaderNode1.configDataStore(),
                leaderNode1.operDataStore());

        RpcResult<Void> rpcResult = service1.removeShardReplica(new RemoveShardReplicaInputBuilder().
                setShardName("cars").setMemberName("member-1").setDataStoreType(DataStoreType.Config).build()).
                        get(10, TimeUnit.SECONDS);
        verifySuccessfulRpcResult(rpcResult);
        service1.close();

        verifyRaftState(replicaNode2.configDataStore(), "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertThat("Leader Id", raftState.getLeader(), anyOf(containsString("member-2"),
                        containsString("member-3")));
            }
        });

        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-2");
        verifyNoShardPresent(leaderNode1.configDataStore(), "cars");
    }

    @Test
    public void testAddReplicasForAllShards() throws Exception {
        String name = "testAddReplicasForAllShards";
        String moduleShardsConfig = "module-shards-member1.conf";
        MemberNode leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).waitForShardLeader("cars", "people").build();

        ModuleShardConfiguration petsModuleConfig = new ModuleShardConfiguration(URI.create("pets-ns"), "pets-module",
                "pets", null, Arrays.asList("member-1"));
        leaderNode1.configDataStore().getActorContext().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), leaderNode1.kit().getRef());
        leaderNode1.kit().expectMsgClass(Success.class);
        leaderNode1.kit().waitUntilLeader(leaderNode1.configDataStore().getActorContext(), "pets");

        MemberNode newReplicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-2");
        newReplicaNode2.waitForMembersUp("member-1");

        newReplicaNode2.configDataStore().getActorContext().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), newReplicaNode2.kit().getRef());
        newReplicaNode2.kit().expectMsgClass(Success.class);

        newReplicaNode2.operDataStore().getActorContext().getShardManager().tell(
                new CreateShard(new ModuleShardConfiguration(URI.create("no-leader-ns"), "no-leader-module",
                        "no-leader", null, Arrays.asList("member-1")), Shard.builder(), null),
                                newReplicaNode2.kit().getRef());
        newReplicaNode2.kit().expectMsgClass(Success.class);

        ClusterAdminRpcService service = new ClusterAdminRpcService(newReplicaNode2.configDataStore(),
                newReplicaNode2.operDataStore());

        RpcResult<AddReplicasForAllShardsOutput> rpcResult = service.addReplicasForAllShards().get(10, TimeUnit.SECONDS);
        AddReplicasForAllShardsOutput result = verifySuccessfulRpcResult(rpcResult);
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

        service.close();
    }

    @Test
    public void testRemoveAllShardReplicas() throws Exception {
        String name = "testRemoveAllShardReplicas";
        String moduleShardsConfig = "module-shards-member1-and-2-and-3.conf";
        MemberNode leaderNode1 = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name ).
                moduleShardsConfig(moduleShardsConfig).datastoreContextBuilder(DatastoreContext.newBuilder().
                        shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(1)).build();

        MemberNode replicaNode2 = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        MemberNode replicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.configDataStore().waitTillReady();
        verifyRaftPeersPresent(leaderNode1.configDataStore(), "cars", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "cars", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "cars", "member-1", "member-2");

        ModuleShardConfiguration petsModuleConfig = new ModuleShardConfiguration(URI.create("pets-ns"), "pets-module",
                "pets", null, Arrays.asList("member-1", "member-2", "member-3"));
        leaderNode1.configDataStore().getActorContext().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), leaderNode1.kit().getRef());
        leaderNode1.kit().expectMsgClass(Success.class);

        replicaNode2.configDataStore().getActorContext().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), replicaNode2.kit().getRef());
        replicaNode2.kit().expectMsgClass(Success.class);

        replicaNode3.configDataStore().getActorContext().getShardManager().tell(
                new CreateShard(petsModuleConfig, Shard.builder(), null), replicaNode3.kit().getRef());
        replicaNode3.kit().expectMsgClass(Success.class);

        verifyRaftPeersPresent(leaderNode1.configDataStore(), "pets", "member-2", "member-3");
        verifyRaftPeersPresent(replicaNode2.configDataStore(), "pets", "member-1", "member-3");
        verifyRaftPeersPresent(replicaNode3.configDataStore(), "pets", "member-1", "member-2");

        ClusterAdminRpcService service3 = new ClusterAdminRpcService(replicaNode3.configDataStore(),
                replicaNode3.operDataStore());

        RpcResult<RemoveAllShardReplicasOutput> rpcResult = service3.removeAllShardReplicas(
                new RemoveAllShardReplicasInputBuilder().setMemberName("member-3").build()).get(10, TimeUnit.SECONDS);
        RemoveAllShardReplicasOutput result = verifySuccessfulRpcResult(rpcResult);
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

        service3.close();
    }

    @Test
    public void testConvertMembersToVotingForAllShards() {
        // TODO implement
    }

    @Test
    public void testConvertMembersToNonvotingForAllShards() {
        // TODO implement
    }

    private static void verifyShardResults(List<ShardResult> shardResults, ShardResult... expShardResults) {
        Map<String, ShardResult> expResultsMap = new HashMap<>();
        for(ShardResult r: expShardResults) {
            expResultsMap.put(r.getShardName() + "-" + r.getDataStoreType(), r);
        }

        for(ShardResult result: shardResults) {
            ShardResult exp = expResultsMap.remove(result.getShardName() + "-" + result.getDataStoreType());
            assertNotNull(String.format("Unexpected result for shard %s, type %s", result.getShardName(),
                    result.getDataStoreType()), exp);
            assertEquals("isSucceeded", exp.isSucceeded(), result.isSucceeded());
            if(exp.isSucceeded()) {
                assertNull("Expected null error message", result.getErrorMessage());
            } else {
                assertNotNull("Expected error message", result.getErrorMessage());
            }
        }

        if(!expResultsMap.isEmpty()) {
            fail("Missing shard results for " + expResultsMap.keySet());
        }
    }

    private static ShardResult successShardResult(String shardName, DataStoreType type) {
        return new ShardResultBuilder().setDataStoreType(type).setShardName(shardName).setSucceeded(true).build();
    }

    private static ShardResult failedShardResult(String shardName, DataStoreType type) {
        return new ShardResultBuilder().setDataStoreType(type).setShardName(shardName).setSucceeded(false).build();
    }
}
