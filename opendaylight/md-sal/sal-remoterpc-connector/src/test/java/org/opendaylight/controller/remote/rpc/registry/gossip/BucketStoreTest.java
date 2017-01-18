/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private static class T implements BucketData<T> {
        @Override
        public Optional<ActorRef> getWatchActor() {
            return Optional.empty();
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
        JavaTestKit.shutdownActorSystem(system);
    }

    /**
     * Given remote buckets, should merge with local copy of remote buckets.
     */
    @Test
    public void testReceiveUpdateRemoteBuckets() {

        final BucketStoreActor<T> store = createStore();

        Address localAddress = system.provider().getDefaultAddress();
        Bucket<T> localBucket = new BucketImpl<>(0L, new T());

        final Address a1 = new Address("tcp", "system1");
        final Address a2 = new Address("tcp", "system2");
        final Address a3 = new Address("tcp", "system3");

        final Bucket<T> b1 = new BucketImpl<>(0L, new T());
        final Bucket<T> b2 = new BucketImpl<>(0L, new T());
        final Bucket<T> b3 = new BucketImpl<>(0L, new T());

        //Given remote buckets
        store.updateRemoteBuckets(ImmutableMap.of(a1, b1, a2, b2, localAddress, localBucket));

        //Should NOT contain local bucket
        //Should contain ONLY 3 entries i.e a1, a2
        Map<Address, Bucket<T>> remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertFalse("remote buckets contains local bucket", remoteBucketsInStore.containsKey(localAddress));
        Assert.assertTrue(remoteBucketsInStore.size() == 2);

        //Add a new remote bucket
        Address a4 = new Address("tcp", "system4");
        Bucket<T> b4 = new BucketImpl<>(0L, new T());
        store.updateRemoteBuckets(ImmutableMap.of(a4, b4));

        //Should contain a4
        //Should contain 4 entries now i.e a1, a2, a4
        remoteBucketsInStore = store.getRemoteBuckets();
        Assert.assertTrue("Does not contain a4", remoteBucketsInStore.containsKey(a4));
        Assert.assertTrue(remoteBucketsInStore.size() == 3);

        //Update a bucket
        Bucket<T> b3New = new BucketImpl<>(0L, new T());
        Map<Address, Bucket<?>> remoteBuckets = new HashMap<>(3);
        remoteBuckets.put(a3, b3New);
        remoteBuckets.put(a1, null);
        remoteBuckets.put(a2, null);
        store.updateRemoteBuckets(remoteBuckets);

        //Should only update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        Bucket<T> b3InStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3New.getVersion(), b3InStore.getVersion());

        //Should NOT update a1 and a2
        Bucket<T> b1InStore = remoteBucketsInStore.get(a1);
        Bucket<T> b2InStore = remoteBucketsInStore.get(a2);
        Assert.assertEquals(b1.getVersion(), b1InStore.getVersion());
        Assert.assertEquals(b2.getVersion(), b2InStore.getVersion());
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Should update versions map
        //versions map contains versions for all remote buckets (4).
        Map<Address, Long> versionsInStore = store.getVersions();
        Assert.assertEquals(4, versionsInStore.size());
        Assert.assertEquals((Long)b1.getVersion(), versionsInStore.get(a1));
        Assert.assertEquals((Long)b2.getVersion(), versionsInStore.get(a2));
        Assert.assertEquals((Long)b3New.getVersion(), versionsInStore.get(a3));
        Assert.assertEquals((Long)b4.getVersion(), versionsInStore.get(a4));

        //Send older version of bucket
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3);
        store.updateRemoteBuckets(remoteBuckets);

        //Should NOT update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        b3InStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3InStore.getVersion(), b3New.getVersion());

    }

    /**
     * Create BucketStore actor and returns the underlying instance of BucketStore class.
     *
     * @return instance of BucketStore class
     */
    private static BucketStoreActor<T> createStore() {
        final Props props = Props.create(TestingBucketStoreActor.class,
                new RemoteRpcProviderConfig(system.settings().config()), "testing-store",new T());
        return TestActorRef.<BucketStoreActor<T>>create(system, props, "testStore").underlyingActor();
    }

    private static final class TestingBucketStoreActor extends BucketStoreActor<T> {

        protected TestingBucketStoreActor(final RemoteRpcProviderConfig config,
                                          final String persistenceId,
                                          final T initialData) {
            super(config, persistenceId, initialData);
        }

        @Override
        protected void onBucketRemoved(final Address address, final Bucket<T> bucket) {

        }

        @Override
        protected void onBucketsUpdated(final Map<Address, Bucket<T>> newBuckets) {

        }
    }
}
