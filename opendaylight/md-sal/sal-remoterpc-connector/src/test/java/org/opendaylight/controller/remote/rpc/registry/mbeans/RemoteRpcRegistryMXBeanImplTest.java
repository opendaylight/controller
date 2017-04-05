/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.UniqueAddress;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.Map;
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
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import scala.concurrent.duration.Duration;

public class RemoteRpcRegistryMXBeanImplTest {

    private static final QName LOCAL_QNAME = QName.create("base", "local");
    private static final SchemaPath EMPTY_SCHEMA_PATH = SchemaPath.ROOT;
    private static final SchemaPath LOCAL_SCHEMA_PATH = SchemaPath.create(true, LOCAL_QNAME);

    private static ActorSystem system;
    private static ActorSystem node1;
    private static ActorSystem node2;

    private JavaTestKit invoker1;
    private JavaTestKit invoker2;
    private JavaTestKit registrar1;
    private JavaTestKit registrar2;

    private ActorRef registry1;
    private ActorRef registry2;

    private JavaTestKit invoker;
    private JavaTestKit registrar;
    private RemoteRpcRegistryMXBeanImpl mxBean;

    @BeforeClass
    public static void startSystem() {
        AkkaConfigurationReader reader = ConfigFactory::load;

        final RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA")
                .gossipTickInterval("200ms").withConfigReader(reader).build();
        final RemoteRpcProviderConfig config2 = new RemoteRpcProviderConfig.Builder("memberB")
                .gossipTickInterval("200ms").withConfigReader(reader).build();
        final RemoteRpcProviderConfig config3 = new RemoteRpcProviderConfig.Builder("memberC")
                .gossipTickInterval("200ms").withConfigReader(reader).build();

        node1 = ActorSystem.create("opendaylight-rpc", config1.get());
        node2 = ActorSystem.create("opendaylight-rpc", config2.get());
        system = ActorSystem.create("opendaylight-rpc", config3.get());

        waitForMembersUp(node1, Cluster.get(node2).selfUniqueAddress(), Cluster.get(system).selfUniqueAddress());
        waitForMembersUp(node2, Cluster.get(node1).selfUniqueAddress(), Cluster.get(system).selfUniqueAddress());
    }

    private static void waitForMembersUp(final ActorSystem node, final UniqueAddress... addresses) {
        final Set<UniqueAddress> otherMembersSet = Sets.newHashSet(addresses);
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            final ClusterEvent.CurrentClusterState state = Cluster.get(node).state();
            for (final Member m : state.getMembers()) {
                if (m.status() == MemberStatus.up() && otherMembersSet.remove(m.uniqueAddress())
                        && otherMembersSet.isEmpty()) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Member(s) " + otherMembersSet + " are not Up");
    }

    @Before
    public void setUp() throws Exception {
        final DOMRpcIdentifier remoteRpcIdentifier = DOMRpcIdentifier.create(
                EMPTY_SCHEMA_PATH, YangInstanceIdentifier.EMPTY);

        invoker1 = new JavaTestKit(node1);
        registrar1 = new JavaTestKit(node1);
        registry1 = node1.actorOf(RpcRegistry.props(config(node1), invoker1.getRef(), registrar1.getRef()));

        invoker2 = new JavaTestKit(node2);
        registrar2 = new JavaTestKit(node2);
        registry2 = node2.actorOf(RpcRegistry.props(config(node2), invoker2.getRef(), registrar2.getRef()));

        final DOMRpcIdentifier localRpcIdentifier = DOMRpcIdentifier.create(
                LOCAL_SCHEMA_PATH, YangInstanceIdentifier.of(LOCAL_QNAME));

        final DOMRpcIdentifier emptyRpcIdentifier = DOMRpcIdentifier.create(
                EMPTY_SCHEMA_PATH, YangInstanceIdentifier.EMPTY);

        invoker = new JavaTestKit(system);
        registrar = new JavaTestKit(system);

        final Props props = RpcRegistry.props(config(system), invoker.getRef(), registrar.getRef());
        final ActorRef supervisor = system.actorOf(props);

        registry1.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(
                Lists.newArrayList(emptyRpcIdentifier, localRpcIdentifier, remoteRpcIdentifier)), ActorRef.noSender());
        registrar.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                RpcRegistry.Messages.UpdateRemoteEndpoints.class);

        registry2.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(
                Lists.newArrayList(emptyRpcIdentifier, localRpcIdentifier, remoteRpcIdentifier)), ActorRef.noSender());
        registrar.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                RpcRegistry.Messages.UpdateRemoteEndpoints.class);

        supervisor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(
                Lists.newArrayList(emptyRpcIdentifier, localRpcIdentifier, remoteRpcIdentifier)), ActorRef.noSender());

        registrar1.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                RpcRegistry.Messages.UpdateRemoteEndpoints.class);
        registrar2.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                RpcRegistry.Messages.UpdateRemoteEndpoints.class);

        final RpcRegistry rpcRegistry = (RpcRegistry) new TestActorRef<>(
                system, props, supervisor, "testActor").underlyingActor();

        mxBean = new RemoteRpcRegistryMXBeanImpl(rpcRegistry);
        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
    }

    private static RemoteRpcProviderConfig config(final ActorSystem node) {
        return new RemoteRpcProviderConfig(node.settings().config());
    }

    @After
    public void tearDown() throws Exception {
        // system
        if (invoker != null) {
            system.stop(invoker.getRef());
        }
        if (registrar != null) {
            system.stop(registrar.getRef());
        }

        // node1
        if (invoker1 != null) {
            node1.stop(invoker1.getRef());
        }
        if (registrar1 != null) {
            node1.stop(registrar1.getRef());
        }
        if (registry1 != null) {
            node1.stop(registry1);
        }

        // node2
        if (invoker2 != null) {
            node2.stop(invoker2.getRef());
        }
        if (registrar2 != null) {
            node2.stop(registrar2.getRef());
        }
        if (registry2 != null) {
            node2.stop(registry2);
        }
    }

    @AfterClass
    public static void stopSystem() {
        JavaTestKit.shutdownActorSystem(system);
        JavaTestKit.shutdownActorSystem(node1);
        JavaTestKit.shutdownActorSystem(node2);
    }

    @Test
    public void testGetGlobalRpc() throws Exception {
        final Set<String> globalRpc = mxBean.getGlobalRpc();

        Assert.assertNotNull(globalRpc);
        Assert.assertEquals(1, globalRpc.size());

        final String rpc = globalRpc.iterator().next();
        Assert.assertEquals(EMPTY_SCHEMA_PATH.toString(), rpc);
    }

    @Test
    public void testGetLocalRegisteredRoutedRpc() throws Exception {
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();

        Assert.assertNotNull(localRegisteredRoutedRpc);
        Assert.assertEquals(1, localRegisteredRoutedRpc.size());

        final String localRpc = localRegisteredRoutedRpc.iterator().next();
        Assert.assertTrue(localRpc.contains(LOCAL_QNAME.toString()));
        Assert.assertTrue(localRpc.contains(LOCAL_SCHEMA_PATH.toString()));
    }

    @Test
    public void testFindRpcByName() throws Exception {
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertEquals(1, rpcByName.size());
        Assert.assertTrue(rpcByName.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testFindRpcByRoute() throws Exception {
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertEquals(1, rpcByRoute.size());
        Assert.assertTrue(rpcByRoute.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testGetBucketVersions() throws Exception {
        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(Collections.EMPTY_MAP.toString(), bucketVersions);
    }
}