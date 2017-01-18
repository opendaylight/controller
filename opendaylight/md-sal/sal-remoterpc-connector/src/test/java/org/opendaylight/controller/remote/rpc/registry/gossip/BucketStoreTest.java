/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static akka.actor.ActorRef.noSender;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.Status.Success;
import akka.pattern.Patterns;
import akka.persistence.SaveSnapshotSuccess;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

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

    private JavaTestKit kit;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"));
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Before
    public void before() {
        kit = new JavaTestKit(system);
    }

    @After
    public void after() {
        kit.shutdown(system);
    }

    /**
     * Given remote buckets, should merge with local copy of remote buckets.
     */
    @Test
    public void testReceiveUpdateRemoteBuckets() throws Exception {

        final ActorRef store = createStore();
        Address localAddress = system.provider().getDefaultAddress();
        Bucket<T> localBucket = new BucketImpl<>(0L, new T());

        Address a1 = new Address("tcp", "system1");
        Address a2 = new Address("tcp", "system2");
        Address a3 = new Address("tcp", "system3");

        Bucket<T> b1 = new BucketImpl<>(0L, new T());
        Bucket<T> b2 = new BucketImpl<>(0L, new T());
        Bucket<T> b3 = new BucketImpl<>(0L, new T());

        Map<Address, Bucket<T>> remoteBuckets = new HashMap<>(3);
        remoteBuckets.put(a1, b1);
        remoteBuckets.put(a2, b2);
        remoteBuckets.put(a3, b3);
        remoteBuckets.put(localAddress, localBucket);

        Await.result(Patterns.ask(store, new WaitUntilDonePersisting(),
                Timeout.apply(5, TimeUnit.SECONDS)), Duration.Inf());

        //Given remote buckets
        store.tell(new UpdateRemoteBuckets<>(remoteBuckets), noSender());

        //Should contain local bucket
        //Should contain 4 entries i.e a1, a2, a3, local
        Map<Address, Bucket<T>> remoteBucketsInStore = getBuckets(store);
        Assert.assertTrue(remoteBucketsInStore.size() == 4);

        //Add a new remote bucket
        Address a4 = new Address("tcp", "system4");
        Bucket<T> b4 = new BucketImpl<>(0L, new T());
        remoteBuckets.clear();
        remoteBuckets.put(a4, b4);
        store.tell(new UpdateRemoteBuckets<>(remoteBuckets), noSender());

        //Should contain a4
        //Should contain 5 entries now i.e a1, a2, a3, a4, local
        remoteBucketsInStore = getBuckets(store);
        Assert.assertTrue("Does not contain a4", remoteBucketsInStore.containsKey(a4));
        Assert.assertTrue(remoteBucketsInStore.size() == 5);

        //Update a bucket
        Bucket<T> b3New = new BucketImpl<>(0L, new T());
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3New);
        remoteBuckets.put(a1, null);
        remoteBuckets.put(a2, null);
        store.tell(new UpdateRemoteBuckets<>(remoteBuckets), noSender());

        //Should only update a3
        remoteBucketsInStore = getBuckets(store);
        Bucket<T> b3InStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3New.getVersion(), b3InStore.getVersion());

        //Should NOT update a1 and a2
        Bucket<T> b1InStore = remoteBucketsInStore.get(a1);
        Bucket<T> b2InStore = remoteBucketsInStore.get(a2);
        Assert.assertEquals(b1.getVersion(), b1InStore.getVersion());
        Assert.assertEquals(b2.getVersion(), b2InStore.getVersion());
        Assert.assertTrue(remoteBucketsInStore.size() == 5);

        //Should update versions map
        //versions map contains versions for all remote buckets (4).
        Map<Address, Long> versionsInStore = getVersions(store);
        Assert.assertEquals(4, versionsInStore.size());
        Assert.assertEquals((Long)b1.getVersion(), versionsInStore.get(a1));
        Assert.assertEquals((Long)b2.getVersion(), versionsInStore.get(a2));
        Assert.assertEquals((Long)b3New.getVersion(), versionsInStore.get(a3));
        Assert.assertEquals((Long)b4.getVersion(), versionsInStore.get(a4));

        //Send older version of bucket
        remoteBuckets.clear();
        remoteBuckets.put(a3, b3);
        store.tell(new UpdateRemoteBuckets<>(remoteBuckets), noSender());

        //Should NOT update a3
        remoteBucketsInStore = getBuckets(store);
        b3InStore = remoteBucketsInStore.get(a3);
        Assert.assertEquals(b3InStore.getVersion(), b3New.getVersion());
    }

    /**
     * Create BucketStore actor and returns the underlying instance of BucketStore class.
     *
     * @return instance of BucketStore class
     */
    private ActorRef createStore() {
        return kit.childActorOf(Props.create(TestingBucketStore.class,
            new RemoteRpcProviderConfig(system.settings().config()), "testStore", new T()));
    }

    @SuppressWarnings("unchecked")
    private static Map<Address, Bucket<T>> getBuckets(final ActorRef store) throws Exception {
        final GetAllBucketsReply<T> result = (GetAllBucketsReply) Await.result(Patterns.ask(store, new GetAllBuckets(),
                Timeout.apply(1, TimeUnit.SECONDS)), Duration.Inf());
        return result.getBuckets();
    }

    @SuppressWarnings("unchecked")
    private static Map<Address, Long> getVersions(final ActorRef store) throws Exception {
        return ((GetBucketVersionsReply) Await.result(Patterns.ask(store, new GetBucketVersions(),
            Timeout.apply(1, TimeUnit.SECONDS)), Duration.Inf())).getVersions();
    }

    private static final class TestingBucketStore extends BucketStore<T> {

        private final List<ActorRef> toNotify = new ArrayList<>();

        TestingBucketStore(final RemoteRpcProviderConfig config,
                                  final String persistenceId,
                                  final T initialData) {
            super(config, persistenceId, initialData);
        }

        @Override
        protected void handleCommand(Object message) throws Exception {
            if (message instanceof WaitUntilDonePersisting) {
                handlePersistAsk();
            } else if (message instanceof SaveSnapshotSuccess) {
                super.handleCommand(message);
                handleSnapshotSuccess();
            } else {
                super.handleCommand(message);
            }
        }

        private void handlePersistAsk() {
            if (isPersisting()) {
                toNotify.add(getSender());
            } else {
                getSender().tell(new Success(null), noSender());
            }
        }

        private void handleSnapshotSuccess() {
            toNotify.forEach(ref -> ref.tell(new Success(null), noSender()));
        }
    }

    /**
     * Message sent to the TestingBucketStore that replies with success once the actor is done persisting.
     */
    private static final class WaitUntilDonePersisting {

    }
}
