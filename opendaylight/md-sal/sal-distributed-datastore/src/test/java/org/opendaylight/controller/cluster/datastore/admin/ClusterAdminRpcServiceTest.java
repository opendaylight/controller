/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Unit tests for ClusterAdminRpcService.
 *
 * @author Thomas Pantelis
 */
public class ClusterAdminRpcServiceTest {
    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");

    private final List<MemberNode> memberNodes = new ArrayList<>();

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

        ClusterAdminRpcService service = new ClusterAdminRpcService(node.configDataStore, node.operDataStore);

        RpcResult<Void> rpcResult = service .backupDatastore(new BackupDatastoreInputBuilder().
                setFilePath(fileName).build()).get(5, TimeUnit.SECONDS);
        checkSuccessfulRpcResult(rpcResult);

        try(FileInputStream fis = new FileInputStream(fileName)) {
            List<DatastoreSnapshot> snapshots = SerializationUtils.deserialize(fis);
            assertEquals("DatastoreSnapshot size", 2, snapshots.size());

            ImmutableMap<String, DatastoreSnapshot> map = ImmutableMap.of(snapshots.get(0).getType(), snapshots.get(0),
                    snapshots.get(1).getType(), snapshots.get(1));
            verifyDatastoreSnapshot(node.configDataStore.getActorContext().getDataStoreType(),
                    map.get(node.configDataStore.getActorContext().getDataStoreType()), "cars", "people");
        } finally {
            new File(fileName).delete();
        }

        // Test failure by killing a shard.

        node.configDataStore.getActorContext().getShardManager().tell(node.datastoreContextBuilder.
                shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(), ActorRef.noSender());

        ActorRef carsShardActor = node.configDataStore.getActorContext().findLocalShard("cars").get();
        node.kit.watch(carsShardActor);
        carsShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        node.kit.expectTerminated(carsShardActor);

        rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().setFilePath(fileName).build()).
                get(5, TimeUnit.SECONDS);
        assertEquals("isSuccessful", false, rpcResult.isSuccessful());
        assertEquals("getErrors", 1, rpcResult.getErrors().size());

        service.close();
    }

    private void verifyDatastoreSnapshot(String type, DatastoreSnapshot datastoreSnapshot, String... expShardNames) {
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

        testAddShardReplica(newReplicaNode2, "cars", "member-1");

        MemberNode newReplicaNode3 = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name).
                moduleShardsConfig(moduleShardsConfig).build();

        leaderNode1.waitForMembersUp("member-3");
        newReplicaNode2.waitForMembersUp("member-3");

        testAddShardReplica(newReplicaNode3, "cars", "member-1", "member-2");

        verifyRaftPeersPresent(newReplicaNode2.configDataStore, "cars", "member-1", "member-3");
        verifyRaftPeersPresent(newReplicaNode2.operDataStore, "cars", "member-1", "member-3");

        // Write data in member-2 and read/verify via member-3

        DOMStoreWriteTransaction writeTx = newReplicaNode2.configDataStore.newWriteOnlyTransaction();
        NormalizedNode<?, ?> carsNode = CarsModel.create();
        writeTx.write(CarsModel.BASE_PATH, carsNode);
        newReplicaNode2.kit.doCommit(writeTx.ready());

        Optional<NormalizedNode<?, ?>> optional = newReplicaNode3.configDataStore.newReadOnlyTransaction().
                read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", carsNode, optional.get());

        // Verify all data has been replicated. We expect 3 log entries and thus last applied index of 2 -
        // 2 ServerConfigurationPayload entries and the transaction payload entry.

        verifyRaftState(leaderNode1.configDataStore, "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("Leader commit index", 2, raftState.getCommitIndex());
                assertEquals("Leader last applied index", 2, raftState.getLastApplied());
            }
        });

        verifyRaftState(newReplicaNode2.configDataStore, "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("Member2 commit index", 2, raftState.getCommitIndex());
                assertEquals("Member2 last applied index", 2, raftState.getLastApplied());
            }
        });

        verifyRaftState(newReplicaNode3.configDataStore, "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("Member3 commit index", 2, raftState.getCommitIndex());
                assertEquals("Member3 last applied index", 2, raftState.getLastApplied());
            }
        });
    }

    private void testAddShardReplica(MemberNode memberNode, String shardName, String... peerMemberNames)
            throws Exception {
        memberNode.waitForMembersUp(peerMemberNames);

        ClusterAdminRpcService service = new ClusterAdminRpcService(memberNode.configDataStore,
                memberNode.operDataStore);

        RpcResult<Void> rpcResult = service.addShardReplica(new AddShardReplicaInputBuilder().setShardName(shardName).
                build()).get(10, TimeUnit.SECONDS);
        checkSuccessfulRpcResult(rpcResult);

        verifyRaftPeersPresent(memberNode.configDataStore, shardName, peerMemberNames);
        verifyRaftPeersPresent(memberNode.operDataStore, shardName, peerMemberNames);

        service.close();
    }

    private void verifyRaftPeersPresent(DistributedDataStore datastore, final String shardName, String... peerMemberNames) {
        final Set<String> peerIds = Sets.newHashSet();
        for(String p: peerMemberNames) {
            peerIds.add(ShardIdentifier.builder().memberName(p).shardName(shardName).
                type(datastore.getActorContext().getDataStoreType()).build().toString());
        }

        verifyRaftState(datastore, shardName, new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertTrue("Peer(s) " + peerIds + " not found for shard " + shardName,
                        raftState.getPeerAddresses().keySet().containsAll(peerIds));
            }
        });
    }

    private void verifyRaftState(DistributedDataStore datastore, String shardName, RaftStateVerifier verifier) {
        ActorContext actorContext = datastore.getActorContext();
        ActorRef shardActor = actorContext.findLocalShard(shardName).get();

        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            OnDemandRaftState raftState = (OnDemandRaftState)actorContext.
                    executeOperation(shardActor, GetOnDemandRaftState.INSTANCE);

            try {
                verifier.verify(raftState);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    private void checkSuccessfulRpcResult(RpcResult<Void> rpcResult) {
        if(!rpcResult.isSuccessful()) {
            if(rpcResult.getErrors().size() > 0) {
                RpcError error = Iterables.getFirst(rpcResult.getErrors(), null);
                throw new AssertionError("Rpc failed with error: " + error, error.getCause());
            }

            fail("Rpc failed with no error");
        }
    }

    @Test
    public void testRemoveShardReplica() {
        // TODO implement
    }

    @Test
    public void testAddReplicasForAllShards() {
        // TODO implement
    }

    @Test
    public void testRemoveAllShardReplicas() {
        // TODO implement
    }

    @Test
    public void testConvertMembersToVotingForAllShards() {
        // TODO implement
    }

    @Test
    public void testConvertMembersToNonvotingForAllShards() {
        // TODO implement
    }

    private static class MemberNode {
        IntegrationTestKit kit;
        DistributedDataStore configDataStore;
        DistributedDataStore operDataStore;
        final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(300).shardElectionTimeoutFactor(30);

        static Builder builder(List<MemberNode> members) {
            return new Builder(members);
        }

        void waitForMembersUp(String... otherMembers) {
            Set<String> otherMembersSet = Sets.newHashSet(otherMembers);
            Stopwatch sw = Stopwatch.createStarted();
            while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
                CurrentClusterState state = Cluster.get(kit.getSystem()).state();
                for(Member m: state.getMembers()) {
                    if(m.status() == MemberStatus.up() && otherMembersSet.remove(m.getRoles().iterator().next()) &&
                            otherMembersSet.isEmpty()) {
                        return;
                    }
                }

                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }

            fail("Member(s) " + otherMembersSet + " are not Up");
        }

        void cleanup() {
            kit.cleanup(configDataStore);
            kit.cleanup(operDataStore);
            JavaTestKit.shutdownActorSystem(kit.getSystem());
        }

        static class Builder {
            List<MemberNode> members;
            String moduleShardsConfig;
            String akkaConfig;
            String[] waitForshardLeader = new String[0];
            String testName;

            Builder(List<MemberNode> members) {
                this.members = members;
            }

            Builder moduleShardsConfig(String moduleShardsConfig) {
                this.moduleShardsConfig = moduleShardsConfig;
                return this;
            }

            Builder akkaConfig(String akkaConfig) {
                this.akkaConfig = akkaConfig;
                return this;
            }

            Builder testName(String testName) {
                this.testName = testName;
                return this;
            }

            Builder waitForShardLeader(String... shardNames) {
                this.waitForshardLeader = shardNames;
                return this;
            }

            MemberNode build() {
                MemberNode info = new MemberNode();
                ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(akkaConfig));
                Cluster.get(system).join(MEMBER_1_ADDRESS);

                info.kit = new IntegrationTestKit(system, info.datastoreContextBuilder);

                info.configDataStore = info.kit.setupDistributedDataStore("config_" + testName, moduleShardsConfig,
                        true, waitForshardLeader);
                info.operDataStore = info.kit.setupDistributedDataStore("oper_" + testName, moduleShardsConfig,
                        true, waitForshardLeader);

                members.add(info);
                return info;
            }
        }
    }

    private static interface RaftStateVerifier {
        void verify(OnDemandRaftState raftState);
    }
}
