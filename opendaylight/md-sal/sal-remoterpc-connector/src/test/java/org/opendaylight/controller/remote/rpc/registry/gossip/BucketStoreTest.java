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

        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster"));
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

    @Test
    public void testReceiveUpdateBucket_WhenInputBucketShouldUpdateVersion(){
        Bucket bucket = new BucketImpl();
        Long expectedVersion = bucket.getVersion();

        mockStore.receiveUpdateBucket(bucket);

        Assert.assertEquals(bucket, mockStore.getLocalBucket());
        Assert.assertEquals(expectedVersion, mockStore.getLocalBucket().getVersion());
    }

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