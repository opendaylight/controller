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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.FileNotFoundException;
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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.RestconfApplication;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    @SuppressWarnings("rawtypes")
    private static NormalizedNode answerFromGet;

    private static SchemaContext schemaContextModules;
    private static SchemaContext schemaContextBehindMountPoint;

    private static final String RESTCONF_NS = "urn:ietf:params:xml:ns:yang:ietf-restconf";

    @BeforeClass
    public static void init() throws FileNotFoundException, ParseException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        answerFromGet = TestUtils.prepareNormalizedNodeWithIetfInterfacesInterfacesData();

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
        resourceConfig = resourceConfig.registerInstances(restconfImpl, new NormalizedNodeJsonBodyWriter(),
                new NormalizedNodeXmlBodyWriter(), new XmlNormalizedNodeBodyReader(), new JsonNormalizedNodeBodyReader());
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        resourceConfig.registerClasses(new RestconfApplication().getClasses());
        return resourceConfig;
    }

    private void setControllerContext(final SchemaContext schemaContext) {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        restconfImpl.setControllerContext(controllerContext);
    }

    /**
     * Tests of status codes for "/operational/{identifier}".
     */
    @Test
    public void getOperationalStatusCodes() throws UnsupportedEncodingException {
        setControllerContext(schemaContextYangsIetf);
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
        setControllerContext(schemaContextYangsIetf);
        mockReadConfigurationDataMethod();
        String uri = "/config/ietf-interfaces:interfaces/interface/eth0";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));

        uri = "/config/wrong-module:interfaces/interface/eth0";
        assertEquals(400, get(uri, MediaType.APPLICATION_XML));
    }

    /**
     * MountPoint test. URI represents mount point.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getDataWithUrlMountPoint() throws UnsupportedEncodingException, URISyntaxException, ParseException {
        when(brokerFacade.readConfigurationData(any(DOMMountPoint.class), any(YangInstanceIdentifier.class))).thenReturn(
                prepareCnDataForMountPointTest(false));
        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

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
     * {@link BrokerFacade#readConfigurationData(DOMMountPoint, YangInstanceIdentifier)} which is called in
     * method {@link RestconfImpl#readConfigurationData}
     *
     * @throws ParseException
     */
    @Test
    public void getDataWithSlashesBehindMountPoint() throws Exception {
        final YangInstanceIdentifier awaitedInstanceIdentifier = prepareInstanceIdentifierForList();
        when(brokerFacade.readConfigurationData(any(DOMMountPoint.class), eq(awaitedInstanceIdentifier))).thenReturn(
                prepareCnDataForSlashesBehindMountPointTest());
        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont/lst1/GigabitEthernet0%2F0%2F0%2F0";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    private YangInstanceIdentifier prepareInstanceIdentifierForList() throws Exception {
        final List<PathArgument> parameters = new ArrayList<>();

        final QName qNameCont = newTestModuleQName("cont");
        final QName qNameList = newTestModuleQName("lst1");
        final QName qNameKeyList = newTestModuleQName("lf11");

        parameters.add(new YangInstanceIdentifier.NodeIdentifier(qNameCont));
        parameters.add(new YangInstanceIdentifier.NodeIdentifier(qNameList));
        parameters.add(new YangInstanceIdentifier.NodeIdentifierWithPredicates(qNameList, qNameKeyList,
                "GigabitEthernet0/0/0/0"));
        return YangInstanceIdentifier.create(parameters);
    }

    private QName newTestModuleQName(String localPart) throws Exception {
        final Date revision = new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-09");
        final URI uri = new URI("test:module");
        return QName.create(uri, revision, localPart);
    }

    @Test
    public void getDataMountPointIntoHighestElement() throws UnsupportedEncodingException, URISyntaxException,
            ParseException {
        when(brokerFacade.readConfigurationData(any(DOMMountPoint.class), any(YangInstanceIdentifier.class))).thenReturn(
                prepareCnDataForMountPointTest(true));
        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getDataWithIdentityrefInURL() throws Exception {
        setControllerContext(schemaContextTestModule);

        QName moduleQN = newTestModuleQName("module");
        ImmutableMap<QName, Object> keyMap = ImmutableMap.<QName, Object>builder()
                .put(newTestModuleQName("type"), newTestModuleQName("test-identity"))
                .put(newTestModuleQName("name"), "foo").build();
        YangInstanceIdentifier iid = YangInstanceIdentifier.builder().node(newTestModuleQName("modules"))
                .node(moduleQN).nodeWithKey(moduleQN, keyMap).build();
        @SuppressWarnings("rawtypes")
        NormalizedNode data = ImmutableMapNodeBuilder.create().withNodeIdentifier(
                new NodeIdentifier(moduleQN)).withChild(ImmutableNodes.mapEntryBuilder()
                        .withNodeIdentifier(new NodeIdentifierWithPredicates(moduleQN, keyMap))
                        .withChild(ImmutableNodes.leafNode(newTestModuleQName("type"), newTestModuleQName("test-identity")))
                        .withChild(ImmutableNodes.leafNode(newTestModuleQName("name"), "foo"))
                        .withChild(ImmutableNodes.leafNode(newTestModuleQName("data"), "bar")).build()).build();
        when(brokerFacade.readConfigurationData(iid)).thenReturn(data);

        String uri = "/config/test-module:modules/module/test-module:test-identity/foo";
        assertEquals(200, get(uri, MediaType.APPLICATION_XML));
    }

    // /modules
    @Test
    public void getModulesTest() throws UnsupportedEncodingException, FileNotFoundException {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setGlobalSchema(schemaContextModules);
        restconfImpl.setControllerContext(controllerContext);

        final String uri = "/modules";

        Response response = target(uri).request("application/yang.api+json").get();
        validateModulesResponseJson(response);

        response = target(uri).request("application/yang.api+xml").get();
        validateModulesResponseXml(response,schemaContextModules);
    }

    // /streams/
    @Test
    @Ignore // FIXME : find why it is fail by in gerrit build
    public void getStreamsTest() throws UnsupportedEncodingException, FileNotFoundException {
        setControllerContext(schemaContextModules);

        final String uri = "/streams";

        Response response = target(uri).request("application/yang.api+json").get();
        final String responseBody = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("streams"));

        response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        final Document responseXmlBody = response.readEntity(Document.class);
        assertNotNull(responseXmlBody);
        final Element rootNode = responseXmlBody.getDocumentElement();

        assertEquals("streams", rootNode.getLocalName());
        assertEquals(RESTCONF_NS, rootNode.getNamespaceURI());
    }

    // /modules/module
    @Test
    public void getModuleTest() throws FileNotFoundException, UnsupportedEncodingException {
        setControllerContext(schemaContextModules);

        final String uri = "/modules/module/module2/2014-01-02";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        final Document responseXml = response.readEntity(Document.class);

        final QName qname = assertedModuleXmlToModuleQName(responseXml.getDocumentElement());
        assertNotNull(qname);

        assertEquals("module2", qname.getLocalName());
        assertEquals("module:2", qname.getNamespace().toString());
        assertEquals("2014-01-02", qname.getFormattedRevision());

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        final String responseBody = response.readEntity(String.class);
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        final String[] split = responseBody.split("\"module\"");
        assertEquals("\"module\" element is returned more then once", 2, split.length);

    }

    // /operations
    @Test
    public void getOperationsTest() throws FileNotFoundException, UnsupportedEncodingException {
        setControllerContext(schemaContextModules);

        final String uri = "/operations";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        final Document responseDoc = response.readEntity(Document.class);
        validateOperationsResponseXml(responseDoc, schemaContextModules);

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        final String responseBody = response.readEntity(String.class);
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

        final Element operationsElem = responseDoc.getDocumentElement();
        assertEquals(RESTCONF_NS, operationsElem.getNamespaceURI());
        assertEquals("operations", operationsElem.getLocalName());

        final NodeList operationsList = operationsElem.getChildNodes();
        final HashSet<String> foundOperations = new HashSet<>();

        for (int i = 0; i < operationsList.getLength(); i++) {
            final org.w3c.dom.Node operation = operationsList.item(i);
            foundOperations.add(operation.getLocalName());
        }

        for (final RpcDefinition schemaOp : schemaContext.getOperations()) {
            assertTrue(foundOperations.contains(schemaOp.getQName().getLocalName()));
        }
    }

    // /operations/pathToMountPoint/yang-ext:mount
    @Test
    public void getOperationsBehindMountPointTest() throws FileNotFoundException, UnsupportedEncodingException {
        setControllerContext(schemaContextModules);

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/operations/ietf-interfaces:interfaces/interface/0/yang-ext:mount/";

        Response response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());

        final Document responseDoc = response.readEntity(Document.class);
        validateOperationsResponseXml(responseDoc, schemaContextBehindMountPoint);

        response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        final String responseBody = response.readEntity(String.class);
        assertTrue("Json response for /operations/mount_point rpc-behind-module1 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module1", "module1-behind-mount-point").find());
        assertTrue("Json response for /operations/mount_point rpc-behind-module2 is incorrect",
                validateOperationsResponseJson(responseBody, "rpc-behind-module2", "module2-behind-mount-point").find());

    }

    private Matcher validateOperationsResponseJson(final String searchIn, final String rpcName, final String moduleName) {
        final StringBuilder regex = new StringBuilder();
        regex.append(".*\"" + rpcName + "\"");
        final Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);

    }

    private Matcher validateOperationsResponseXml(final String searchIn, final String rpcName, final String namespace) {
        final StringBuilder regex = new StringBuilder();

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
        final Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);
    }

    // /restconf/modules/pathToMountPoint/yang-ext:mount
    @Test
    public void getModulesBehindMountPoint() throws FileNotFoundException, UnsupportedEncodingException {
        setControllerContext(schemaContextModules);

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/modules/ietf-interfaces:interfaces/interface/0/yang-ext:mount/";

        Response response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        final String responseBody = response.readEntity(String.class);

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
        setControllerContext(schemaContextModules);

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextBehindMountPoint);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/modules/module/ietf-interfaces:interfaces/interface/0/yang-ext:mount/module1-behind-mount-point/2014-02-03";

        Response response = target(uri).request("application/yang.api+json").get();
        assertEquals(200, response.getStatus());
        final String responseBody = response.readEntity(String.class);

        assertTrue(
                "module1-behind-mount-point in json wasn't found",
                prepareJsonRegex("module1-behind-mount-point", "2014-02-03", "module:1:behind:mount:point",
                        responseBody).find());
        final String[] split = responseBody.split("\"module\"");
        assertEquals("\"module\" element is returned more then once", 2, split.length);

        response = target(uri).request("application/yang.api+xml").get();
        assertEquals(200, response.getStatus());
        final Document responseXml = response.readEntity(Document.class);

        final QName module = assertedModuleXmlToModuleQName(responseXml.getDocumentElement());

        assertEquals("module1-behind-mount-point", module.getLocalName());
        assertEquals("2014-02-03", module.getFormattedRevision());
        assertEquals("module:1:behind:mount:point", module.getNamespace().toString());


    }

    private void validateModulesResponseXml(final Response response, final SchemaContext schemaContext) {
        assertEquals(200, response.getStatus());
        final Document responseBody = response.readEntity(Document.class);
        final NodeList moduleNodes = responseBody.getDocumentElement().getElementsByTagNameNS(RESTCONF_NS, "module");

        assertTrue(moduleNodes.getLength() > 0);

        final HashSet<QName> foundModules = new HashSet<>();

        for(int i=0;i < moduleNodes.getLength();i++) {
            final org.w3c.dom.Node module = moduleNodes.item(i);

            final QName name = assertedModuleXmlToModuleQName(module);
            foundModules.add(name);
        }

        assertAllModules(foundModules,schemaContext);
    }

    private void assertAllModules(final Set<QName> foundModules, final SchemaContext schemaContext) {
        for(final Module module : schemaContext.getModules()) {
            final QName current = QName.create(module.getQNameModule(),module.getName());
            assertTrue("Module not found in response.",foundModules.contains(current));
        }

    }

    private QName assertedModuleXmlToModuleQName(final org.w3c.dom.Node module) {
        assertEquals("module", module.getLocalName());
        assertEquals(RESTCONF_NS, module.getNamespaceURI());
        String revision = null;
        String namespace = null;
        String name = null;


        final NodeList childNodes = module.getChildNodes();

        for(int i =0;i < childNodes.getLength(); i++) {
            final org.w3c.dom.Node child = childNodes.item(i);
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
        final String responseBody = response.readEntity(String.class);

        assertTrue("Module1 in json wasn't found", prepareJsonRegex("module1", "2014-01-01", "module:1", responseBody)
                .find());
        assertTrue("Module2 in json wasn't found", prepareJsonRegex("module2", "2014-01-02", "module:2", responseBody)
                .find());
        assertTrue("Module3 in json wasn't found", prepareJsonRegex("module3", "2014-01-03", "module:3", responseBody)
                .find());
    }

    private Matcher prepareJsonRegex(final String module, final String revision, final String namespace,
            final String searchIn) {
        final StringBuilder regex = new StringBuilder();
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
        final Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return ptrn.matcher(searchIn);

    }


    private int get(final String uri, final String mediaType) {
        return target(uri).request(mediaType).get().getStatus();
    }

    /**
    container cont {
        container cont1 {
            leaf lf11 {
                type string;
            }
    */
    @SuppressWarnings("rawtypes")
    private NormalizedNode prepareCnDataForMountPointTest(final boolean wrapToCont) throws URISyntaxException, ParseException {
        final String testModuleDate = "2014-01-09";
        final ContainerNode contChild = Builders
                .containerBuilder()
                .withNodeIdentifier(TestUtils.getNodeIdentifier("cont1", "test:module", testModuleDate))
                .withChild(
                        Builders.leafBuilder()
                                .withNodeIdentifier(TestUtils.getNodeIdentifier("lf11", "test:module", testModuleDate))
                                .withValue("lf11 value").build()).build();

        if (wrapToCont) {
            return Builders.containerBuilder()
                    .withNodeIdentifier(TestUtils.getNodeIdentifier("cont", "test:module", testModuleDate))
                    .withChild(contChild).build();
        }
        return contChild;

    }

    @SuppressWarnings("unchecked")
    private void mockReadOperationalDataMethod() {
        when(brokerFacade.readOperationalData(any(YangInstanceIdentifier.class))).thenReturn(answerFromGet);
    }

    @SuppressWarnings("unchecked")
    private void mockReadConfigurationDataMethod() {
        when(brokerFacade.readConfigurationData(any(YangInstanceIdentifier.class))).thenReturn(answerFromGet);
    }

    @SuppressWarnings("rawtypes")
    private NormalizedNode prepareCnDataForSlashesBehindMountPointTest() throws ParseException {
        return ImmutableMapEntryNodeBuilder
                .create()
                .withNodeIdentifier(
                        TestUtils.getNodeIdentifierPredicate("lst1", "test:module", "2014-01-09", "lf11",
                                "GigabitEthernet0/0/0/0"))
                .withChild(
                        ImmutableLeafNodeBuilder.create()
                                .withNodeIdentifier(TestUtils.getNodeIdentifier("lf11", "test:module", "2014-01-09"))
                                .withValue("GigabitEthernet0/0/0/0").build()).build();

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
        mockReadOperationalDataMethod();
        final String uri = "/" + target + "/ietf-interfaces:interfaces/interface/eth0";
        Response response = target(uri).queryParam("prettyPrint", "false").request("application/xml").get();
        final String xmlData = response.readEntity(String.class);

        Pattern pattern = Pattern.compile(".*(>\\s+|\\s+<).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlData);
        // XML element can't surrounded with white character (e.g ">    " or
        // "    <")
        assertFalse(matcher.matches());

        response = target(uri).queryParam("prettyPrint", "false").request("application/json").get();
        final String jsonData = response.readEntity(String.class);
        pattern = Pattern.compile(".*(\\}\\s+|\\s+\\{|\\]\\s+|\\s+\\[|\\s+:|:\\s+).*", Pattern.DOTALL);
        matcher = pattern.matcher(jsonData);
        // JSON element can't surrounded with white character (e.g "} ", " {",
        // "] ", " [", " :" or ": ")
        assertFalse(matcher.matches());
    }

    /**
     * Tests behavior when invalid value of depth URI parameter
     */
    @Test
    @Ignore
    public void getDataWithInvalidDepthParameterTest() {
        setControllerContext(schemaContextModules);

        final MultivaluedMap<String, String> paramMap = new MultivaluedHashMap<>();
        paramMap.putSingle("depth", "1o");
        final UriInfo mockInfo = mock(UriInfo.class);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void getDataWithInvalidDepthParameterTest(final UriInfo uriInfo) {
        try {
            final QName qNameDepth1Cont = QName.create("urn:nested:module", "2014-06-3", "depth1-cont");
            final YangInstanceIdentifier ii = YangInstanceIdentifier.builder().node(qNameDepth1Cont).build();
            final NormalizedNode value = (Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(qNameDepth1Cont)).build());
            when(brokerFacade.readConfigurationData(eq(ii))).thenReturn(value);
            restconfImpl.readConfigurationData("nested-module:depth1-cont", uriInfo);
            fail("Expected RestconfDocumentedException");
        } catch (final RestconfDocumentedException e) {
            assertTrue("Unexpected error message: " + e.getErrors().get(0).getErrorMessage(), e.getErrors().get(0)
                    .getErrorMessage().contains("depth"));
        }
    }

    @SuppressWarnings("unused")
    private void verifyXMLResponse(final Response response, final NodeData nodeData) {
        final Document doc = response.readEntity(Document.class);
//        Document doc = TestUtils.loadDocumentFrom((InputStream) response.getEntity());
//        System.out.println();
        assertNotNull("Could not parse XML document", doc);

        // System.out.println(TestUtils.getDocumentInPrintableForm( doc ));

        verifyContainerElement(doc.getDocumentElement(), nodeData);
    }

    @SuppressWarnings("unchecked")
    private void verifyContainerElement(final Element element, final NodeData nodeData) {

        assertEquals("Element local name", nodeData.key, element.getLocalName());

        final NodeList childNodes = element.getChildNodes();
        if (nodeData.data == null) { // empty container
            assertTrue("Expected no child elements for \"" + element.getLocalName() + "\"", childNodes.getLength() == 0);
            return;
        }

        final Map<String, NodeData> expChildMap = Maps.newHashMap();
        for (final NodeData expChild : (List<NodeData>) nodeData.data) {
            expChildMap.put(expChild.key.toString(), expChild);
        }

        for (int i = 0; i < childNodes.getLength(); i++) {
            final org.w3c.dom.Node actualChild = childNodes.item(i);
            if (!(actualChild instanceof Element)) {
                continue;
            }

            final Element actualElement = (Element) actualChild;
            final NodeData expChild = expChildMap.remove(actualElement.getLocalName());
            assertNotNull(
                    "Unexpected child element for parent \"" + element.getLocalName() + "\": "
                            + actualElement.getLocalName(), expChild);

            if (expChild.data == null || expChild.data instanceof List) {
                verifyContainerElement(actualElement, expChild);
            } else {
                assertEquals("Text content for element: " + actualElement.getLocalName(), expChild.data,
                        actualElement.getTextContent());
            }
        }

        if (!expChildMap.isEmpty()) {
            fail("Missing elements for parent \"" + element.getLocalName() + "\": " + expChildMap.keySet());
        }
    }

}
