/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestGetOperationTest extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;
    private static CompositeNode answerFromGet;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        answerFromGet = prepareCompositeNodeWithIetfInterfacesInterfacesData();
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
//         enable(TestProperties.LOG_TRAFFIC);
//         enable(TestProperties.DUMP_ENTITY);
//         enable(TestProperties.RECORD_LOG_LEVEL);
//         set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    /**
     * Tests of status codes for "/operational/{identifier}".
     */
    @Test
    public void getOperationalStatusCodes() throws UnsupportedEncodingException {
        mockReadOperationalDataMethod();
        String uri = createUri("/operational/", "ietf-interfaces:interfaces/interface/eth0");
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = createUri("/operational/", "wrong-module:interfaces/interface/eth0");
        assertEquals(400, get(uri, MediaType.APPLICATION_XML));
    }

    /**
     * Tests of status codes for "/config/{identifier}".
     */
    @Test
    public void getConfigStatusCodes() throws UnsupportedEncodingException {
        mockReadConfigurationDataMethod();
        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/eth0");
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = createUri("/config/", "wrong-module:interfaces/interface/eth0");
        assertEquals(400, get(uri, MediaType.APPLICATION_XML));
    }

    /**
     * MountPoint test. URI represents mount point.
     */
    @Test
    public void getDataWithUrlMountPoint() throws UnsupportedEncodingException, URISyntaxException {
        when(
                brokerFacade.readConfigurationDataBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class))).thenReturn(prepareCnDataForMountPointTest());
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);
        
        String uri = createUri("/config/",
                "ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont/cont1");
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = createUri("/config/", "ietf-interfaces:interfaces/yang-ext:mount/test-module:cont/cont1");
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    @Test
    public void getDataMountPointIntoHighestElement() throws UnsupportedEncodingException, URISyntaxException {
        when(brokerFacade.readConfigurationDataBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class))).thenReturn(prepareCnDataForMountPointTest());
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/",
                "ietf-interfaces:interfaces/interface/0/yang-ext:mount/");
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    private int get(String uri, String mediaType) {
        return target(uri).request(mediaType).get().getStatus();
    }

    private CompositeNode prepareCnDataForMountPointTest() throws URISyntaxException {
        CompositeNodeWrapper cont1 = new CompositeNodeWrapper(new URI("test:module"), "cont1");
        SimpleNodeWrapper lf11 = new SimpleNodeWrapper(new URI("test:module"), "lf11", "lf11 value");
        cont1.addValue(lf11);
        return cont1.unwrap();
    }

    private void mockReadOperationalDataMethod() {
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(answerFromGet);
    }

    private void mockReadConfigurationDataMethod() {
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(answerFromGet);
    }

    private static CompositeNode prepareCompositeNodeWithIetfInterfacesInterfacesData() {
        CompositeNode intface;
        try {
            intface = new CompositeNodeWrapper(new URI("interface"), "interface");
            List<Node<?>> childs = new ArrayList<>();

            childs.add(new SimpleNodeWrapper(new URI("name"), "name", "eth0"));
            childs.add(new SimpleNodeWrapper(new URI("type"), "type", "ethernetCsmacd"));
            childs.add(new SimpleNodeWrapper(new URI("enabled"), "enabled", Boolean.FALSE));
            childs.add(new SimpleNodeWrapper(new URI("description"), "description", "some interface"));
            intface.setValue(childs);
            return intface;
        } catch (URISyntaxException e) {
        }

        return null;
    }

}
