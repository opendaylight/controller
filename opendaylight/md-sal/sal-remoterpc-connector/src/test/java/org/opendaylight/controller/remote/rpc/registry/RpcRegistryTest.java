package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.ChildActorPath;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.Uninterruptibles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.api.RpcRouter.RouteIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RpcRegistryTest {

    private static ActorSystem node1;
    private static ActorSystem node2;
    private static ActorSystem node3;

    private ActorRef registry1;
    private ActorRef registry2;
    private ActorRef registry3;

    private int routeIdCounter = 1;

    @BeforeClass
    public static void staticSetup() throws InterruptedException {
      RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").build();
      RemoteRpcProviderConfig config2 = new RemoteRpcProviderConfig.Builder("memberB").build();
      RemoteRpcProviderConfig config3 = new RemoteRpcProviderConfig.Builder("memberC").build();
      node1 = ActorSystem.create("opendaylight-rpc", config1.get());
      node2 = ActorSystem.create("opendaylight-rpc", config2.get());
      node3 = ActorSystem.create("opendaylight-rpc", config3.get());
    }

    @AfterClass
    public static void staticTeardown() {
      JavaTestKit.shutdownActorSystem(node1);
      JavaTestKit.shutdownActorSystem(node2);
      JavaTestKit.shutdownActorSystem(node3);
    }

    @Before
    public void setup() {
        registry1 = node1.actorOf(Props.create(RpcRegistry.class));
        registry2 = node2.actorOf(Props.create(RpcRegistry.class));
        registry3 = node3.actorOf(Props.create(RpcRegistry.class));
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
    }

    /**
     * One node cluster. 1. Register rpc, ensure router can be found 2. Then remove rpc, ensure its
     * deleted
     *
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    @Test
    public void testAddRemoveRpcOnSameNode() throws Exception {

        System.out.println("testAddRemoveRpcOnSameNode starting");

        final JavaTestKit mockBroker = new JavaTestKit(node1);

        final ActorPath bucketStorePath = new ChildActorPath(registry1.path(), "store");

        Address nodeAddress = node1.provider().getDefaultAddress();

        // Add rpc on node 1
        registry1.tell(new SetLocalRouter(mockBroker.getRef()), mockBroker.getRef());

        List<RpcRouter.RouteIdentifier<?, ?, ?>> addedRouteIds = createRouteIds();

        registry1.tell(new AddOrUpdateRoutes(addedRouteIds), mockBroker.getRef());

        // Bucket store should get an update bucket message. Updated bucket contains added rpc.

        Map<Address, Bucket> buckets = retrieveBuckets(bucketStorePath, mockBroker, nodeAddress);
        verifyBucket(buckets.get(nodeAddress), addedRouteIds);

        Map<Address, Long> versions = retrieveVersions(bucketStorePath, mockBroker);
        Assert.assertEquals("Version for bucket " + nodeAddress, buckets.get(nodeAddress).getVersion(),
                versions.get(nodeAddress));

        // Now remove rpc
        registry1.tell(new RemoveRoutes(addedRouteIds), mockBroker.getRef());

        // Bucket store should get an update bucket message. Rpc is removed in the updated bucket

        verifyEmptyBucket(mockBroker, bucketStorePath, nodeAddress);

        System.out.println("testAddRemoveRpcOnSameNode ending");

    }

    /**
     * Three node cluster. 1. Register rpc on 1 node, ensure 2nd node gets updated 2. Remove rpc on
     * 1 node, ensure 2nd node gets updated
     *
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    @Test
    public void testRpcAddRemoveInCluster() throws Exception {

        System.out.println("testRpcAddRemoveInCluster starting");

        final JavaTestKit mockBroker1 = new JavaTestKit(node1);
        final JavaTestKit mockBroker2 = new JavaTestKit(node2);

        // install probe on node2's bucket store
        final ActorPath bucketStorePath2 = new ChildActorPath(registry2.path(), "store");

        List<RpcRouter.RouteIdentifier<?, ?, ?>> addedRouteIds = createRouteIds();

        Address node1Address = node1.provider().getDefaultAddress();

        // Add rpc on node 1
        registry1.tell(new SetLocalRouter(mockBroker1.getRef()), mockBroker1.getRef());
        registry1.tell(new AddOrUpdateRoutes(addedRouteIds), mockBroker1.getRef());

        // Bucket store on node2 should get a message to update its local copy of remote buckets

        Map<Address, Bucket> buckets = retrieveBuckets(bucketStorePath2, mockBroker2, node1Address);
        verifyBucket(buckets.get(node1Address), addedRouteIds);

        // Now remove
        registry1.tell(new RemoveRoutes(addedRouteIds), mockBroker1.getRef());

        // Bucket store on node2 should get a message to update its local copy of remote buckets.
        // Wait for the bucket for node1 to be empty.

        verifyEmptyBucket(mockBroker2, bucketStorePath2, node1Address);

        System.out.println("testRpcAddRemoveInCluster ending");
    }

    private void verifyEmptyBucket(JavaTestKit testKit, ActorPath bucketStorePath, Address address)
            throws AssertionError {
        Map<Address, Bucket> buckets;
        int nTries = 0;
        while(true) {
            buckets = retrieveBuckets(bucketStorePath, testKit, address);

            try {
                verifyBucket(buckets.get(address), Collections.<RouteIdentifier<?, ?, ?>>emptyList());
                break;
            } catch (AssertionError e) {
                if(++nTries >= 50) {
                    throw e;
                }
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Three node cluster. Register rpc on 2 nodes. Ensure 3rd gets updated.
     *
     * @throws Exception
     */
    @Test
    public void testRpcAddedOnMultiNodes() throws Exception {

        final JavaTestKit mockBroker1 = new JavaTestKit(node1);
        final JavaTestKit mockBroker2 = new JavaTestKit(node2);
        final JavaTestKit mockBroker3 = new JavaTestKit(node3);

        registry3.tell(new SetLocalRouter(mockBroker3.getRef()), mockBroker3.getRef());

        // install probe on node 3
        final ActorPath bucketStorePath = new ChildActorPath(registry3.path(), "store");

        // Add rpc on node 1
        List<RpcRouter.RouteIdentifier<?, ?, ?>> addedRouteIds1 = createRouteIds();
        registry1.tell(new SetLocalRouter(mockBroker1.getRef()), mockBroker1.getRef());
        registry1.tell(new AddOrUpdateRoutes(addedRouteIds1), mockBroker1.getRef());

        // Add rpc on node 2
        List<RpcRouter.RouteIdentifier<?, ?, ?>> addedRouteIds2 = createRouteIds();
        registry2.tell(new SetLocalRouter(mockBroker2.getRef()), mockBroker2.getRef());
        registry2.tell(new AddOrUpdateRoutes(addedRouteIds2), mockBroker2.getRef());

        Address node1Address = node1.provider().getDefaultAddress();
        Address node2Address = node2.provider().getDefaultAddress();

        Map<Address, Bucket> buckets = retrieveBuckets(bucketStorePath, mockBroker3, node1Address,
                node2Address);

        verifyBucket(buckets.get(node1Address), addedRouteIds1);
        verifyBucket(buckets.get(node2Address), addedRouteIds2);

        Map<Address, Long> versions = retrieveVersions(bucketStorePath, mockBroker3);
        Assert.assertEquals("Version for bucket " + node1Address, buckets.get(node1Address).getVersion(),
                versions.get(node1Address));
        Assert.assertEquals("Version for bucket " + node2Address, buckets.get(node2Address).getVersion(),
                versions.get(node2Address));
    }

    private Map<Address, Long> retrieveVersions(ActorPath bucketStorePath, JavaTestKit testKit) {
        ActorSelection bucketStore = node3.actorSelection(bucketStorePath);
        bucketStore.tell(new GetBucketVersions(), testKit.getRef());
        GetBucketVersionsReply reply = testKit.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                GetBucketVersionsReply.class);
        return reply.getVersions();
    }

    private void verifyBucket(Bucket<RoutingTableImpl> bucket, List<RouteIdentifier<?, ?, ?>> expRouteIds) {
        RoutingTable table = bucket.getData();
        Assert.assertNotNull("Bucket RoutingTable is null", table);
        for(RouteIdentifier<?, ?, ?> r: expRouteIds) {
            if(!table.contains(r)) {
                Assert.fail("RoutingTable does not contain " + r + ". Actual: " + table);
            }
        }

        Assert.assertEquals("RoutingTable size", expRouteIds.size(), table.size());
    }

    private Map<Address, Bucket> retrieveBuckets(ActorPath bucketStorePath, JavaTestKit testKit,
            Address... addresses) {
        ActorSelection bucketStore = node3.actorSelection(bucketStorePath);
        int nTries = 0;
        while(true) {
            bucketStore.tell(new GetAllBuckets(), testKit.getRef());
            GetAllBucketsReply reply = testKit.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                    GetAllBucketsReply.class);

            Map<Address, Bucket> buckets = reply.getBuckets();
            boolean foundAll = true;
            for(Address addr: addresses) {
                Bucket bucket = buckets.get(addr);
                if(bucket  == null) {
                    foundAll = false;
                    break;
                }
            }

            if(foundAll) {
                return buckets;
            }

            if(++nTries >= 50) {
                Assert.fail("Missing expected buckets for addresses: " + Arrays.toString(addresses)
                        + ", Actual: " + buckets);
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddRoutesConcurrency() throws Exception {
        final JavaTestKit testKit = new JavaTestKit(node1);

        registry1.tell(new SetLocalRouter(testKit.getRef()), ActorRef.noSender());

        ActorPath bucketStorePath = new ChildActorPath(registry1.path(), "store");
        ActorSelection bucketStore = node1.actorSelection(bucketStorePath);

        final int nRoutes = 500;
        final RouteIdentifier<?, ?, ?>[] added = new RouteIdentifier<?, ?, ?>[nRoutes];
        for(int i = 0; i < nRoutes; i++) {
            final RouteIdentifierImpl routeId = new RouteIdentifierImpl(null,
                    new QName(new URI("/mockrpc"), "type" + i), null);
            added[i] = routeId;

            //Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            registry1.tell(new AddOrUpdateRoutes(Arrays.<RouteIdentifier<?, ?, ?>>asList(routeId)),
                    ActorRef.noSender());
        }

        GetAllBuckets getAllBuckets = new GetAllBuckets();
        FiniteDuration duration = Duration.create(3, TimeUnit.SECONDS);
        int nTries = 0;
        while(true) {
            bucketStore.tell(getAllBuckets, testKit.getRef());
            GetAllBucketsReply reply = testKit.expectMsgClass(duration, GetAllBucketsReply.class);

            Bucket<RoutingTableImpl> localBucket = reply.getBuckets().values().iterator().next();
            RoutingTable table = localBucket.getData();
            if(table != null && table.size() == nRoutes) {
                for(RouteIdentifier<?, ?, ?> r: added) {
                    Assert.assertEquals("RoutingTable contains " + r, true, table.contains(r));
                }

                break;
            }

            if(++nTries >= 50) {
                Assert.fail("Expected # routes: " + nRoutes + ", Actual: " + table.size());
            }

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
    }

    private List<RpcRouter.RouteIdentifier<?, ?, ?>> createRouteIds() throws URISyntaxException {
        QName type = new QName(new URI("/mockrpc"), "mockrpc" + routeIdCounter++);
        List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = new ArrayList<>();
        routeIds.add(new RouteIdentifierImpl(null, type, null));
        return routeIds;
    }
}
