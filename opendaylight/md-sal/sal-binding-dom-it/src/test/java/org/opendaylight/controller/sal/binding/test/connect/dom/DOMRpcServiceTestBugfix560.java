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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.OpendaylightTestRpcServiceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.RockTheHouseInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

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

    private static final String TLL_NAME = "id";
    private static final QName TLL_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");

    private static final InstanceIdentifier<TopLevelList> BA_MOUNT_ID = createBATllIdentifier(TLL_NAME);
    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_MOUNT_ID = createBITllIdentifier(TLL_NAME);

    private BindingTestContext testContext;
    private DOMMountPointService domMountPointService;
    private MountPointService bindingMountPointService;
    private SchemaContext schemaContext;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        final BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.newDirectExecutorService());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        domMountPointService = testContext.getDomMountProviderService();
        bindingMountPointService = testContext.getBindingMountPointService();
        assertNotNull(domMountPointService);

        final InputStream moduleStream = BindingReflections.getModuleInfo(
                OpendaylightTestRpcServiceService.class)
                .getModuleSourceStream();

        assertNotNull(moduleStream);
        final List<InputStream> rpcModels = Collections.singletonList(moduleStream);
        schemaContext = YangParserTestUtils.parseYangStreams(rpcModels);
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBITllIdentifier(
            final String mount) {
        return org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
                .builder().node(Top.QNAME)
                .node(TopLevelList.QNAME)
                .nodeWithKey(TopLevelList.QNAME, TLL_NAME_QNAME, mount)
                .build();
    }

    private static InstanceIdentifier<TopLevelList> createBATllIdentifier(
            final String mount) {
        return InstanceIdentifier.builder(Top.class)
                .child(TopLevelList.class, new TopLevelListKey(mount)).build();
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        // FIXME: This is made to only make sure instance identifier codec for path is instantiated.
        domMountPointService
                .createMountPoint(BI_MOUNT_ID).addService(DOMRpcService.class, new DOMRpcService() {

                    @Override
                    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(final T arg0) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath arg0, final NormalizedNode<?, ?> arg1) {
                        final DOMRpcResult result = new DefaultDOMRpcResult((NormalizedNode<?, ?>) null);
                        return Futures.immediateCheckedFuture(result);
                    }
                }).register();

        final Optional<MountPoint> mountInstance = bindingMountPointService.getMountPoint(BA_MOUNT_ID);
        assertTrue(mountInstance.isPresent());

        final Optional<RpcConsumerRegistry> rpcRegistry = mountInstance.get().getService(RpcConsumerRegistry.class);
        assertTrue(rpcRegistry.isPresent());
        final OpendaylightTestRpcServiceService rpcService = rpcRegistry.get()
                .getRpcService(OpendaylightTestRpcServiceService.class);
        assertNotNull(rpcService);

        try {
            final Future<RpcResult<Void>> result = rpcService
                    .rockTheHouse(new RockTheHouseInputBuilder().build());
            assertTrue(result.get().isSuccessful());
        } catch (final IllegalStateException ex) {
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
