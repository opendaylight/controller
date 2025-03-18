/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class RemoteActionRegistryMXBeanImplTest {

    private static final QName LOCAL_QNAME = QName.create("base", "local");
    private static final QName REMOTE_QNAME = QName.create("base", "local");
    private static final Absolute LOCAL_SCHEMA_PATH = Absolute.of(LOCAL_QNAME);
    private static final Absolute REMOTE_SCHEMA_PATH = Absolute.of(REMOTE_QNAME);

    private ActorSystem system;
    private TestActorRef<ActionRegistry> testActor;
    private List<DOMActionInstance> buckets;
    private RemoteActionRegistryMXBeanImpl mxBean;

    @Before
    public void setUp() {
        system = ActorSystem.create("test", ConfigFactory.load().getConfig("unit-test"));

        final DOMActionInstance emptyActionIdentifier = DOMActionInstance.of(
                REMOTE_SCHEMA_PATH, LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of());
        final DOMActionInstance localActionIdentifier = DOMActionInstance.of(
                LOCAL_SCHEMA_PATH, LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(LOCAL_QNAME));

        buckets = List.of(emptyActionIdentifier, localActionIdentifier);

        final RemoteOpsProviderConfig config = new RemoteOpsProviderConfig.Builder("system").build();
        final TestKit invoker = new TestKit(system);
        final TestKit registrar = new TestKit(system);
        final TestKit supervisor = new TestKit(system);
        final Props props = ActionRegistry.props(config, invoker.getRef(), registrar.getRef())
                .withDispatcher(Dispatchers.DefaultDispatcherId());
        testActor = new TestActorRef<>(system, props, supervisor.getRef(), "testActor");

        final var timeout = Duration.ofSeconds(10);
        mxBean = new RemoteActionRegistryMXBeanImpl(new BucketStoreAccess(testActor, system.dispatcher(), timeout),
                timeout);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
    }

    @Test
    public void testGetLocalRegisteredRoutedActionEmptyBuckets() {
        final Set<String> localRegisteredRoutedAction = mxBean.getLocalRegisteredAction();

        Assert.assertNotNull(localRegisteredRoutedAction);
        Assert.assertTrue(localRegisteredRoutedAction.isEmpty());
    }

    @Test
    public void testGetLocalRegisteredRoutedAction() {
        testActor.tell(new ActionRegistry.UpdateActions(buckets, List.of()), ActorRef.noSender());
        final Set<String> localRegisteredRoutedAction = mxBean.getLocalRegisteredAction();

        Assert.assertNotNull(localRegisteredRoutedAction);
        Assert.assertEquals(1, localRegisteredRoutedAction.size());

        final String localAction = localRegisteredRoutedAction.iterator().next();
        Assert.assertTrue(localAction.contains(LOCAL_QNAME.toString()));
        Assert.assertTrue(localAction.contains(LOCAL_SCHEMA_PATH.toString()));
    }

    @Test
    public void testFindActionByNameEmptyBuckets() {
        final Map<String, String> rpcByName = mxBean.findActionByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertTrue(rpcByName.isEmpty());
    }

    @Test
    public void testFindActionByName() {
        testActor.tell(new ActionRegistry.UpdateActions(buckets, List.of()), ActorRef.noSender());
        final Map<String, String> rpcByName = mxBean.findActionByName("");

        Assert.assertNotNull(rpcByName);
        Assert.assertEquals(1, rpcByName.size());
        Assert.assertTrue(rpcByName.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testFindActionByRouteEmptyBuckets() {
        final Map<String, String> rpcByRoute = mxBean.findActionByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertTrue(rpcByRoute.isEmpty());
    }

    @Test
    public void testFindActionByRoute() {
        testActor.tell(new ActionRegistry.UpdateActions(buckets, List.of()), ActorRef.noSender());
        final Map<String, String> rpcByRoute = mxBean.findActionByRoute("");

        Assert.assertNotNull(rpcByRoute);
        Assert.assertEquals(1, rpcByRoute.size());
        Assert.assertTrue(rpcByRoute.containsValue(LOCAL_QNAME.getLocalName()));
    }

    @Test
    public void testGetBucketVersionsEmptyBuckets() {
        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(Collections.emptyMap().toString(), bucketVersions);
    }

    @Test
    public void testGetBucketVersions() {
        testActor.tell(new ActionRegistry.UpdateActions(buckets, List.of()), ActorRef.noSender());
        final String bucketVersions = mxBean.getBucketVersions();

        Assert.assertTrue(bucketVersions.contains(testActor.provider().getDefaultAddress().toString()));
    }
}
