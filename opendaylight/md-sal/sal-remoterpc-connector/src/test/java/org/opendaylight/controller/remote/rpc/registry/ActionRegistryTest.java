/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.Messages.UpdateActions;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.Messages.UpdateRemoteActionEndpoints;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionRegistryTest {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRegistryTest.class);

    private static ActorSystem node1;
    private static ActorSystem node2;
    private static ActorSystem node3;

    private TestKit invoker1;
    private TestKit invoker2;
    private TestKit invoker3;
    private TestKit registrar1;
    private TestKit registrar2;
    private TestKit registrar3;
    private ActorRef registry1;
    private ActorRef registry2;
    private ActorRef registry3;

    private int routeIdCounter = 1;

    @BeforeClass
    public static void staticSetup() {
        AkkaConfigurationReader reader = ConfigFactory::load;

        RemoteOpsProviderConfig config1 = new RemoteOpsProviderConfig.Builder("memberA").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        RemoteOpsProviderConfig config2 = new RemoteOpsProviderConfig.Builder("memberB").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        RemoteOpsProviderConfig config3 = new RemoteOpsProviderConfig.Builder("memberC").gossipTickInterval("200ms")
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
        TestKit.shutdownActorSystem(node1);
        TestKit.shutdownActorSystem(node2);
        TestKit.shutdownActorSystem(node3);
    }

    @Before
    public void setup() {
        invoker1 = new TestKit(node1);
        registrar1 = new TestKit(node1);
        registry1 = node1.actorOf(ActionRegistry.props(config(node1), invoker1.getRef(), registrar1.getRef()));
        invoker2 = new TestKit(node2);
        registrar2 = new TestKit(node2);
        registry2 = node2.actorOf(ActionRegistry.props(config(node2), invoker2.getRef(), registrar2.getRef()));
        invoker3 = new TestKit(node3);
        registrar3 = new TestKit(node3);
        registry3 = node3.actorOf(ActionRegistry.props(config(node3), invoker3.getRef(), registrar3.getRef()));
    }

    private static RemoteOpsProviderConfig config(final ActorSystem node) {
        return new RemoteOpsProviderConfig(node.settings().config());
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
     * One node cluster. 1. Register action, ensure router can be found 2. Then remove action, ensure its
     * deleted
     */
    @Test
    public void testAddRemoveActionOnSameNode() {
        LOG.info("testAddRemoveActionOnSameNode starting");

        Address nodeAddress = node1.provider().getDefaultAddress();

        // Add action on node 1

        List<DOMActionInstance> addedRouteIds = createRouteIds();

        registry1.tell(new ActionRegistry.Messages.UpdateActions(addedRouteIds,
                Collections.emptyList()), ActorRef.noSender());

        // Bucket store should get an update bucket message. Updated bucket contains added action.
        final TestKit testKit = new TestKit(node1);

        Map<Address, Bucket<ActionRoutingTable>> buckets = retrieveBuckets(registry1, testKit, nodeAddress);
        verifyBucket(buckets.get(nodeAddress), addedRouteIds);

        Map<Address, Long> versions = retrieveVersions(registry1, testKit);
        assertEquals("Version for bucket " + nodeAddress, (Long) buckets.get(nodeAddress).getVersion(),
                versions.get(nodeAddress));

        // Now remove action
        registry1.tell(new UpdateActions(Collections.emptyList(),addedRouteIds), ActorRef.noSender());

        // Bucket store should get an update bucket message. Action is removed in the updated bucket

        verifyEmptyBucket(testKit, registry1, nodeAddress);

        LOG.info("testAddRemoveActionOnSameNode ending");

    }

    /**
     * Three node cluster. 1. Register action on 1 node, ensure 2nd node gets updated 2. Remove action on
     * 1 node, ensure 2nd node gets updated
     */
    @Test
    public void testActionAddRemoveInCluster() {

        LOG.info("testActionAddRemoveInCluster starting");

        List<DOMActionInstance> addedRouteIds = createRouteIds();

        Address node1Address = node1.provider().getDefaultAddress();

        // Add action on node 1
        registry1.tell(new UpdateActions(addedRouteIds, Collections.emptyList()), ActorRef.noSender());

        // Bucket store on node2 should get a message to update its local copy of remote buckets
        final TestKit testKit = new TestKit(node2);

        Map<Address, Bucket<ActionRoutingTable>> buckets = retrieveBuckets(registry2, testKit, node1Address);
        verifyBucket(buckets.get(node1Address), addedRouteIds);

        // Now remove
        registry1.tell(new UpdateActions(Collections.emptyList(), addedRouteIds), ActorRef.noSender());

        // Bucket store on node2 should get a message to update its local copy of remote buckets.
        // Wait for the bucket for node1 to be empty.

        verifyEmptyBucket(testKit, registry2, node1Address);

        LOG.info("testActionAddRemoveInCluster ending");
    }

    private void verifyEmptyBucket(final TestKit testKit, final ActorRef registry, final Address address)
            throws AssertionError {
        Map<Address, Bucket<ActionRoutingTable>> buckets;
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
     * Three node cluster. Register action on 2 nodes. Ensure 3rd gets updated.
     */
    @Test
    public void testActionAddedOnMultiNodes() {
        final TestKit testKit = new TestKit(node3);

        // Add action on node 1
        List<DOMActionInstance> addedRouteIds1 = createRouteIds();
        registry1.tell(new UpdateActions(addedRouteIds1, Collections.emptyList()), ActorRef.noSender());

        final UpdateRemoteActionEndpoints req1 = registrar3.expectMsgClass(Duration.ofSeconds(3),
                UpdateRemoteActionEndpoints.class);

        // Add action on node 2
        List<DOMActionInstance> addedRouteIds2 = createRouteIds();
        registry2.tell(new UpdateActions(addedRouteIds2, Collections.emptyList()), ActorRef.noSender());

        final UpdateRemoteActionEndpoints req2 = registrar3.expectMsgClass(Duration.ofSeconds(3),
                UpdateRemoteActionEndpoints.class);
        Address node2Address = node2.provider().getDefaultAddress();
        Address node1Address = node1.provider().getDefaultAddress();

        Map<Address, Bucket<ActionRoutingTable>> buckets = retrieveBuckets(registry3, testKit, node1Address,
                node2Address);

        verifyBucket(buckets.get(node1Address), addedRouteIds1);
        verifyBucket(buckets.get(node2Address), addedRouteIds2);

        Map<Address, Long> versions = retrieveVersions(registry3, testKit);
        assertEquals("Version for bucket " + node1Address, (Long) buckets.get(node1Address).getVersion(),
                versions.get(node1Address));
        assertEquals("Version for bucket " + node2Address, (Long) buckets.get(node2Address).getVersion(),
                versions.get(node2Address));

        assertEndpoints(req1, node1Address, invoker1);
        assertEndpoints(req2, node2Address, invoker2);

    }

    private static void assertEndpoints(final ActionRegistry.Messages.UpdateRemoteActionEndpoints msg,
                                        final Address address, final TestKit invoker) {
        final Map<Address, Optional<RemoteActionEndpoint>> endpoints = msg.getActionEndpoints();
        assertEquals(1, endpoints.size());

        final Optional<RemoteActionEndpoint> maybeEndpoint = endpoints.get(address);
        assertNotNull(maybeEndpoint);
        assertTrue(maybeEndpoint.isPresent());

        final RemoteActionEndpoint endpoint = maybeEndpoint.get();
        final ActorRef router = endpoint.getRouter();
        assertNotNull(router);

        router.tell("hello", ActorRef.noSender());
        final String s = invoker.expectMsgClass(Duration.ofSeconds(3), String.class);
        assertEquals("hello", s);
    }

    private static Map<Address, Long> retrieveVersions(final ActorRef bucketStore, final TestKit testKit) {
        bucketStore.tell(GET_BUCKET_VERSIONS, testKit.getRef());
        @SuppressWarnings("unchecked")
        final Map<Address, Long> reply = testKit.expectMsgClass(Duration.ofSeconds(3), Map.class);
        return reply;
    }

    private static void verifyBucket(final Bucket<ActionRoutingTable> bucket,
                                     final List<DOMActionInstance> expRouteIds) {
        ActionRoutingTable table = bucket.getData();
        assertNotNull("Bucket ActionRoutingTable is null", table);
        for (DOMActionInstance r : expRouteIds) {
            if (!table.contains(r)) {
                fail("ActionRoutingTable does not contain " + r + ". Actual: " + table);
            }
        }

        assertEquals("ActionRoutingTable size", expRouteIds.size(), table.size());
    }

    private static Map<Address, Bucket<ActionRoutingTable>> retrieveBuckets(
            final ActorRef bucketStore, final TestKit testKit, final Address... addresses) {
        int numTries = 0;
        while (true) {
            bucketStore.tell(GET_ALL_BUCKETS, testKit.getRef());
            @SuppressWarnings("unchecked")
            Map<Address, Bucket<ActionRoutingTable>> buckets = testKit.expectMsgClass(Duration.ofSeconds(3),
                    Map.class);

            boolean foundAll = true;
            for (Address addr : addresses) {
                Bucket<ActionRoutingTable> bucket = buckets.get(addr);
                if (bucket == null) {
                    foundAll = false;
                    break;
                }
            }

            if (foundAll) {
                return buckets;
            }

            if (++numTries >= 50) {
                fail("Missing expected buckets for addresses: " + Arrays.toString(addresses)
                        + ", Actual: " + buckets);
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void testAddRoutesConcurrency() {
        final TestKit testKit = new TestKit(node1);

        final int nRoutes = 500;
        final Collection<DOMActionInstance> added = new ArrayList<>(nRoutes);
        for (int i = 0; i < nRoutes; i++) {
            QName type = QName.create("/mockaction", "mockaction" + routeIdCounter++);
            final DOMActionInstance routeId = DOMActionInstance.of(Absolute.of(type), LogicalDatastoreType.OPERATIONAL,
                    YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(type)));
            added.add(routeId);

            //Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            registry1.tell(new UpdateActions(Arrays.asList(routeId), Collections.emptyList()),
                    ActorRef.noSender());
        }

        int numTries = 0;
        while (true) {
            registry1.tell(GET_ALL_BUCKETS, testKit.getRef());
            @SuppressWarnings("unchecked")
            Map<Address, Bucket<ActionRoutingTable>> buckets = testKit.expectMsgClass(Duration.ofSeconds(3),
                    Map.class);

            Bucket<ActionRoutingTable> localBucket = buckets.values().iterator().next();
            ActionRoutingTable table = localBucket.getData();
            if (table != null && table.size() == nRoutes) {
                for (DOMActionInstance r : added) {
                    assertTrue("ActionRoutingTable contains " + r, table.contains(r));
                }

                break;
            }

            if (++numTries >= 50) {
                fail("Expected # routes: " + nRoutes + ", Actual: " + table.size());
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    private List<DOMActionInstance> createRouteIds() {
        QName type = QName.create("/mockaction", "mockaction" + routeIdCounter++);
        List<DOMActionInstance> routeIds = new ArrayList<>(1);
        routeIds.add(DOMActionInstance.of(Absolute.of(type), LogicalDatastoreType.OPERATIONAL,
            YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(type))));
        return routeIds;
    }
}
