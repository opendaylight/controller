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
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Unit tests for ClusterAdminRpcService.
 *
 * @author Thomas Pantelis
 */
public class ClusterAdminRpcServiceTest {
    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder();

    private IntegrationTestKit kit;
    private DistributedDataStore configDataStore;
    private DistributedDataStore operDataStore;
    private ClusterAdminRpcService service;

    @BeforeClass
    public static void setUpClass() throws IOException {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Address member1Address = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @After
    public void tearDown() {
        if(kit != null) {
            kit.cleanup(configDataStore);
            kit.cleanup(operDataStore);
        }
    }

    private void setup(String testName, String... shardNames) {
        kit = new IntegrationTestKit(system, datastoreContextBuilder);

        configDataStore = kit.setupDistributedDataStore(testName + "Config", "module-shards-member1.conf",
                true, shardNames);
        operDataStore = kit.setupDistributedDataStore(testName + "Oper", "module-shards-member1.conf",
                true, shardNames);

        service = new ClusterAdminRpcService(configDataStore, operDataStore);
    }

    @Test
    public void testBackupDatastore() throws Exception {
        setup("testBackupDatastore", "cars", "people");

        String fileName = "target/testBackupDatastore";
        new File(fileName).delete();

        RpcResult<Void> rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().
                setFilePath(fileName).build()).get(5, TimeUnit.SECONDS);
        assertEquals("isSuccessful", true, rpcResult.isSuccessful());

        try(FileInputStream fis = new FileInputStream(fileName)) {
            List<DatastoreSnapshot> snapshots = SerializationUtils.deserialize(fis);
            assertEquals("DatastoreSnapshot size", 2, snapshots.size());

            ImmutableMap<String, DatastoreSnapshot> map = ImmutableMap.of(snapshots.get(0).getType(), snapshots.get(0),
                    snapshots.get(1).getType(), snapshots.get(1));
            verifyDatastoreSnapshot(configDataStore.getActorContext().getDataStoreType(),
                    map.get(configDataStore.getActorContext().getDataStoreType()), "cars", "people");
        } finally {
            new File(fileName).delete();
        }

        // Test failure by killing a shard.

        configDataStore.getActorContext().getShardManager().tell(datastoreContextBuilder.
                shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(), ActorRef.noSender());

        ActorRef carsShardActor = configDataStore.getActorContext().findLocalShard("cars").get();
        kit.watch(carsShardActor);
        carsShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectTerminated(carsShardActor);

        rpcResult = service.backupDatastore(new BackupDatastoreInputBuilder().setFilePath(fileName).build()).
                get(5, TimeUnit.SECONDS);
        assertEquals("isSuccessful", false, rpcResult.isSuccessful());
        assertEquals("getErrors", 1, rpcResult.getErrors().size());
        assertTrue("Expected error cause TimeoutException",
                rpcResult.getErrors().iterator().next().getCause() instanceof TimeoutException);
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
    public void testAddShardReplica() {
        // TODO implement
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
}
