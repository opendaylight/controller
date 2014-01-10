package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.JSON;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.entity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.api.Draft01;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPutOperationTest extends JerseyTest {

    String xmlData;
    String jsonData;

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);

    }

    @Before
    public void logs() throws IOException {
        List<LogRecord> loggedRecords = getLoggedRecords();
        for (LogRecord l : loggedRecords) {
            System.out.println(l.getMessage());
        }
    }

    private void loadData() throws IOException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        String jsonPath = RestconfImplTest.class.getResource("/parts/ietf-interfaces_interfaces.json").getPath();
        jsonData = TestUtils.loadTextFile(jsonPath);
    }

    @Test
    public void putDataViaUrlTest() throws IOException {
        loadData();
        putDataViaUrlReturnStatusCodeTest(TransactionStatus.COMMITED, 200);
        putDataViaUrlReturnStatusCodeTest(TransactionStatus.FAILED, 500);
    }

    @Test
    public void testRpcResultCommitedToStatusCodesWithMountPoint() throws UnsupportedEncodingException,
            FileNotFoundException, URISyntaxException {

        mockCommitConfigurationDataPutMethod(TransactionStatus.COMMITED);

        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data2.xml");
        String xml = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        Entity<String> entity = Entity.entity(xml, Draft02.MediaTypes.DATA + XML);

        MountService mockMountService = mock(MountService.class);
        SchemaContext otherSchemaContext = TestUtils.loadSchemaContext("/full-versions/yangs2");
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(
                new DummyMountInstanceImpl.Builder().setSchemaContext(otherSchemaContext).build());

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/test-module:cont");
        Response response = target(uri).request(Draft02.MediaTypes.DATA + XML).put(entity);
        assertEquals(200, response.getStatus());
    }

    private void putDataViaUrlReturnStatusCodeTest(TransactionStatus statusName, int statusCode)
            throws UnsupportedEncodingException {

        mockCommitConfigurationDataPutMethod(statusName);

        putDatastoreDataViaUrlTest(statusCode);
        putConfigDataViaUrlTest(statusCode);
    }

    private void mockCommitConfigurationDataPutMethod(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);
    }

    private void putConfigDataViaUrlTest(int responseStatus) throws UnsupportedEncodingException {
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + JSON, jsonData, responseStatus);
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + XML, xmlData, responseStatus);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_JSON, jsonData, responseStatus);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_XML, xmlData, responseStatus);
        putDataViaUrlTest("/config/", MediaType.TEXT_XML, xmlData, responseStatus);

    }

    private void putDatastoreDataViaUrlTest(int responseStatus) throws UnsupportedEncodingException {
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + JSON, jsonData, responseStatus);
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + XML, xmlData, responseStatus);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_JSON, jsonData, responseStatus);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_XML, xmlData, responseStatus);
        putDataViaUrlTest("/datastore/", MediaType.TEXT_XML, xmlData, responseStatus);
    }

    private void putDataViaUrlTest(String uriPrefix, String mediaType, String data, int responseStatus)
            throws UnsupportedEncodingException {
        String uri = createUri(uriPrefix, "ietf-interfaces:interfaces/interface/eth0");
        Response response = target(uri).request(mediaType).put(entity(data, mediaType));
        assertEquals(responseStatus, response.getStatus());
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.RECORD_LOG_LEVEL);
        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }
}
