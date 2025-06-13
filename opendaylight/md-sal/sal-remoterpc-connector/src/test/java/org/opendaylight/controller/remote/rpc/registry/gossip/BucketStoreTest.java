/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BucketStoreTest {
    /**
     * Dummy class to eliminate rawtype warnings.
     *
     * @author gwu
     */
    private static final class TestBucketData implements BucketData<TestBucketData> {
        @Override
        public Optional<ActorRef> getWatchActor() {
            return Optional.empty();
        }
    }

    private static final class TestingBucketStoreActor extends BucketStoreActor<TestBucketData> {
        private static final Logger LOG = LoggerFactory.getLogger(TestingBucketStoreActor.class);

        TestingBucketStoreActor(final RemoteOpsProviderConfig config, final String persistenceId,
                final TestBucketData initialData) {
            super(config, persistenceId, initialData);
        }

        @Override
        protected void onBucketRemoved(final Address address, final Bucket<TestBucketData> bucket) {
            // No-op
        }

        @Override
        protected void onBucketsUpdated(final Map<Address, Bucket<TestBucketData>> newBuckets) {
            // No-op
        }

        @Override
        protected Logger log() {
            return LOG;
        }
    }

    private static ActorSystem system;

    private BucketStoreActor<TestBucketData> store;

    @BeforeAll
    static void beforeAll() {
        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"));
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
    }

    @AfterAll
    static void afterAll() {
        TestKit.shutdownActorSystem(system);
    }

    @BeforeEach
    void before() {
        final var props = Props.create(TestingBucketStoreActor.class,
            new RemoteOpsProviderConfig(system.settings().config()),
            "testing-store", new TestBucketData());
        store = TestActorRef.<BucketStoreActor<TestBucketData>>create(system, props, "testStore").underlyingActor();
    }

    /**
     * Given remote buckets, should merge with local copy of remote buckets.
     */
    @Test
    void testReceiveUpdateRemoteBuckets() {
        final var localAddress = system.provider().getDefaultAddress();
        final var localBucket = new BucketImpl<>(0L, new TestBucketData());

        final var a1 = new Address("tcp", "system1");
        final var a2 = new Address("tcp", "system2");
        final var a3 = new Address("tcp", "system3");

        final var b1 = new BucketImpl<>(0L, new TestBucketData());
        final var b2 = new BucketImpl<>(0L, new TestBucketData());
        final var b3 = new BucketImpl<>(0L, new TestBucketData());

        //Given remote buckets
        store.updateRemoteBuckets(Map.of(a1, b1, a2, b2, localAddress, localBucket));

        //Should NOT contain local bucket
        //Should contain ONLY 3 entries i.e a1, a2
        var remoteBucketsInStore = store.getRemoteBuckets();
        assertFalse(remoteBucketsInStore.containsKey(localAddress), "remote buckets contains local bucket");
        assertEquals(2, remoteBucketsInStore.size());

        //Add a new remote bucket
        final var a4 = new Address("tcp", "system4");
        final var b4 = new BucketImpl<>(0L, new TestBucketData());
        store.updateRemoteBuckets(Map.of(a4, b4));

        //Should contain a4
        //Should contain 4 entries now i.e a1, a2, a4
        remoteBucketsInStore = store.getRemoteBuckets();
        assertTrue(remoteBucketsInStore.containsKey(a4), "Does not contain a4");
        assertEquals(3, remoteBucketsInStore.size());

        //Update a bucket
        final var b3New = new BucketImpl<>(0L, new TestBucketData());
        // Note: includes nulls on purpose
        final var remoteBuckets = HashMap.<Address, Bucket<?>>newHashMap(3);
        remoteBuckets.put(a3, b3New);
        remoteBuckets.put(a1, null);
        remoteBuckets.put(a2, null);
        store.updateRemoteBuckets(remoteBuckets);

        //Should only update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        var b3InStore = remoteBucketsInStore.get(a3);
        assertEquals(b3New.getVersion(), b3InStore.getVersion());

        //Should NOT update a1 and a2
        final var b1InStore = remoteBucketsInStore.get(a1);
        final var b2InStore = remoteBucketsInStore.get(a2);
        assertEquals(b1.getVersion(), b1InStore.getVersion());
        assertEquals(b2.getVersion(), b2InStore.getVersion());
        assertEquals(4, remoteBucketsInStore.size());

        //Should update versions map
        //versions map contains versions for all remote buckets (4).
        final var versionsInStore = store.getVersions();
        assertEquals(4, versionsInStore.size());
        assertEquals(b1.getVersion(), versionsInStore.get(a1));
        assertEquals(b2.getVersion(), versionsInStore.get(a2));
        assertEquals(b3New.getVersion(), versionsInStore.get(a3));
        assertEquals(b4.getVersion(), versionsInStore.get(a4));

        //Send older version of bucket
        store.updateRemoteBuckets(Map.of(a3, b3));

        //Should NOT update a3
        remoteBucketsInStore = store.getRemoteBuckets();
        b3InStore = remoteBucketsInStore.get(a3);
        assertEquals(b3InStore.getVersion(), b3New.getVersion());
    }
}
