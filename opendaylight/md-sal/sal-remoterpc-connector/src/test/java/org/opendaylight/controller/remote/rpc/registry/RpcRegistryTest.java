/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;

import static org.junit.Assert.fail;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons.GET_ALL_BUCKETS;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons.GET_BUCKET_VERSIONS;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.UniqueAddress;
import akka.testkit.JavaTestKit;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RpcRegistryTest {
    private static final Logger LOG = LoggerFactory.getLogger(RpcRegistryTest.class);

    private static ActorSystem node1;
    private static ActorSystem node2;
    private static ActorSystem node3;

    private JavaTestKit invoker1;
    private JavaTestKit invoker2;
    private JavaTestKit invoker3;
    private JavaTestKit registrar1;
    private JavaTestKit registrar2;
    private JavaTestKit registrar3;
    private ActorRef registry1;
    private ActorRef registry2;
    private ActorRef registry3;

    private int routeIdCounter = 1;

    @BeforeClass
    public static void staticSetup() throws InterruptedException {
        AkkaConfigurationReader reader = ConfigFactory::load;

        RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        RemoteRpcProviderConfig config2 = new RemoteRpcProviderConfig.Builder("memberB").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        RemoteRpcProviderConfig config3 = new RemoteRpcProviderConfig.Builder("memberC").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        node1 = ActorSystem.create("opendaylight-rpc", config1.get());
        node2 = ActorSystem.create("opendaylight-rpc", config2.get());
        node3 = ActorSystem.create("opendaylight-rpc", config3.get());

        waitForMembersUp(node1, Cluster.get(node2).selfUniqueAddress(), Cluster.get(node3).selfUniqueAddress());
        waitForMembersUp(node2, Cluster.get(node1).selfUniqueAddress(), Cluster.get(node3).selfUniqueAddress());
    }

    static void waitForMembersUp(final ActorSystem node, final UniqueAddress... addresses) {
        Set<UniqueAddress> otherMembersSet = Sets.newHashSet(addresses);
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            CurrentClusterState state = Cluster.get(node).state();
            for (Member m : state.getMembers()) {
                if (m.status() == MemberStatus.up() && otherMembersSet.remove(m.uniqueAddress())
                        && otherMembersSet.isEmpty()) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member(s) " + otherMembersSet + " are not Up");
    }

    @AfterClass
    public static void staticTeardown() {
        JavaTestKit.shutdownActorSystem(node1);
        JavaTestKit.shutdownActorSystem(node2);
        JavaTestKit.shutdownActorSystem(node3);
    }

    @Before
    public void setup() {
        invoker1 = new JavaTestKit(node1);
        registrar1 = new JavaTestKit(node1);
        registry1 = node1.actorOf(RpcRegistry.props(config(node1), invoker1.getRef(), registrar1.getRef()));
        invoker2 = new JavaTestKit(node2);
        registrar2 = new JavaTestKit(node2);
        registry2 = node2.actorOf(RpcRegistry.props(config(node2), invoker2.getRef(), registrar2.getRef()));
        invoker3 = new JavaTestKit(node3);
        registrar3 = new JavaTestKit(node3);
        registry3 = node3.actorOf(RpcRegistry.props(config(node3), invoker3.getRef(), registrar3.getRef()));
    }

    private static RemoteRpcProviderConfig config(final ActorSystem node) {
        return new RemoteRpcProviderConfig(node.settings().config());
    }

    @After
    public void teardown() {
        if (registry1 != null) {
            node1.stop(registry1);
        }
        if (registry2 != null) {
            node2.stop(registry2);
        }
        if (registry3 != null) {
            node3.stop(registry3);
        }

        if (invoker1 != null) {
            node1.stop(invoker1.getRef());
        }
        if (invoker2 != null) {
            node2.stop(invoker2.getRef());
        }
        if (invoker3 != null) {
            node3.stop(invoker3.getRef());
        }

        if (registrar1 != null) {
            node1.stop(registrar1.getRef());
        }
        if (registrar2 != null) {
            node2.stop(registrar2.getRef());
        }
        if (registrar3 != null) {
            node3.stop(registrar3.getRef());
        }
    }

    /**
     * One node cluster. 1. Register rpc, ensure router can be found 2. Then remove rpc, ensure its
     * deleted
     */
    @Test
    public void testAddRemoveRpcOnSameNode() throws Exception {
        LOG.info("testAddRemoveRpcOnSameNode starting");

        Address nodeAddress = node1.provider().getDefaultAddress();

        // Add rpc on node 1

        List<DOMRpcIdentifier> addedRouteIds = createRouteIds();

        registry1.tell(new AddOrUpdateRoutes(addedRouteIds), ActorRef.noSender());

        // Bucket store should get an update bucket message. Updated bucket contains added rpc.
        final JavaTestKit testKit = new JavaTestKit(node1);

        Map<Address, Bucket<RoutingTable>> buckets = retrieveBuckets(registry1, testKit, nodeAddress);
        verifyBucket(buckets.get(nodeAddress), addedRouteIds);

        Map<Address, Long> versions = retrieveVersions(registry1, testKit);
        Assert.assertEquals("Version for bucket " + nodeAddress, (Long) buckets.get(nodeAddress).getVersion(),
                versions.get(nodeAddress));

        // Now remove rpc
        registry1.tell(new RemoveRoutes(addedRouteIds), ActorRef.noSender());

        // Bucket store should get an update bucket message. Rpc is removed in the updated bucket

        verifyEmptyBucket(testKit, registry1, nodeAddress);

        LOG.info("testAddRemoveRpcOnSameNode ending");

    }

    /**
     * Three node cluster. 1. Register rpc on 1 node, ensure 2nd node gets updated 2. Remove rpc on
     * 1 node, ensure 2nd node gets updated
     */
    @Test
    public void testRpcAddRemoveInCluster() throws Exception {

        LOG.info("testRpcAddRemoveInCluster starting");

        List<DOMRpcIdentifier> addedRouteIds = createRouteIds();

        Address node1Address = node1.provider().getDefaultAddress();

        // Add rpc on node 1
        registry1.tell(new AddOrUpdateRoutes(addedRouteIds), ActorRef.noSender());

        // Bucket store on node2 should get a message to update its local copy of remote buckets
        final JavaTestKit testKit = new JavaTestKit(node2);

        Map<Address, Bucket<RoutingTable>> buckets = retrieveBuckets(registry2, testKit, node1Address);
        verifyBucket(buckets.get(node1Address), addedRouteIds);

        // Now remove
        registry1.tell(new RemoveRoutes(addedRouteIds), ActorRef.noSender());

        // Bucket store on node2 should get a message to update its local copy of remote buckets.
        // Wait for the bucket for node1 to be empty.

        verifyEmptyBucket(testKit, registry2, node1Address);

        LOG.info("testRpcAddRemoveInCluster ending");
    }

    private void verifyEmptyBucket(final JavaTestKit testKit, final ActorRef registry, final Address address)
            throws AssertionError {
        Map<Address, Bucket<RoutingTable>> buckets;
        int numTries = 0;
        while (true) {
            buckets = retrieveBuckets(registry1, testKit, address);

            try {
                verifyBucket(buckets.get(address), Collections.emptyList());
                break;
            } catch (AssertionError e) {
                if (++numTries >= 50) {
                    throw e;
                }
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Three node cluster. Register rpc on 2 nodes. Ensure 3rd gets updated.
     */
    @Test
    public void testRpcAddedOnMultiNodes() throws Exception {
        final JavaTestKit testKit = new JavaTestKit(node3);

        // Add rpc on node 1
        List<DOMRpcIdentifier> addedRouteIds1 = createRouteIds();
        registry1.tell(new AddOrUpdateRoutes(addedRouteIds1), ActorRef.noSender());

        final UpdateRemoteEndpoints req1 = registrar3.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
            UpdateRemoteEndpoints.class);

        // Add rpc on node 2
        List<DOMRpcIdentifier> addedRouteIds2 = createRouteIds();
        registry2.tell(new AddOrUpdateRoutes(addedRouteIds2), ActorRef.noSender());

        final UpdateRemoteEndpoints req2 = registrar3.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
            UpdateRemoteEndpoints.class);
        Address node2Address = node2.provider().getDefaultAddress();
        Address node1Address = node1.provider().getDefaultAddress();

        Map<Address, Bucket<RoutingTable>> buckets = retrieveBuckets(registry3, testKit, node1Address,
                node2Address);

        verifyBucket(buckets.get(node1Address), addedRouteIds1);
        verifyBucket(buckets.get(node2Address), addedRouteIds2);

        Map<Address, Long> versions = retrieveVersions(registry3, testKit);
        Assert.assertEquals("Version for bucket " + node1Address, (Long) buckets.get(node1Address).getVersion(),
                versions.get(node1Address));
        Assert.assertEquals("Version for bucket " + node2Address, (Long) buckets.get(node2Address).getVersion(),
                versions.get(node2Address));

        assertEndpoints(req1, node1Address, invoker1);
        assertEndpoints(req2, node2Address, invoker2);

    }

    private static void assertEndpoints(final UpdateRemoteEndpoints msg, final Address address,
            final JavaTestKit invoker) {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = msg.getEndpoints();
        Assert.assertEquals(1, endpoints.size());

        final Optional<RemoteRpcEndpoint> maybeEndpoint = endpoints.get(address);
        Assert.assertNotNull(maybeEndpoint);
        Assert.assertTrue(maybeEndpoint.isPresent());

        final RemoteRpcEndpoint endpoint = maybeEndpoint.get();
        final ActorRef router = endpoint.getRouter();
        Assert.assertNotNull(router);

        router.tell("hello", ActorRef.noSender());
        final String s = invoker.expectMsgClass(Duration.create(3, TimeUnit.SECONDS), String.class);
        Assert.assertEquals("hello", s);
    }

    private static Map<Address, Long> retrieveVersions(final ActorRef bucketStore, final JavaTestKit testKit) {
        bucketStore.tell(GET_BUCKET_VERSIONS, testKit.getRef());
        @SuppressWarnings("unchecked")
        final Map<Address, Long> reply = testKit.expectMsgClass(Duration.create(3, TimeUnit.SECONDS), Map.class);
        return reply;
    }

    private static void verifyBucket(final Bucket<RoutingTable> bucket, final List<DOMRpcIdentifier> expRouteIds) {
        RoutingTable table = bucket.getData();
        Assert.assertNotNull("Bucket RoutingTable is null", table);
        for (DOMRpcIdentifier r : expRouteIds) {
            if (!table.contains(r)) {
                Assert.fail("RoutingTable does not contain " + r + ". Actual: " + table);
            }
        }

        Assert.assertEquals("RoutingTable size", expRouteIds.size(), table.size());
    }

    private static Map<Address, Bucket<RoutingTable>> retrieveBuckets(final ActorRef bucketStore,
            final JavaTestKit testKit, final Address... addresses) {
        int numTries = 0;
        while (true) {
            bucketStore.tell(GET_ALL_BUCKETS, testKit.getRef());
            @SuppressWarnings("unchecked")
            Map<Address, Bucket<RoutingTable>> buckets = testKit.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                    Map.class);

            boolean foundAll = true;
            for (Address addr : addresses) {
                Bucket<RoutingTable> bucket = buckets.get(addr);
                if (bucket == null) {
                    foundAll = false;
                    break;
                }
            }

            if (foundAll) {
                return buckets;
            }

            if (++numTries >= 50) {
                Assert.fail("Missing expected buckets for addresses: " + Arrays.toString(addresses)
                        + ", Actual: " + buckets);
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void testAddRoutesConcurrency() throws Exception {
        final JavaTestKit testKit = new JavaTestKit(node1);

        final int nRoutes = 500;
        final Collection<DOMRpcIdentifier> added = new ArrayList<>(nRoutes);
        for (int i = 0; i < nRoutes; i++) {
            final DOMRpcIdentifier routeId = DOMRpcIdentifier.create(SchemaPath.create(true,
                    new QName(new URI("/mockrpc"), "type" + i)));
            added.add(routeId);

            //Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            registry1.tell(new AddOrUpdateRoutes(Arrays.asList(routeId)),
                    ActorRef.noSender());
        }

        FiniteDuration duration = Duration.create(3, TimeUnit.SECONDS);
        int numTries = 0;
        while (true) {
            registry1.tell(GET_ALL_BUCKETS, testKit.getRef());
            @SuppressWarnings("unchecked")
            Map<Address, Bucket<RoutingTable>> buckets = testKit.expectMsgClass(duration, Map.class);

            Bucket<RoutingTable> localBucket = buckets.values().iterator().next();
            RoutingTable table = localBucket.getData();
            if (table != null && table.size() == nRoutes) {
                for (DOMRpcIdentifier r : added) {
                    Assert.assertTrue("RoutingTable contains " + r, table.contains(r));
                }

                break;
            }

            if (++numTries >= 50) {
                Assert.fail("Expected # routes: " + nRoutes + ", Actual: " + table.size());
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    private List<DOMRpcIdentifier> createRouteIds() throws URISyntaxException {
        QName type = new QName(new URI("/mockrpc"), "mockrpc" + routeIdCounter++);
        List<DOMRpcIdentifier> routeIds = new ArrayList<>(1);
        routeIds.add(DOMRpcIdentifier.create(SchemaPath.create(true, type)));
        return routeIds;
    }
}
