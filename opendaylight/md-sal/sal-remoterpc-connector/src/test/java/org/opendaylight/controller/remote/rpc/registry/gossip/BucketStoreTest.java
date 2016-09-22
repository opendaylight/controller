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
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;

public class BucketStoreTest {

    /**
     * Dummy class to eliminate rawtype warnings.
     *
     * @author gwu
     *
     */
    private static class T implements Copier<T> {
        @Override
        public T copy() {
            return new T();
        }
    }

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {

        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"));
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
    }

    @AfterClass
    public static void teardown() {
        system.shutdown();
    }

    /**
     * Given remote buckets
     * Should merge with local copy of remote buckets
     */
    @Test
    public void testReceiveUpdateRemoteBuckets(){

        BucketStore<T> store = createStore();

        Address localAddress = system.provider().getDefaultAddress();
        Bucket<T> localBucket = new BucketImpl<>();

        Address a1 = new Address("tcp", "system1");
        Address a2 = new Address("tcp", "system2");
        Address a3 = new Address("tcp", "system3");

        Bucket<T> b1 = new BucketImpl<>();
        Bucket<T> b2 = new BucketImpl<>();
        Bucket<T> b3 = new BucketImpl<>();

        Map<Address, Bucket<T>> remoteBuckets = new HashMap<>(3);
        remoteBuckets.put(a1, b1);
        remoteBuckets.put(a2, b2);
        remoteBuckets.put(a3, b3);
        remoteBuckets.put(localAddress, localBucket);

        //Given remote buckets
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should NOT contain local bucket
        //Should contain ONLY 3 entries i.e a1, a2, a3
        Map<Address, Bucket<T>> remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertFalse("remote buckets contains local bucket", remoteBucketsInStore.containsKey(localAddress));
        Assert.assertTrue(remoteBucketsInStore.size() == 3);

        //Add a new remote bucket
        Address a4 = new Address("tcp", "system4");
        Bucket<T> b4 = new BucketImpl<>();
        remoteBuckets.clear();
        remoteBuckets.put(a4, b4);
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should contain a4
        //Should contain 4 entries now i.e a1, a2, a3, a4
        remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertTrue("Does not contain a4", remoteBucketsInStore.containsKey(a4));
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Update a bucket
        Bucket<T> b3_new = new BucketImpl<>();
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3_new);
        remoteBuckets.put(a1, null);
        remoteBuckets.put(a2, null);
        store.receiveUpdateRemoteBuckets(remoteBuckets);

        //Should only update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        Bucket<T> b3_inStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3_new.getVersion(), b3_inStore.getVersion());

        //Should NOT update a1 and a2
        Bucket<T> b1_inStore = remoteBucketsInStore.get(a1);
        Bucket<T> b2_inStore = remoteBucketsInStore.get(a2);
        Assert.assertEquals(b1.getVersion(), b1_inStore.getVersion());
        Assert.assertEquals(b2.getVersion(), b2_inStore.getVersion());
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Should update versions map
        //versions map contains versions for all remote buckets (4).
        Map<Address, Long> versionsInStore = store.getVersions();
        Assert.assertEquals(4, versionsInStore.size());
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
    private static BucketStore<T> createStore(){
        final Props props = Props.create(BucketStore.class, new RemoteRpcProviderConfig(system.settings().config()));
        final TestActorRef<BucketStore<T>> testRef = TestActorRef.create(system, props, "testStore");
        return testRef.underlyingActor();
    }

}
