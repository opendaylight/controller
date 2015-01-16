/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.OpendaylightTestRpcServiceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.RockTheHouseInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Test case for reported bug 560
 *
 * @author Lukas Sedlak
 * @see <a
 *      href="https://bugs.opendaylight.org/show_bug.cgi?id=560">https://bugs.opendaylight.org/show_bug.cgi?id=560</a>
 */
public class DOMRpcServiceTestBugfix560 {

    private final static String RPC_SERVICE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:test:bi:ba:rpcservice";
    private final static String REVISION_DATE = "2014-07-01";
    private final static QName RPC_NAME = QName.create(RPC_SERVICE_NAMESPACE,
            REVISION_DATE, "rock-the-house");

    private static final NodeId MOUNT_NODE = new NodeId("id");
    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");

    private static final InstanceIdentifier<Node> BA_MOUNT_ID = createBANodeIdentifier(MOUNT_NODE);
    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_MOUNT_ID = createBINodeIdentifier(MOUNT_NODE);

    private BindingTestContext testContext;
    private MountProvisionService domMountPointService;
    private MountProviderService bindingMountPointService;
    private SchemaContext schemaContext;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.sameThreadExecutor());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        domMountPointService = testContext.getDomMountProviderService();
        bindingMountPointService = testContext.getBindingMountProviderService();
        assertNotNull(domMountPointService);

        final YangContextParser parser = new YangParserImpl();
        final InputStream moduleStream = BindingReflections.getModuleInfo(
                OpendaylightTestRpcServiceService.class)
                .getModuleSourceStream();

        assertNotNull(moduleStream);
        List<InputStream> rpcModels = Collections.singletonList(moduleStream);
        @SuppressWarnings("deprecation")
        Set<Module> modules = parser.parseYangModelsFromStreams(rpcModels);
        @SuppressWarnings("deprecation")
        SchemaContext mountSchemaContext = parser.resolveSchemaContext(modules);
        schemaContext = mountSchemaContext;
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBINodeIdentifier(
            final NodeId mountNode) {
        return org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
                .builder().node(Nodes.QNAME)
                .nodeWithKey(Node.QNAME, NODE_ID_QNAME, mountNode.getValue())
                .build();
    }

    private static InstanceIdentifier<Node> createBANodeIdentifier(
            final NodeId mountNode) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(mountNode)).build();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test() throws ExecutionException, InterruptedException {
        // FIXME: This is made to only make sure instance identifier codec
        // for path is instantiated.
        testContext.getBindingDataBroker().readOperationalData(BA_MOUNT_ID);
        final MountProvisionInstance mountPoint = domMountPointService
                .createMountPoint(BI_MOUNT_ID);
        mountPoint.setSchemaContext(schemaContext);
        assertNotNull(mountPoint);

        mountPoint.addRpcImplementation(RPC_NAME, new RpcImplementation() {

            @Override
            public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(
                    final QName rpc, final CompositeNode input) {

                return Futures.immediateFuture(RpcResultBuilder
                        .<CompositeNode> success().build());
            }

            @Override
            public Set<QName> getSupportedRpcs() {
                return ImmutableSet.of(RPC_NAME);
            }
        });

        final Set<QName> biSupportedRpcs = mountPoint.getSupportedRpcs();
        assertNotNull(biSupportedRpcs);
        assertTrue(!biSupportedRpcs.isEmpty());

        MountProviderInstance mountInstance = bindingMountPointService
                .getMountPoint(BA_MOUNT_ID);
        assertNotNull(mountInstance);
        final OpendaylightTestRpcServiceService rpcService = mountInstance
                .getRpcService(OpendaylightTestRpcServiceService.class);
        assertNotNull(rpcService);

        try {
            Future<RpcResult<Void>> result = rpcService
                    .rockTheHouse(new RockTheHouseInputBuilder().build());
            assertTrue(result.get().isSuccessful());
        } catch (IllegalStateException ex) {
            fail("OpendaylightTestRpcServiceService class doesn't contain rockTheHouse method!");
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void teardown() throws Exception {
        testContext.close();
    }
}
