package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
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
import java.util.concurrent.Future;
import java.util.logging.Level;

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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPutOperationTest extends JerseyTest {

    private static String xmlData;
    private static String jsonData;

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;

    @BeforeClass
    public static void init() throws IOException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        loadData();
    }

    @Before
    public void logs() throws IOException {
        /* enable/disable Jersey logs to console */
        /*
         * List<LogRecord> loggedRecords = getLoggedRecords(); for (LogRecord l
         * : loggedRecords) { System.out.println(l.getMessage()); }
         */
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        /*
         * enable(TestProperties.LOG_TRAFFIC);
         */
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.RECORD_LOG_LEVEL);
        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    /**
     * Test method
     * {@link RestconfImpl#updateConfigurationData(String, CompositeNode)} of
     * RestconfImpl for "/config/...identifier..." URL. Return status code is
     * 200.
     * 
     */
    @Test
    public void putConfigDataViaUrlTest200() throws UnsupportedEncodingException {
        mockCommitConfigurationDataPutMethod(TransactionStatus.COMMITED);
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + JSON, jsonData, 200);
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + XML, xmlData, 200);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_JSON, jsonData, 200);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_XML, xmlData, 200);
        putDataViaUrlTest("/config/", MediaType.TEXT_XML, xmlData, 200);

    }

    /**
     * Test method
     * {@link RestconfImpl#updateConfigurationData(String, CompositeNode)} of
     * RestconfImpl for "/config/...identifier..." URL. Return status code is
     * 500.
     * 
     */
    @Test
    public void putConfigDataViaUrlTest500() throws UnsupportedEncodingException {
        mockCommitConfigurationDataPutMethod(TransactionStatus.FAILED);
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + JSON, jsonData, 500);
        putDataViaUrlTest("/config/", Draft02.MediaTypes.DATA + XML, xmlData, 500);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_JSON, jsonData, 500);
        putDataViaUrlTest("/config/", MediaType.APPLICATION_XML, xmlData, 500);
        putDataViaUrlTest("/config/", MediaType.TEXT_XML, xmlData, 500);

    }

    /**
     * Test method
     * {@link RestconfImpl#updateConfigurationData(String, CompositeNode)} of
     * RestconfImpl for "/datastore/...identifier..." URL. Return status code is
     * 200.
     * 
     */
    @Test
    public void putDatastoreDataViaUrlTest200() throws UnsupportedEncodingException {
        mockCommitConfigurationDataPutMethod(TransactionStatus.COMMITED);
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + JSON, jsonData, 200);
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + XML, xmlData, 200);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_JSON, jsonData, 200);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_XML, xmlData, 200);
        putDataViaUrlTest("/datastore/", MediaType.TEXT_XML, xmlData, 200);
    }

    /**
     * Test method
     * {@link RestconfImpl#updateConfigurationData(String, CompositeNode)} of
     * RestconfImpl for "/datastore/...identifier..." URL. Return status code is
     * 500.
     * 
     */
    @Test
    public void putDatastoreDataViaUrlTest500() throws UnsupportedEncodingException {
        mockCommitConfigurationDataPutMethod(TransactionStatus.FAILED);
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + JSON, jsonData, 500);
        putDataViaUrlTest("/datastore/", Draft01.MediaTypes.DATA + XML, xmlData, 500);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_JSON, jsonData, 500);
        putDataViaUrlTest("/datastore/", MediaType.APPLICATION_XML, xmlData, 500);
        putDataViaUrlTest("/datastore/", MediaType.TEXT_XML, xmlData, 500);
    }

    @Test
    public void testRpcResultCommitedToStatusCodesWithMountPoint() throws UnsupportedEncodingException,
            FileNotFoundException, URISyntaxException {

        mockCommitConfigurationDataPutMethod(TransactionStatus.COMMITED);

        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data2.xml");
        String xml = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        Entity<String> entity = Entity.entity(xml, Draft02.MediaTypes.DATA + XML);

        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(
                new DummyMountInstanceImpl.Builder().setSchemaContext(schemaContextTestModule).build());

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/test-module:cont");
        Response response = target(uri).request(Draft02.MediaTypes.DATA + XML).put(entity);
        assertEquals(200, response.getStatus());
    }

    private void putDataViaUrlTest(String uriPrefix, String mediaType, String data, int responseStatus)
            throws UnsupportedEncodingException {
        String uri = createUri(uriPrefix, "ietf-interfaces:interfaces/interface/eth0");
        Response response = target(uri).request(mediaType).put(entity(data, mediaType));
        assertEquals(responseStatus, response.getStatus());
    }

    private static void loadData() throws IOException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        String jsonPath = RestconfImplTest.class.getResource("/parts/ietf-interfaces_interfaces.json").getPath();
        jsonData = TestUtils.loadTextFile(jsonPath);
    }

    private void mockCommitConfigurationDataPutMethod(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);
    }

}
