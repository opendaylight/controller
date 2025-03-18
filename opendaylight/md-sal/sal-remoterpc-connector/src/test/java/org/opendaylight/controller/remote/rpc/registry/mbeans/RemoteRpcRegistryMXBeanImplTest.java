/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RemoteRpcRegistryMXBeanImplTest {
    private static final QName LOCAL_QNAME = QName.create("base", "local");
    private static final QName REMOTE_QNAME = QName.create("base", "remote");

    private ActorSystem system;
    private TestActorRef<RpcRegistry> testActor;
    private List<DOMRpcIdentifier> buckets;
    private RemoteRpcRegistryMXBeanImpl mxBean;

    @Before
    public void setUp() {
        system = ActorSystem.create("test", ConfigFactory.load().getConfig("unit-test"));

        final DOMRpcIdentifier emptyRpcIdentifier = DOMRpcIdentifier.create(
                REMOTE_QNAME, YangInstanceIdentifier.of());
        final DOMRpcIdentifier localRpcIdentifier = DOMRpcIdentifier.create(
                LOCAL_QNAME, YangInstanceIdentifier.of(LOCAL_QNAME));

        buckets = List.of(emptyRpcIdentifier, localRpcIdentifier);

        final RemoteOpsProviderConfig config = new RemoteOpsProviderConfig.Builder("system").build();
        final TestKit invoker = new TestKit(system);
        final TestKit registrar = new TestKit(system);
        final TestKit supervisor = new TestKit(system);
        final Props props = RpcRegistry.props(config, invoker.getRef(), registrar.getRef())
                .withDispatcher(Dispatchers.DefaultDispatcherId());
        testActor = new TestActorRef<>(system, props, supervisor.getRef(), "testActor");

        final var timeout = Duration.ofSeconds(10);
        mxBean = new RemoteRpcRegistryMXBeanImpl(new BucketStoreAccess(testActor, system.dispatcher(), timeout),
                timeout);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
    }

    @Test
    public void testGetGlobalRpcEmptyBuckets() {
        final Set<String> globalRpc = mxBean.getGlobalRpc();

        assertNotNull(globalRpc);
        assertTrue(globalRpc.isEmpty());
    }

    @Test
    public void testGetGlobalRpc() {
        testActor.tell(new RpcRegistry.AddOrUpdateRoutes(buckets), ActorRef.noSender());
        final Set<String> globalRpc = mxBean.getGlobalRpc();

        assertNotNull(globalRpc);
        assertEquals(1, globalRpc.size());

        final String rpc = globalRpc.iterator().next();
        assertEquals(REMOTE_QNAME.toString(), rpc);
    }

    @Test
    public void testGetLocalRegisteredRoutedRpcEmptyBuckets() {
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();

        assertNotNull(localRegisteredRoutedRpc);
        assertTrue(localRegisteredRoutedRpc.isEmpty());
    }

    @Test
    public void testGetLocalRegisteredRoutedRpc() {
        testActor.tell(new RpcRegistry.AddOrUpdateRoutes(buckets), ActorRef.noSender());
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();

        assertNotNull(localRegisteredRoutedRpc);
        assertEquals(1, localRegisteredRoutedRpc.size());

        final String localRpc = localRegisteredRoutedRpc.iterator().next();
        assertThat(localRpc, containsString(LOCAL_QNAME.toString()));
    }

    @Test
    public void testFindRpcByNameEmptyBuckets() {
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        assertNotNull(rpcByName);
        assertTrue(rpcByName.isEmpty());
    }

    @Test
    public void testFindRpcByName() {
        testActor.tell(new RpcRegistry.AddOrUpdateRoutes(buckets), ActorRef.noSender());
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        assertNotNull(rpcByName);
        assertEquals(1, rpcByName.size());
        assertTrue(rpcByName.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testFindRpcByRouteEmptyBuckets() {
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        assertNotNull(rpcByRoute);
        assertTrue(rpcByRoute.isEmpty());
    }

    @Test
    public void testFindRpcByRoute() {
        testActor.tell(new RpcRegistry.AddOrUpdateRoutes(buckets), ActorRef.noSender());
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        assertNotNull(rpcByRoute);
        assertEquals(1, rpcByRoute.size());
        assertTrue(rpcByRoute.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testGetBucketVersionsEmptyBuckets() {
        final String bucketVersions = mxBean.getBucketVersions();
        assertEquals(Collections.emptyMap().toString(), bucketVersions);
    }

    @Test
    public void testGetBucketVersions() {
        testActor.tell(new RpcRegistry.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final String bucketVersions = mxBean.getBucketVersions();

        assertTrue(bucketVersions.contains(testActor.provider().getDefaultAddress().toString()));
    }
}
