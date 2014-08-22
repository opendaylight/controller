package org.opendaylight.controller.remote.rpc.registry;


import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.ChildActorPath;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.base.Predicate;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.utils.ConditionalProbe;
import org.opendaylight.yangtools.yang.common.QName;
import scala.concurrent.duration.FiniteDuration;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;

public class RpcRegistryTest {

  private static ActorSystem node1;
  private static ActorSystem node2;
  private static ActorSystem node3;

  private ActorRef registry1;
  private ActorRef registry2;
  private ActorRef registry3;

  @BeforeClass
  public static void setup() throws InterruptedException {
    node1 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberA"));
    node2 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberB"));
    node3 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberC"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(node1);
    JavaTestKit.shutdownActorSystem(node2);
    JavaTestKit.shutdownActorSystem(node3);
    if (node1 != null)
      node1.shutdown();
    if (node2 != null)
      node2.shutdown();
    if (node3 != null)
      node3.shutdown();

  }

  @Before
  public void createRpcRegistry() throws InterruptedException {
    registry1 = node1.actorOf(Props.create(RpcRegistry.class));
    registry2 = node2.actorOf(Props.create(RpcRegistry.class));
    registry3 = node3.actorOf(Props.create(RpcRegistry.class));
  }

  @After
  public void stopRpcRegistry() throws InterruptedException {
    if (registry1 != null)
      node1.stop(registry1);
    if (registry2 != null)
      node2.stop(registry2);
    if (registry3 != null)
      node3.stop(registry3);
  }

  /**
   * One node cluster.
   * 1. Register rpc, ensure router can be found
   * 2. Then remove rpc, ensure its deleted
   *
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  @Test
  public void testAddRemoveRpcOnSameNode() throws URISyntaxException, InterruptedException {

    final JavaTestKit mockBroker = new JavaTestKit(node1);

    final ActorPath bucketStorePath = new ChildActorPath(registry1.path(), "store");

    //install probe
    final JavaTestKit probe1 = createProbeForMessage(
        node1, bucketStorePath, Messages.BucketStoreMessages.UpdateBucket.class);

    //Add rpc on node 1
    registry1.tell(new SetLocalRouter(mockBroker.getRef()), mockBroker.getRef());
    registry1.tell(getAddRouteMessage(), mockBroker.getRef());

    //Bucket store should get an update bucket message. Updated bucket contains added rpc.
    probe1.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateBucket.class);

    //Now remove rpc
    registry1.tell(getRemoveRouteMessage(), mockBroker.getRef());

    //Bucket store should get an update bucket message. Rpc is removed in the updated bucket
    probe1.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateBucket.class);


  }


  /**
   * Three node cluster.
   * 1. Register rpc on 1 node, ensure 2nd node gets updated
   * 2. Remove rpc on 1 node, ensure 2nd node gets updated
   *
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  @Test
  public void testRpcAddRemoveInCluster() throws URISyntaxException, InterruptedException {

    final JavaTestKit mockBroker1 = new JavaTestKit(node1);

    //install probe on node2's bucket store
    final ActorPath bucketStorePath = new ChildActorPath(registry2.path(), "store");
    final JavaTestKit probe2 = createProbeForMessage(
        node2, bucketStorePath, Messages.BucketStoreMessages.UpdateRemoteBuckets.class);

    //Add rpc on node 1
    registry1.tell(new SetLocalRouter(mockBroker1.getRef()), mockBroker1.getRef());
    registry1.tell(getAddRouteMessage(), mockBroker1.getRef());

    //Bucket store on node2 should get a message to update its local copy of remote buckets
    probe2.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateRemoteBuckets.class);

    //Now remove
    registry1.tell(getRemoveRouteMessage(), mockBroker1.getRef());

    //Bucket store on node2 should get a message to update its local copy of remote buckets
    probe2.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateRemoteBuckets.class);

  }

  /**
   * Three node cluster.
   * Register rpc on 2 nodes. Ensure 3rd gets updated.
   *
   * @throws Exception
   */
  @Test
  public void testRpcAddedOnMultiNodes() throws Exception {

    final JavaTestKit mockBroker1 = new JavaTestKit(node1);
    final JavaTestKit mockBroker2 = new JavaTestKit(node2);
    final JavaTestKit mockBroker3 = new JavaTestKit(node3);

    registry3.tell(new SetLocalRouter(mockBroker3.getRef()), mockBroker3.getRef());

    //install probe on node 3
    final ActorPath bucketStorePath = new ChildActorPath(registry3.path(), "store");
    final JavaTestKit probe3 = createProbeForMessage(
        node3, bucketStorePath, Messages.BucketStoreMessages.UpdateRemoteBuckets.class);


    //Add rpc on node 1
    registry1.tell(new SetLocalRouter(mockBroker1.getRef()), mockBroker1.getRef());
    registry1.tell(getAddRouteMessage(), mockBroker1.getRef());

    probe3.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateRemoteBuckets.class);


    //Add same rpc on node 2
    registry2.tell(new SetLocalRouter(mockBroker2.getRef()), mockBroker2.getRef());
    registry2.tell(getAddRouteMessage(), mockBroker2.getRef());

    probe3.expectMsgClass(
        FiniteDuration.apply(10, TimeUnit.SECONDS),
        Messages.BucketStoreMessages.UpdateRemoteBuckets.class);
  }

  private JavaTestKit createProbeForMessage(ActorSystem node, ActorPath subjectPath, final Class clazz) {
    final JavaTestKit probe = new JavaTestKit(node);

    ConditionalProbe conditionalProbe =
        new ConditionalProbe(probe.getRef(), new Predicate() {
          @Override
          public boolean apply(@Nullable Object input) {
            return clazz.equals(input.getClass());
          }
        });

    ActorSelection subject = node.actorSelection(subjectPath);
    subject.tell(conditionalProbe, ActorRef.noSender());

    return probe;

  }

  private AddOrUpdateRoutes getAddRouteMessage() throws URISyntaxException {
    return new AddOrUpdateRoutes(createRouteIds());
  }

  private RemoveRoutes getRemoveRouteMessage() throws URISyntaxException {
    return new RemoveRoutes(createRouteIds());
  }

  private List<RpcRouter.RouteIdentifier<?, ?, ?>> createRouteIds() throws URISyntaxException {
    QName type = new QName(new URI("/mockrpc"), "mockrpc");
    List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = new ArrayList<>();
    routeIds.add(new RouteIdentifierImpl(null, type, null));
    return routeIds;
  }

}
