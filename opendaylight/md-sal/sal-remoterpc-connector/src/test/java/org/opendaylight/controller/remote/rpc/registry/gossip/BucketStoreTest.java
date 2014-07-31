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
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

public class BucketStoreTest {

    private static ActorSystem system;
    private static BucketStore store;

    private BucketStore mockStore;

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

    @Before
    public void createMocks(){
        mockStore = spy(store);
    }

    @After
    public void resetMocks(){
        reset(mockStore);

    }
    //@Test
//    public void testBucketStore() throws Exception{
//        new JavaTestKit(system){{
//
//            ActorRef store = system.actorOf(Props.create(BucketStore.class, mockActorRef), "store");
//
//            Assert.assertNotNull(store);
//            System.out.println("Bucket store actor:" + store);
//            store.tell("Cryptic Message", ActorRef.noSender());
//            Thread.sleep(5000);
//        }};
//    }


    @Test
    public void testReceiveUpdateBucket_WhenNullInputBucketShouldInitialize(){

        mockStore.receiveUpdateBucket(null);

        Assert.assertNotNull(mockStore.getLocalBucket());
        Assert.assertTrue(mockStore.getLocalBucket().getVersion() > 0);
    }

    @Test
    public void testReceiveUpdateBucket_WhenInputBucketShouldUpdateVersion(){
        Bucket bucket = new BucketImpl();
        Long expectedVersion = bucket.getVersion();

        mockStore.receiveUpdateBucket(bucket);

        Assert.assertEquals(bucket, mockStore.getLocalBucket());
        Assert.assertEquals(expectedVersion, mockStore.getLocalBucket().getVersion());
    }

    @Test
    public void testGetAllBuckets_ShouldReturnACopy() {
        //add some buckets

        //verify if local exists

    }
    //@Test
//    public void testReceiveUpdateBucket() throws Exception {
//        final Props props = Props.create(BucketStore.class, mockActorRef);
//        final TestActorRef<BucketStore> testRef = TestActorRef.create(system, props, "testbucketstore");
//        final BucketStore store = testRef.underlyingActor();
//        BucketImpl mockBucket = new BucketImpl();
//        Copier data = mock(Copier.class);
//        when(data.copy()).thenReturn(data);
//        mockBucket.setData(data);
//
//
//        UpdateBucket mockMessage = mock(UpdateBucket.class);
//        when(mockMessage.getBucket()).thenReturn(mockBucket);
//
//        store.onReceive(mockMessage);
//
//        final Future<Object> fut =
//                akka.pattern.Patterns.ask(testRef, new Messages.BucketStoreMessages.GetAllBuckets(), 1000);
//
//        Assert.assertTrue(fut.isCompleted());
//
//        Messages.BucketStoreMessages.GetAllBucketsReply reply =
//                (Messages.BucketStoreMessages.GetAllBucketsReply) Await.result(fut, Duration.Zero());
//
//        Assert.assertEquals(1, reply.getBuckets().size());
//
//        System.out.println(reply.getBuckets());
//
//        for (Address address: reply.getBuckets().keySet()) {
//              System.out.println(address.protocol() + ":" + address.hostPort());
//        }
//
//
//        final Future<Object> fut1 =
//                akka.pattern.Patterns.ask(testRef, new Messages.BucketStoreMessages.GetBucketVersions(), 1000);
//
//        Assert.assertTrue(fut1.isCompleted());
//
//        Messages.BucketStoreMessages.GetBucketVersionsReply reply1 =
//                (Messages.BucketStoreMessages.GetBucketVersionsReply) Await.result(fut1, Duration.Zero());
//
//        System.out.println(reply1.getVersions());
//
//        mockBucket.setData(data);
//        store.onReceive(mockMessage);
//
//        final Future<Object> fut2 =
//                akka.pattern.Patterns.ask(testRef, new Messages.BucketStoreMessages.GetBucketVersions(), 1000);
//
//        Assert.assertTrue(fut2.isCompleted());
//
//        Messages.BucketStoreMessages.GetBucketVersionsReply reply2 =
//                (Messages.BucketStoreMessages.GetBucketVersionsReply) Await.result(fut2, Duration.Zero());
//
//        System.out.println(reply2.getVersions());
//    }

    /**
     * Create BucketStore actor and returns the underlying instance of BucketStore class.
     *
     * @return instance of BucketStore class
     */
    private static BucketStore createStore(){
        TestProbe mockActor = new TestProbe(system);
        ActorRef mockGossiper = mockActor.ref();
        final Props props = Props.create(BucketStore.class, mockGossiper);
        final TestActorRef<BucketStore> testRef = TestActorRef.create(system, props, "testStore");

        return testRef.underlyingActor();
    }
}