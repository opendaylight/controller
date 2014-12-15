/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.TestActorRef;

import com.typesafe.config.ConfigFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;

import java.util.HashMap;
import java.util.Map;

public class BucketStoreTest {

    private static ActorSystem system;
    private static BucketStore store;

    @BeforeClass
    public static void setup() {

        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"));
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");

        store = createStore();
    }

    @AfterClass
    public static void teardown() {
        system.shutdown();
    }

    /**
     * Given a new local bucket
     * Should replace
     */
    @Test
    public void testReceiveUpdateBucket(){
        Bucket<RoutingTable> bucket = new BucketImpl<>();
        Long expectedVersion = bucket.getVersion();

        store.receiveUpdateBucket(bucket);

        Assert.assertEquals(bucket, store.getLocalBucket());
        Assert.assertEquals(expectedVersion, store.getLocalBucket().getVersion());
    }

    /**
     * Given remote buckets
     * Should merge with local copy of remote buckets
     */
    @Test
    public void testReceiveUpdateRemoteBuckets(){

        Address localAddress = system.provider().getDefaultAddress();
        Bucket<RoutingTable> localBucket = new BucketImpl<>();

        Address a1 = new Address("tcp", "system1");
        Address a2 = new Address("tcp", "system2");
        Address a3 = new Address("tcp", "system3");

        Bucket<RoutingTable> b1 = new BucketImpl<>();
        Bucket<RoutingTable> b2 = new BucketImpl<>();
        Bucket<RoutingTable> b3 = new BucketImpl<>();

        Map<Address, Bucket<RoutingTable>> remoteBuckets = new HashMap<>(3);
        remoteBuckets.put(a1, b1);
        remoteBuckets.put(a2, b2);
        remoteBuckets.put(a3, b3);
        remoteBuckets.put(localAddress, localBucket);

        //Given remote buckets
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should NOT contain local bucket
        //Should contain ONLY 3 entries i.e a1, a2, a3
        Map<Address, Bucket<RoutingTable>> remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertFalse("remote buckets contains local bucket", remoteBucketsInStore.containsKey(localAddress));
        Assert.assertTrue(remoteBucketsInStore.size() == 3);

        //Add a new remote bucket
        Address a4 = new Address("tcp", "system4");
        Bucket<RoutingTable> b4 = new BucketImpl<>();
        remoteBuckets.clear();
        remoteBuckets.put(a4, b4);
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should contain a4
        //Should contain 4 entries now i.e a1, a2, a3, a4
        remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertTrue("Does not contain a4", remoteBucketsInStore.containsKey(a4));
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Update a bucket
        Bucket<RoutingTable> b3_new = new BucketImpl<>();
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3_new);
        remoteBuckets.put(a1, null);
        remoteBuckets.put(a2, null);
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should only update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        Bucket<RoutingTable> b3_inStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3_new.getVersion(), b3_inStore.getVersion());

        //Should NOT update a1 and a2
        Bucket<RoutingTable> b1_inStore = remoteBucketsInStore.get(a1);
        Bucket<RoutingTable> b2_inStore = remoteBucketsInStore.get(a2);
        Assert.assertEquals(b1.getVersion(), b1_inStore.getVersion());
        Assert.assertEquals(b2.getVersion(), b2_inStore.getVersion());
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Should update versions map
        //versions map contains versions for all remote buckets (4) + local bucket
        //so it should have total 5.
        Map<Address, Long> versionsInStore = store.getVersions();
        Assert.assertTrue(String.format("Expected:%s, Actual:%s", 5, versionsInStore.size()),
                          versionsInStore.size() == 5);
        Assert.assertEquals(b1.getVersion(), versionsInStore.get(a1));
        Assert.assertEquals(b2.getVersion(), versionsInStore.get(a2));
        Assert.assertEquals(b3_new.getVersion(), versionsInStore.get(a3));
        Assert.assertEquals(b4.getVersion(), versionsInStore.get(a4));

        //Send older version of bucket
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3);
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should NOT update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        b3_inStore = remoteBucketsInStore.get(a3);
        Assert.assertTrue(b3_inStore.getVersion().longValue() == b3_new.getVersion().longValue());

    }

    /**
     * Create BucketStore actor and returns the underlying instance of BucketStore class.
     *
     * @return instance of BucketStore class
     */
    private static BucketStore createStore(){
        final Props props = Props.create(BucketStore.class);
        final TestActorRef<BucketStore> testRef = TestActorRef.create(system, props, "testStore");
        return testRef.underlyingActor();
    }
}