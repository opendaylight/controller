/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RestGetOperationTest extends JerseyTest {

    static class NodeData {
        Object key;
        Object data; // List for a CompositeNode, value Object for a SimpleNode

        NodeData(final Object key, final Object data) {
            this.key = key;
            this.data = data;
        }
    }

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;
    private static CompositeNode answerFromGet;

    private static SchemaContext schemaContextModules;
    private static SchemaContext schemaContextBehindMountPoint;

    private static final String RESTCONF_NS = "urn:ietf:params:xml:ns:yang:ietf-restconf";

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
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
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

    /**
     * MountPoint test. URI represents mount point.
     *
     * Slashes in URI behind mount point. lst1 element with key GigabitEthernet0%2F0%2F0%2F0 (GigabitEthernet0/0/0/0) is
     * requested via GET HTTP operation. It is tested whether %2F character is replaced with simple / in
     * InstanceIdentifier parameter in method
     * {@link BrokerFacade#readConfigurationDataBehindMountPoint(MountInstance, InstanceIdentifier)} which is called in
     * method {@link RestconfImpl#readConfigurationData}
     *
     *
     * @throws ParseException
     */
    @Test
    public void getDataWithSlashesBehindMountPoint() throws UnsupportedEncodingException, URISyntaxException,
            ParseException {
        InstanceIdentifier awaitedInstanceIdentifier = prepareInstanceIdentifierForList();
        when(
                brokerFacade.readConfigurationDataBehindMountPoint(any(MountInstance.class),
                        eq(awaitedInstanceIdentifier))).thenReturn(prepareCnDataForMountPointTest());
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont/lst1/GigabitEthernet0%2F0%2F0%2F0";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    private InstanceIdentifier prepareInstanceIdentifierForList() throws URISyntaxException, ParseException {
        List<PathArgument> parameters = new ArrayList<>();

        Date revision = new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-09");
        URI uri = new URI("test:module");
        QName qNameCont = QName.create(uri, revision, "cont");
        QName qNameList = QName.create(uri, revision, "lst1");
        QName qNameKeyList = QName.create(uri, revision, "lf11");

        parameters.add(new InstanceIdentifier.NodeIdentifier(qNameCont));
        parameters.add(new InstanceIdentifier.NodeIdentifierWithPredicates(qNameList, qNameKeyList,
                "GigabitEthernet0/0/0/0"));
        return InstanceIdentifier.create(parameters);
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
        validateModulesResponseXml(response,schemaContextModules);
    }

    // /streams/
    @Test
    public void getStreamsTest() throws UnsupportedEncodingException, FileNotFoundException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/streams";

        Response response = target(uri).request("application/yang.api+json").get();
        String responseBody = response.readEntity(String.class);
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("streams"));

        response = target(uri).request("application/yang.api+xml").get();
        Document responseXmlBody = response.readEntity(Document.class);
        assertNotNull(responseXmlBody);
        Element rootNode = responseXmlBody.getDocumentElement();

        assertEquals("streams", rootNode.getLocalName());
        assertEquals(RESTCONF_NS, rootNode.getNamespaceURI());
    }

    // /modules/module
    @Test
    public void getModuleTest() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/modules/module/module2/2014-01-02";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        Document responseXml = response.readEntity(Document.class);



        QName qname = assertedModuleXmlToModuleQName(responseXml.getDocumentElement());
        assertNotNull(qname);

        assertEquals("module2", qname.getLocalName());
        assertEquals("module:2", qname.getNamespace().toString());
        assertEquals("2014-01-02", qname.getFormattedRevision());

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        String[] split = responseBody.split("\"module\"");
        assertEquals("\"module\" element is returned more then once", 2, split.length);

    }

    // /operations
    @Test
    public void getOperationsTest() throws FileNotFoundException, UnsupportedEncodingException {
        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        String uri = "/operations";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        Document responseDoc = response.readEntity(Document.class);
        validateOperationsResponseXml(responseDoc, schemaContextModules);

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Json response for /operations dummy-rpc1-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc1-module1", "module1").find());
        assertTrue("Json response for /operations dummy-rpc2-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc2-module1", "module1").find());
        assertTrue("Json response for /operations dummy-rpc1-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc1-module2", "module2").find());
        assertTrue("Json response for /operations dummy-rpc2-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "dummy-rpc2-module2", "module2").find());

    }

    private void validateOperationsResponseXml(final Document responseDoc, final SchemaContext schemaContext) {
        Element operationsElem = responseDoc.getDocumentElement();
        assertEquals(RESTCONF_NS, operationsElem.getNamespaceURI());
        assertEquals("operations", operationsElem.getLocalName());


        HashSet<QName> foundOperations = new HashSet<>();

        NodeList operationsList = operationsElem.getChildNodes();
        for(int i = 0;i < operationsList.getLength();i++) {
            org.w3c.dom.Node operation = operationsList.item(i);

            String namespace = operation.getNamespaceURI();
            String name = operation.getLocalName();
            QName opQName = QName.create(URI.create(namespace), null, name);
            foundOperations.add(opQName);
        }

        for(RpcDefinition schemaOp : schemaContext.getOperations()) {
            assertTrue(foundOperations.contains(schemaOp.getQName().withoutRevision()));
        }

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

        Document responseDoc = response.readEntity(Document.class);
        validateOperationsResponseXml(responseDoc, schemaContextBehindMountPoint);

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue("Json response for /operations/mount_point rpc-behind-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module1", "module1-behind-mount-point").find());
        assertTrue("Json response for /operations/mount_point rpc-behind-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module2", "module2-behind-mount-point").find());

    }

    private Matcher validateOperationsResponseJson(final String searchIn, final String rpcName, final String moduleName) {
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

    private Matcher validateOperationsResponseXml(final String searchIn, final String rpcName, final String namespace) {
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
        validateModulesResponseXml(response, schemaContextBehindMountPoint);

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
        assertEquals("\"module\" element is returned more then once", 2, split.length);

        response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        Document responseXml = response.readEntity(Document.class);

        QName module = assertedModuleXmlToModuleQName(responseXml.getDocumentElement());

        assertEquals("module1-behind-mount-point", module.getLocalName());
        assertEquals("2014-02-03", module.getFormattedRevision());
        assertEquals("module:1:behind:mount:point", module.getNamespace().toString());


    }

    private void validateModulesResponseXml(final Response response, final SchemaContext schemaContext) {
        assertEquals(200, response.getStatus());
        Document responseBody = response.readEntity(Document.class);
        NodeList moduleNodes = responseBody.getDocumentElement().getElementsByTagNameNS(RESTCONF_NS, "module");

        assertTrue(moduleNodes.getLength() > 0);

        HashSet<QName> foundModules = new HashSet<>();

        for(int i=0;i < moduleNodes.getLength();i++) {
            org.w3c.dom.Node module = moduleNodes.item(i);

            QName name = assertedModuleXmlToModuleQName(module);
            foundModules.add(name);
        }

        assertAllModules(foundModules,schemaContext);
    }

    private void assertAllModules(final Set<QName> foundModules, final SchemaContext schemaContext) {
        for(Module module : schemaContext.getModules()) {
            QName current = QName.create(module.getQNameModule(),module.getName());
            assertTrue("Module not found in response.",foundModules.contains(current));
        }

    }

    private QName assertedModuleXmlToModuleQName(final org.w3c.dom.Node module) {
        assertEquals("module", module.getLocalName());
        assertEquals(RESTCONF_NS, module.getNamespaceURI());
        String revision = null;
        String namespace = null;
        String name = null;


        NodeList childNodes = module.getChildNodes();

        for(int i =0;i < childNodes.getLength(); i++) {
            org.w3c.dom.Node child = childNodes.item(i);
            assertEquals(RESTCONF_NS, child.getNamespaceURI());

            switch(child.getLocalName()) {
                case "name":
                    assertNull("Name element appeared multiple times",name);
                    name = child.getTextContent().trim();
                    break;
                case "revision":
                    assertNull("Revision element appeared multiple times",revision);
                    revision = child.getTextContent().trim();
                    break;

                case "namespace":
                    assertNull("Namespace element appeared multiple times",namespace);
                    namespace = child.getTextContent().trim();
                    break;
            }
        }

        assertNotNull("Revision was not part of xml",revision);
        assertNotNull("Module namespace was not part of xml",namespace);
        assertNotNull("Module identiffier was not part of xml",name);


        // TODO Auto-generated method stub

        return QName.create(namespace,revision,name);
    }

    private void validateModulesResponseJson(final Response response) {
        assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);

        assertTrue("Module1 in json wasn't found", prepareJsonRegex("module1", "2014-01-01", "module:1", responseBody)
                .find());
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        assertTrue("Module3 in json wasn't found", prepareJsonRegex("module3", "2014-01-03", "module:3", responseBody)
                .find());
    }

    private Matcher prepareJsonRegex(final String module, final String revision, final String namespace,
            final String searchIn) {
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


    private void prepareMockForModulesTest(final ControllerContext mockedControllerContext)
            throws FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/modules");
        mockedControllerContext.setGlobalSchema(schemaContext);
        // when(mockedControllerContext.getGlobalSchema()).thenReturn(schemaContext);
    }

    private int get(final String uri, final String mediaType) {
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

    /**
     * If includeWhiteChars URI parameter is set to false then no white characters can be included in returned output
     *
     * @throws UnsupportedEncodingException
     */
    @Test
    public void getDataWithUriIncludeWhiteCharsParameterTest() throws UnsupportedEncodingException {
        getDataWithUriIncludeWhiteCharsParameter("config");
        getDataWithUriIncludeWhiteCharsParameter("operational");
    }

    private void getDataWithUriIncludeWhiteCharsParameter(final String target) throws UnsupportedEncodingException {
        mockReadConfigurationDataMethod();
        String uri = "/" + target + "/ietf-interfaces:interfaces/interface/eth0";
        Response response = target(uri).queryParam("prettyPrint", "false").request("application/xml").get();
        String xmlData = response.readEntity(String.class);

        Pattern pattern = Pattern.compile(".*(>\\s+|\\s+<).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlData);
        // XML element can't surrounded with white character (e.g ">    " or
        // "    <")
        assertFalse(matcher.matches());

        response = target(uri).queryParam("prettyPrint", "false").request("application/json").get();
        String jsonData = response.readEntity(String.class);
        pattern = Pattern.compile(".*(\\}\\s+|\\s+\\{|\\]\\s+|\\s+\\[|\\s+:|:\\s+).*", Pattern.DOTALL);
        matcher = pattern.matcher(jsonData);
        // JSON element can't surrounded with white character (e.g "} ", " {",
        // "] ", " [", " :" or ": ")
        assertFalse(matcher.matches());
    }

    @Test
    public void getDataWithUriDepthParameterTest() throws UnsupportedEncodingException {

        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        CompositeNode depth1Cont = toCompositeNode(toCompositeNodeData(
                toNestedQName("depth1-cont"),
                toCompositeNodeData(
                        toNestedQName("depth2-cont1"),
                        toCompositeNodeData(
                                toNestedQName("depth3-cont1"),
                                toCompositeNodeData(toNestedQName("depth4-cont1"),
                                        toSimpleNodeData(toNestedQName("depth5-leaf1"), "depth5-leaf1-value")),
                                toSimpleNodeData(toNestedQName("depth4-leaf1"), "depth4-leaf1-value")),
                        toSimpleNodeData(toNestedQName("depth3-leaf1"), "depth3-leaf1-value")),
                toCompositeNodeData(
                        toNestedQName("depth2-cont2"),
                        toCompositeNodeData(
                                toNestedQName("depth3-cont2"),
                                toCompositeNodeData(toNestedQName("depth4-cont2"),
                                        toSimpleNodeData(toNestedQName("depth5-leaf2"), "depth5-leaf2-value")),
                                toSimpleNodeData(toNestedQName("depth4-leaf2"), "depth4-leaf2-value")),
                        toSimpleNodeData(toNestedQName("depth3-leaf2"), "depth3-leaf2-value")),
                toSimpleNodeData(toNestedQName("depth2-leaf1"), "depth2-leaf1-value")));

        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(depth1Cont);

        // Test config with depth 1

        Response response = target("/config/nested-module:depth1-cont").queryParam("depth", "1")
                .request("application/xml").get();

        verifyXMLResponse(response, expectEmptyContainer("depth1-cont"));

        // Test config with depth 2

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "2").request("application/xml")
                .get();

        // String
        // xml="<depth1-cont><depth2-cont1/><depth2-cont2/><depth2-leaf1>depth2-leaf1-value</depth2-leaf1></depth1-cont>";
        // Response mr=mock(Response.class);
        // when(mr.getEntity()).thenReturn( new
        // java.io.StringBufferInputStream(xml) );

        verifyXMLResponse(
                response,
                expectContainer("depth1-cont", expectEmptyContainer("depth2-cont1"),
                        expectEmptyContainer("depth2-cont2"), expectLeaf("depth2-leaf1", "depth2-leaf1-value")));

        // Test config with depth 3

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "3").request("application/xml")
                .get();

        verifyXMLResponse(
                response,
                expectContainer(
                        "depth1-cont",
                        expectContainer("depth2-cont1", expectEmptyContainer("depth3-cont1"),
                                expectLeaf("depth3-leaf1", "depth3-leaf1-value")),
                        expectContainer("depth2-cont2", expectEmptyContainer("depth3-cont2"),
                                expectLeaf("depth3-leaf2", "depth3-leaf2-value")),
                        expectLeaf("depth2-leaf1", "depth2-leaf1-value")));

        // Test config with depth 4

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "4").request("application/xml")
                .get();

        verifyXMLResponse(
                response,
                expectContainer(
                        "depth1-cont",
                        expectContainer(
                                "depth2-cont1",
                                expectContainer("depth3-cont1", expectEmptyContainer("depth4-cont1"),
                                        expectLeaf("depth4-leaf1", "depth4-leaf1-value")),
                                expectLeaf("depth3-leaf1", "depth3-leaf1-value")),
                        expectContainer(
                                "depth2-cont2",
                                expectContainer("depth3-cont2", expectEmptyContainer("depth4-cont2"),
                                        expectLeaf("depth4-leaf2", "depth4-leaf2-value")),
                                expectLeaf("depth3-leaf2", "depth3-leaf2-value")),
                        expectLeaf("depth2-leaf1", "depth2-leaf1-value")));

        // Test config with depth 5

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "5").request("application/xml")
                .get();

        verifyXMLResponse(
                response,
                expectContainer(
                        "depth1-cont",
                        expectContainer(
                                "depth2-cont1",
                                expectContainer(
                                        "depth3-cont1",
                                        expectContainer("depth4-cont1",
                                                expectLeaf("depth5-leaf1", "depth5-leaf1-value")),
                                        expectLeaf("depth4-leaf1", "depth4-leaf1-value")),
                                expectLeaf("depth3-leaf1", "depth3-leaf1-value")),
                        expectContainer(
                                "depth2-cont2",
                                expectContainer(
                                        "depth3-cont2",
                                        expectContainer("depth4-cont2",
                                                expectLeaf("depth5-leaf2", "depth5-leaf2-value")),
                                        expectLeaf("depth4-leaf2", "depth4-leaf2-value")),
                                expectLeaf("depth3-leaf2", "depth3-leaf2-value")),
                        expectLeaf("depth2-leaf1", "depth2-leaf1-value")));

        // Test config with depth unbounded

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "unbounded")
                .request("application/xml").get();

        verifyXMLResponse(
                response,
                expectContainer(
                        "depth1-cont",
                        expectContainer(
                                "depth2-cont1",
                                expectContainer(
                                        "depth3-cont1",
                                        expectContainer("depth4-cont1",
                                                expectLeaf("depth5-leaf1", "depth5-leaf1-value")),
                                        expectLeaf("depth4-leaf1", "depth4-leaf1-value")),
                                expectLeaf("depth3-leaf1", "depth3-leaf1-value")),
                        expectContainer(
                                "depth2-cont2",
                                expectContainer(
                                        "depth3-cont2",
                                        expectContainer("depth4-cont2",
                                                expectLeaf("depth5-leaf2", "depth5-leaf2-value")),
                                        expectLeaf("depth4-leaf2", "depth4-leaf2-value")),
                                expectLeaf("depth3-leaf2", "depth3-leaf2-value")),
                        expectLeaf("depth2-leaf1", "depth2-leaf1-value")));

        // Test operational

        CompositeNode depth2Cont1 = toCompositeNode(toCompositeNodeData(
                toNestedQName("depth2-cont1"),
                toCompositeNodeData(
                        toNestedQName("depth3-cont1"),
                        toCompositeNodeData(toNestedQName("depth4-cont1"),
                                toSimpleNodeData(toNestedQName("depth5-leaf1"), "depth5-leaf1-value")),
                        toSimpleNodeData(toNestedQName("depth4-leaf1"), "depth4-leaf1-value")),
                toSimpleNodeData(toNestedQName("depth3-leaf1"), "depth3-leaf1-value")));

        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(depth2Cont1);

        response = target("/operational/nested-module:depth1-cont/depth2-cont1").queryParam("depth", "3")
                .request("application/xml").get();

        verifyXMLResponse(
                response,
                expectContainer(
                        "depth2-cont1",
                        expectContainer("depth3-cont1", expectEmptyContainer("depth4-cont1"),
                                expectLeaf("depth4-leaf1", "depth4-leaf1-value")),
                        expectLeaf("depth3-leaf1", "depth3-leaf1-value")));
    }

    @Test
    public void getDataWithInvalidDepthParameterTest() {

        ControllerContext.getInstance().setGlobalSchema(schemaContextModules);

        final MultivaluedMap<String, String> paramMap = new MultivaluedHashMap<>();
        paramMap.putSingle("depth", "1o");
        UriInfo mockInfo = mock(UriInfo.class);
        when(mockInfo.getQueryParameters(false)).thenAnswer(new Answer<MultivaluedMap<String, String>>() {
            @Override
            public MultivaluedMap<String, String> answer(final InvocationOnMock invocation) {
                return paramMap;
            }
        });

        getDataWithInvalidDepthParameterTest(mockInfo);

        paramMap.putSingle("depth", "0");
        getDataWithInvalidDepthParameterTest(mockInfo);

        paramMap.putSingle("depth", "-1");
        getDataWithInvalidDepthParameterTest(mockInfo);
    }

    private void getDataWithInvalidDepthParameterTest(final UriInfo uriInfo) {
        try {
            restconfImpl.readConfigurationData("nested-module:depth1-cont", uriInfo);
            fail("Expected RestconfDocumentedException");
        } catch (RestconfDocumentedException e) {
            assertTrue("Unexpected error message: " + e.getErrors().get(0).getErrorMessage(), e.getErrors().get(0)
                    .getErrorMessage().contains("depth"));
        }
    }

    private void verifyXMLResponse(final Response response, final NodeData nodeData) {

        Document doc = TestUtils.loadDocumentFrom((InputStream) response.getEntity());
        assertNotNull("Could not parse XML document", doc);

        // System.out.println(TestUtils.getDocumentInPrintableForm( doc ));

        verifyContainerElement(doc.getDocumentElement(), nodeData);
    }

    @SuppressWarnings("unchecked")
    private void verifyContainerElement(final Element element, final NodeData nodeData) {

        assertEquals("Element local name", nodeData.key, element.getNodeName());

        NodeList childNodes = element.getChildNodes();
        if (nodeData.data == null) { // empty container
            assertTrue("Expected no child elements for \"" + element.getNodeName() + "\"", childNodes.getLength() == 0);
            return;
        }

        Map<String, NodeData> expChildMap = Maps.newHashMap();
        for (NodeData expChild : (List<NodeData>) nodeData.data) {
            expChildMap.put(expChild.key.toString(), expChild);
        }

        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node actualChild = childNodes.item(i);
            if (!(actualChild instanceof Element)) {
                continue;
            }

            Element actualElement = (Element) actualChild;
            NodeData expChild = expChildMap.remove(actualElement.getNodeName());
            assertNotNull(
                    "Unexpected child element for parent \"" + element.getNodeName() + "\": "
                            + actualElement.getNodeName(), expChild);

            if (expChild.data == null || expChild.data instanceof List) {
                verifyContainerElement(actualElement, expChild);
            } else {
                assertEquals("Text content for element: " + actualElement.getNodeName(), expChild.data,
                        actualElement.getTextContent());
            }
        }

        if (!expChildMap.isEmpty()) {
            fail("Missing elements for parent \"" + element.getNodeName() + "\": " + expChildMap.keySet());
        }
    }

    private NodeData expectContainer(final String name, final NodeData... childData) {
        return new NodeData(name, Lists.newArrayList(childData));
    }

    private NodeData expectEmptyContainer(final String name) {
        return new NodeData(name, null);
    }

    private NodeData expectLeaf(final String name, final Object value) {
        return new NodeData(name, value);
    }

    private QName toNestedQName(final String localName) {
        return QName.create("urn:nested:module", "2014-06-3", localName);
    }

    @SuppressWarnings("unchecked")
    private CompositeNode toCompositeNode(final NodeData nodeData) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName((QName) nodeData.key);

        for (NodeData child : (List<NodeData>) nodeData.data) {
            if (child.data instanceof List) {
                builder.add(toCompositeNode(child));
            } else {
                builder.addLeaf((QName) child.key, child.data);
            }
        }

        return builder.toInstance();
    }

    private NodeData toCompositeNodeData(final QName key, final NodeData... childData) {
        return new NodeData(key, Lists.newArrayList(childData));
    }

    private NodeData toSimpleNodeData(final QName key, final Object value) {
        return new NodeData(key, value);
    }
}
