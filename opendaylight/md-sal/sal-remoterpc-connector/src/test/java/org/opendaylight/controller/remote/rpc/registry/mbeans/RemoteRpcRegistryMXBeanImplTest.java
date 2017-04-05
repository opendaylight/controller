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
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RemoteRpcRegistryMXBeanImplTest {

    private static final QName LOCAL_QNAME = QName.create("base", "local");
    private static final SchemaPath EMPTY_SCHEMA_PATH = SchemaPath.ROOT;
    private static final SchemaPath LOCAL_SCHEMA_PATH = SchemaPath.create(true, LOCAL_QNAME);

    private static ActorSystem system;
    private static TestActorRef<?> testActor;
    private static List<DOMRpcIdentifier> buckets;
    private static RemoteRpcRegistryMXBeanImpl mxBean;

    @BeforeClass
    public static void setUp() throws Exception {
        system = ActorSystem.create("test");

        final DOMRpcIdentifier localRpcIdentifier = DOMRpcIdentifier.create(
                LOCAL_SCHEMA_PATH, YangInstanceIdentifier.of(LOCAL_QNAME));

        final DOMRpcIdentifier emptyRpcIdentifier = DOMRpcIdentifier.create(
                EMPTY_SCHEMA_PATH, YangInstanceIdentifier.EMPTY);

        buckets = Lists.newArrayList(emptyRpcIdentifier);

        final RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("system").build();
        final JavaTestKit testKit = new JavaTestKit(system);
        final Props props = RpcRegistry.props(
                config, testKit.getRef(), testKit.getRef(), localRpcIdentifier, emptyRpcIdentifier);
        testActor = new TestActorRef<>(system, props, testKit.getRef(), "testActor");
        final RpcRegistry rpcRegistry = (RpcRegistry) testActor.underlyingActor();

        mxBean = new RemoteRpcRegistryMXBeanImpl(rpcRegistry);
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
    public void testFindRpcByNameEmptyBuckets() throws Exception {
        final Map<String, String> rpcByName = mxBean.findRpcByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertEquals(1, rpcByName.size());
        Assert.assertTrue(rpcByName.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testFindRpcByRouteEmptyBuckets() throws Exception {
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertEquals(1, rpcByRoute.size());
        Assert.assertTrue(rpcByRoute.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testGetBucketVersionsEmptyBuckets() throws Exception {
        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(Collections.EMPTY_MAP.toString(), bucketVersions);
    }

    @Ignore
    @Test
    public void testGetBucketVersions() throws Exception {
        testActor.tell(new RpcRegistry.Messages.AddOrUpdateRoutes(buckets), ActorRef.noSender());

        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(testActor.provider().getDefaultAddress().toString(), bucketVersions);
    }
}