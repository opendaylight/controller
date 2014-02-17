/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    private static SchemaContext schemaContextModules;
    private static SchemaContext schemaContextBehindMountPoint;

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

        schemaContextModules = TestUtils.loadSchemaContext("/modules");
        schemaContextBehindMountPoint = TestUtils.loadSchemaContext("/modules/modules-behind-mount-point");
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
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
        String uri = "/operational/ietf-interfaces:interfaces/interface/eth0";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = "/operational/wrong-module:interfaces/interface/eth0";
        assertEquals(400, get(uri, MediaType.APPLICATION_XML));
    }

    /**
     * Tests of status codes for "/config/{identifier}".
     */
    @Test
    public void getConfigStatusCodes() throws UnsupportedEncodingException {
        mockReadConfigurationDataMethod();
        String uri = "/config/ietf-interfaces:interfaces/interface/eth0";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = "/config/wrong-module:interfaces/interface/eth0";
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

        String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont/cont1";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = "/config/ietf-interfaces:interfaces/yang-ext:mount/test-module:cont/cont1";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    @Test
    public void getDataMountPointIntoHighestElement() throws UnsupportedEncodingException, URISyntaxException {
        when(
                brokerFacade.readConfigurationDataBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class))).thenReturn(prepareCnDataForMountPointTest());
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    // /modules
    @Test
    public void getModulesTest() throws UnsupportedEncodingException, FileNotFoundException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/modules";

        Response response = target(uri).request("application/yang.api+json").get();
        validateModulesResponseJson(response);

        response = target(uri).request("application/yang.api+xml").get();
        validateModulesResponseXml(response);
    }

    // /modules/module
    @Test
    public void getModuleTest() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/modules/module/module2/2014-01-02";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Module2 in xml wasn't found", prepareXmlRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        String[] split = responseBody.split("<module");
        assertEquals("<module element is returned more then once",2,split.length);

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        responseBody = response.readEntity(String.class);
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        split = responseBody.split("\"module\"");
        assertEquals("\"module\" element is returned more then once",2,split.length);

    }

    // /operations
    @Test
    public void getOperationsTest() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/operations";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Xml response for /operations dummy-rpc1-module1 is incorrect",
                validateOperationsResponseXml(responseBody, "dummy-rpc1-module1", "module:1").find());
        assertTrue("Xml response for /operations dummy-rpc2-module1 is incorrect",
                validateOperationsResponseXml(responseBody, "dummy-rpc2-module1", "module:1").find());
        assertTrue("Xml response for /operations dummy-rpc1-module2 is incorrect",
                validateOperationsResponseXml(responseBody, "dummy-rpc1-module2", "module:2").find());
        assertTrue("Xml response for /operations dummy-rpc2-module2 is incorrect",
                validateOperationsResponseXml(responseBody, "dummy-rpc2-module2", "module:2").find());

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        responseBody = response.readEntity(String.class);
        assertTrue("Json response for /operations dummy-rpc1-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc1-module1", "module1").find());
        assertTrue("Json response for /operations dummy-rpc2-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc2-module1", "module1").find());
        assertTrue("Json response for /operations dummy-rpc1-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc1-module2", "module2").find());
        assertTrue("Json response for /operations dummy-rpc2-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc2-module2", "module2").find());

    }

    // /operations/pathToMountPoint/yang-ext:mount
    @Test
    public void getOperationsBehindMountPointTest() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setGlobalSchema(schemaContextModules);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        controllerContext.setMountService(mockMountService);

        String uri = "/operations/ietf-interfaces:interfaces/interface/0/yang-ext:mount/";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Xml response for /operations/mount_point rpc-behind-module1 is incorrect",
                validateOperationsResponseXml(responseBody, "rpc-behind-module1", "module:1:behind:mount:point").find());
        assertTrue("Xml response for /operations/mount_point rpc-behind-module2 is incorrect",
                validateOperationsResponseXml(responseBody, "rpc-behind-module2", "module:2:behind:mount:point").find());

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        responseBody = response.readEntity(String.class);
        assertTrue("Json response for /operations/mount_point rpc-behind-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module1", "module1-behind-mount-point").find());
        assertTrue("Json response for /operations/mount_point rpc-behind-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module2", "module2-behind-mount-point").find());

    }

    private Matcher validateOperationsResponseJson(String searchIn, String rpcName, String moduleName) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");

        regex.append(".*\\{");
        regex.append(".*\"");

        // operations prefix optional
        regex.append("(");
        regex.append("ietf-restconf:");
        regex.append("|)");
        // :operations prefix optional

        regex.append("operations\"");
        regex.append(".*:");
        regex.append(".*\\{");

        regex.append(".*\"" + moduleName);
        regex.append(":");
        regex.append(rpcName + "\"");
        regex.append(".*\\[");
        regex.append(".*null");
        regex.append(".*\\]");

        regex.append(".*\\}");
        regex.append(".*\\}");

        regex.append(".*");
        regex.append("$");
        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);

    }

    private Matcher validateOperationsResponseXml(String searchIn, String rpcName, String namespace) {
        StringBuilder regex = new StringBuilder();

        regex.append("^");

        regex.append(".*<operations");
        regex.append(".*xmlns=\"urn:ietf:params:xml:ns:yang:ietf-restconf\"");
        regex.append(".*>");

        regex.append(".*<");
        regex.append(".*" + rpcName);
        regex.append(".*" + namespace);
        regex.append(".*/");
        regex.append(".*>");

        regex.append(".*</operations.*");
        regex.append(".*>");

        regex.append(".*");
        regex.append("$");
        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);
    }

    // /restconf/modules/pathToMountPoint/yang-ext:mount
    @Test
    public void getModulesBehindMountPoint() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setGlobalSchema(schemaContextModules);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        controllerContext.setMountService(mockMountService);

        String uri = "/modules/ietf-interfaces:interfaces/interface/0/yang-ext:mount/";

        Response response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);

        assertTrue(
                "module1-behind-mount-point in json wasn't found",
                prepareJsonRegex("module1-behind-mount-point", "2014-02-03", "module:1:behind:mount:point",
                        responseBody).find());
        assertTrue(
                "module2-behind-mount-point in json wasn't found",
                prepareJsonRegex("module2-behind-mount-point", "2014-02-04", "module:2:behind:mount:point",
                        responseBody).find());

        response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        responseBody = response.readEntity(String.class);
        assertTrue(
                "module1-behind-mount-point in json wasn't found",
                prepareXmlRegex("module1-behind-mount-point", "2014-02-03", "module:1:behind:mount:point", responseBody)
                        .find());
        assertTrue(
                "module2-behind-mount-point in json wasn't found",
                prepareXmlRegex("module2-behind-mount-point", "2014-02-04", "module:2:behind:mount:point", responseBody)
                        .find());

    }

    // /restconf/modules/module/pathToMountPoint/yang-ext:mount/moduleName/revision
    @Test
    public void getModuleBehindMountPoint() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setGlobalSchema(schemaContextModules);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        controllerContext.setMountService(mockMountService);

        String uri = "/modules/module/ietf-interfaces:interfaces/interface/0/yang-ext:mount/module1-behind-mount-point/2014-02-03";

        Response response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);

        assertTrue(
                "module1-behind-mount-point in json wasn't found",
                prepareJsonRegex("module1-behind-mount-point", "2014-02-03", "module:1:behind:mount:point",
                        responseBody).find());
        String[] split = responseBody.split("\"module\"");
        assertEquals("\"module\" element is returned more then once",2,split.length);


        response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        responseBody = response.readEntity(String.class);
        assertTrue(
                "module1-behind-mount-point in json wasn't found",
                prepareXmlRegex("module1-behind-mount-point", "2014-02-03", "module:1:behind:mount:point", responseBody)
                        .find());
        split = responseBody.split("<module");
        assertEquals("<module element is returned more then once",2,split.length);




    }

    private void validateModulesResponseXml(Response response) {
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);

        assertTrue("Module1 in xml wasn't found", prepareXmlRegex("module1", "2014-01-01", "module:1", responseBody)
                .find());
        assertTrue("Module2 in xml wasn't found", prepareXmlRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        assertTrue("Module3 in xml wasn't found", prepareXmlRegex("module3", "2014-01-03", "module:3", responseBody)
                .find());
    }

    private void validateModulesResponseJson(Response response) {
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);

        assertTrue("Module1 in json wasn't found", prepareJsonRegex("module1", "2014-01-01", "module:1", responseBody)
                .find());
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        assertTrue("Module3 in json wasn't found", prepareJsonRegex("module3", "2014-01-03", "module:3", responseBody)
                .find());
    }

    private Matcher prepareJsonRegex(String module, String revision, String namespace, String searchIn) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");

        regex.append(".*\\{");
        regex.append(".*\"name\"");
        regex.append(".*:");
        regex.append(".*\"" + module + "\",");

        regex.append(".*\"revision\"");
        regex.append(".*:");
        regex.append(".*\"" + revision + "\",");

        regex.append(".*\"namespace\"");
        regex.append(".*:");
        regex.append(".*\"" + namespace + "\"");

        regex.append(".*\\}");

        regex.append(".*");
        regex.append("$");
        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);

    }

    private Matcher prepareXmlRegex(String module, String revision, String namespace, String searchIn) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");

        regex.append(".*<module.*");
        regex.append(".*>");

        regex.append(".*<name>");
        regex.append(".*" + module);
        regex.append(".*<\\/name>");

        regex.append(".*<revision>");
        regex.append(".*" + revision);
        regex.append(".*<\\/revision>");

        regex.append(".*<namespace>");
        regex.append(".*" + namespace);
        regex.append(".*<\\/namespace>");

        regex.append(".*<\\/module.*>");

        regex.append(".*");
        regex.append("$");

        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);
    }

    private void prepareMockForModulesTest(ControllerContext mockedControllerContext) throws FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/modules");
        mockedControllerContext.setGlobalSchema(schemaContext);
        // when(mockedControllerContext.getGlobalSchema()).thenReturn(schemaContext);
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
