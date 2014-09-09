/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import java.io.FileNotFoundException;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestconfImplTest {

    private static final RestconfImpl restconfImpl = RestconfImpl.getInstance();
    private static final BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);
    private static SchemaContext ietfInterfacesSchemaContext = null;
    private static SchemaContext testmoduleSchemaContext = null;

    static {
        try {
            ietfInterfacesSchemaContext = TestUtils.loadSchemaContext("/modules");
            testmoduleSchemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        } catch (FileNotFoundException e) {
            fail("Impossible to load schema context");
        }
    }

    @BeforeClass
    public static void beforeClass() {
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(ietfInterfacesSchemaContext);

        DOMMountPoint mockedMountInstance = mock(DOMMountPoint.class);
        when(mockedMountInstance.getSchemaContext()).thenReturn(testmoduleSchemaContext);

        DOMMountPointService mockedMountService = mock(DOMMountPointService.class);

        when(mockedMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(
                Optional.of(mockedMountInstance));

        controllerContext.setMountService(mockedMountService);
        restconfImpl.setControllerContext(controllerContext);
        restconfImpl.setBroker(mockedBrokerFacade);
    }

    /**
     *
     * Tests whether branch for not mount point is called in findModule method
     *
     * findModule method is private therefore is tested indirectly via createConfigurationData method
     */
    @Test
    public void findModuleTest() {
        when(mockedBrokerFacade.commitConfigurationDataPost(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(null);

        Response response = restconfImpl.createConfigurationData("/ietf-interfaces:interfaces",
                preparePayloadInterfaceWithName());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    /**
     * Tests whether branch for mount point is called in findModule method
     *
     * findModule method is private therefore is tested indirectly via createConfigurationData method
     *
     */
    @Test
    public void findModuleBehindMountPointTest() {
        when(
                mockedBrokerFacade.commitConfigurationDataPost(any(DOMMountPoint.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(null);
        Response response = restconfImpl.createConfigurationData("/ietf-interfaces:interfaces/"
                + ControllerContext.MOUNT + "/test-module:cont", preparePayloadCont1WithLf11());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    /**
     * <interface xmlns="urn:ietf:params:xml:ns:yang:ietf-interfaces"> <name>key name</name> </interface>
     */
    private CompositeNode preparePayloadInterfaceWithName() {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(TestUtils.buildQName("interface", "urn:ietf:params:xml:ns:yang:ietf-interfaces", "2013-07-04"));
        SimpleNode<String> name = NodeFactory.createImmutableSimpleNode(
                TestUtils.buildQName("name", "urn:ietf:params:xml:ns:yang:ietf-interfaces", "2013-07-04"), null,
                "key name");
        builder.add(name);
        return builder.toInstance();
    }

    /**
     * <cont1 xmlns="test:module"> <lf11>lf11 value</lf11> </cont1>
     */
    private CompositeNode preparePayloadCont1WithLf11() {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(TestUtils.buildQName("cont1", "test:module", "2014-01-09"));
        SimpleNode<String> name = NodeFactory.createImmutableSimpleNode(
                TestUtils.buildQName("lf11", "test:module", "2014-01-09"), null, "lf11 value");
        builder.add(name);
        return builder.toInstance();
    }

}
