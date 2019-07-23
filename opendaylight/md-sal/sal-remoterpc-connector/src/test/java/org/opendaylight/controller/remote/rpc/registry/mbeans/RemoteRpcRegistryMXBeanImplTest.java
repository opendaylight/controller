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
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RemoteRpcRegistryMXBeanImplTest {

    private static final QName LOCAL_QNAME = QName.create("base", "local");
    private static final SchemaPath EMPTY_SCHEMA_PATH = SchemaPath.ROOT;
    private static final SchemaPath LOCAL_SCHEMA_PATH = SchemaPath.create(true, LOCAL_QNAME);

    private ActorSystem system;
    private TestActorRef<RpcRegistry> testActor;
    private List<DOMRpcIdentifier> buckets;
    private RemoteRpcRegistryMXBeanImpl mxBean;

    @Before
    public void setUp() {
        system = ActorSystem.create("test");

        final DOMRpcIdentifier emptyRpcIdentifier = DOMRpcIdentifier.create(
                EMPTY_SCHEMA_PATH, YangInstanceIdentifier.EMPTY);
        final DOMRpcIdentifier localRpcIdentifier = DOMRpcIdentifier.create(
                LOCAL_SCHEMA_PATH, YangInstanceIdentifier.of(LOCAL_QNAME));

        buckets = Lists.newArrayList(emptyRpcIdentifier, localRpcIdentifier);

        final RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("system").build();
        final TestKit invoker = new TestKit(system);
        final TestKit registrar = new TestKit(system);
        final TestKit supervisor = new TestKit(system);
        final Props props = RpcRegistry.props(config, invoker.getRef(), registrar.getRef())
                .withDispatcher(Dispatchers.DefaultDispatcherId());
        testActor = new TestActorRef<>(system, props, supervisor.getRef(), "testActor");

        final Timeout timeout = Timeout.apply(10, TimeUnit.SECONDS);
        mxBean = new RemoteRpcRegistryMXBeanImpl(new BucketStoreAccess(testActor, system.dispatcher(), timeout),
                timeout);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, Boolean.TRUE);
    }

    @Test
    public void testGetGlobalRpcEmptyBuckets() {
        final Set<String> globalRpc = mxBean.getGlobalRpc();

        Assert.assertNotNull(globalRpc);
        Assert.assertTrue(globalRpc.isEmpty());
    }

    @Test
    public void testGetGlobalRpc() {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final Set<String> globalRpc = mxBean.getGlobalRpc();

        Assert.assertNotNull(globalRpc);
        Assert.assertEquals(1, globalRpc.size());

        final String rpc = globalRpc.iterator().next();
        Assert.assertEquals(EMPTY_SCHEMA_PATH.toString(), rpc);
    }

    @Test
    public void testGetLocalRegisteredRoutedRpcEmptyBuckets() {
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();

        Assert.assertNotNull(localRegisteredRoutedRpc);
        Assert.assertTrue(localRegisteredRoutedRpc.isEmpty());
    }

    @Test
    public void testGetLocalRegisteredRoutedRpc() {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();

        Assert.assertNotNull(localRegisteredRoutedRpc);
        Assert.assertEquals(1, localRegisteredRoutedRpc.size());

        final String localRpc = localRegisteredRoutedRpc.iterator().next();
        Assert.assertTrue(localRpc.contains(LOCAL_QNAME.toString()));
        Assert.assertTrue(localRpc.contains(LOCAL_SCHEMA_PATH.toString()));
    }

    @Test
    public void testFindRpcByNameEmptyBuckets() {
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertTrue(rpcByName.isEmpty());
    }

    @Test
    public void testFindRpcByName() {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertEquals(1, rpcByName.size());
        Assert.assertTrue(rpcByName.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testFindRpcByRouteEmptyBuckets() {
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertTrue(rpcByRoute.isEmpty());
    }

    @Test
    public void testFindRpcByRoute() {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertEquals(1, rpcByRoute.size());
        Assert.assertTrue(rpcByRoute.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testGetBucketVersionsEmptyBuckets() {
        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(Collections.EMPTY_MAP.toString(), bucketVersions);
    }

    @Test
    public void testGetBucketVersions() {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(Lists.newArrayList(buckets)), ActorRef.noSender());
        final String bucketVersions = mxBean.getBucketVersions();

        Assert.assertTrue(bucketVersions.contains(testActor.provider().getDefaultAddress().toString()));
    }
}
