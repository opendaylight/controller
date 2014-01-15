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
import java.net.URI;
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
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPostOperationTest extends JerseyTest {

    private static String xmlData;
    private static String xmlDataAbsolutePath;
    private static String jsonData;
    private static String jsonDataAbsolutePath;
    private static String xmlDataRpcInput;
    private static CompositeNodeWrapper cnSnDataOutput;
    private static String jsonDataRpcInput;
    private static String xmlData2;

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;

    @BeforeClass
    public static void init() throws URISyntaxException, IOException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext = ControllerContext.getInstance();
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        loadData();
    }

    @Before
    public void logs() throws IOException, URISyntaxException {
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

    @Test
    public void postOperationsDataViaUrl200() throws URISyntaxException, IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 200);
        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 200);
        postOperationsDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput, 200);

        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 200);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 200);
        postOperationsDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput, 200);
        postOperationsDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput, 200);
    }

    @Test
    public void postOperationsDataViaUrl204() throws URISyntaxException, IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 204);
        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 204);
        postOperationsDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput, 204);

        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 204);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 204);
        postOperationsDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput, 204);
        postOperationsDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput, 204);
    }

    @Test
    public void postOperationsDataViaUrl500() throws URISyntaxException, IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 500);
        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 500);
        postOperationsDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput, 500);

        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 500);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 500);
        postOperationsDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput, 500);
        postOperationsDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput, 500);
    }

    @Test
    public void postOperationsDataViaUrl400() throws URISyntaxException, IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 400);
        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 400);
        postOperationsDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput, 400);

        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 400);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 400);
        postOperationsDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput, 400);
        postOperationsDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput, 400);
    }

    @Test
    public void postOperationsDataViaUrl404() throws URISyntaxException, IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 404);
        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput, 404);
        postOperationsDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput, 404);

        postOperationsDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 404);
        postOperationsDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput, 404);
        postOperationsDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput, 404);
        postOperationsDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput, 404);
    }

    @Test
    public void postConfigDataViaUrlConfigOnlyTest204() throws UnsupportedEncodingException, FileNotFoundException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + JSON, jsonDataAbsolutePath, 204);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + XML, xmlDataAbsolutePath, 204);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_JSON, jsonDataAbsolutePath, 204);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_XML, xmlDataAbsolutePath, 204);
        postDataViaUrlTest("/config", "", MediaType.TEXT_XML, xmlDataAbsolutePath, 204);
    }

    @Test
    public void postConfigDataViaUrlConfigOnlyTest202() throws UnsupportedEncodingException, FileNotFoundException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(null);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + JSON, jsonDataAbsolutePath, 202);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + XML, xmlDataAbsolutePath, 202);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_JSON, jsonDataAbsolutePath, 202);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_XML, xmlDataAbsolutePath, 202);
        postDataViaUrlTest("/config", "", MediaType.TEXT_XML, xmlDataAbsolutePath, 202);
    }

    @Test
    public void postConfigDataViaUrlConfigOnlyTest500() throws UnsupportedEncodingException, FileNotFoundException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + JSON, jsonDataAbsolutePath, 500);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + XML, xmlDataAbsolutePath, 500);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_JSON, jsonDataAbsolutePath, 500);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_XML, xmlDataAbsolutePath, 500);
        postDataViaUrlTest("/config", "", MediaType.TEXT_XML, xmlDataAbsolutePath, 500);
    }

    @Test
    public void postConfigDataViaUrlTest204() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + JSON, jsonData, 204);
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + XML, xmlData, 204);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_JSON, jsonData, 204);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_XML, xmlData, 204);
        postDataViaUrlTest("/config/", urlPath, MediaType.TEXT_XML, xmlData, 204);
    }

    @Test
    public void postConfigDataViaUrlTest202() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(null);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + JSON, jsonData, 202);
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + XML, xmlData, 202);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_JSON, jsonData, 202);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_XML, xmlData, 202);
        postDataViaUrlTest("/config/", urlPath, MediaType.TEXT_XML, xmlData, 202);
    }

    @Test
    public void postConfigDataViaUrlTest500() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + JSON, jsonData, 500);
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + XML, xmlData, 500);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_JSON, jsonData, 500);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_XML, xmlData, 500);
        postDataViaUrlTest("/config/", urlPath, MediaType.TEXT_XML, xmlData, 500);
    }

    @Test
    public void postDatastoreDataViaUrlTest204() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + JSON, jsonData, 204);
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + XML, xmlData, 204);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_JSON, jsonData, 204);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_XML, xmlData, 204);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.TEXT_XML, xmlData, 204);
    }

    @Test
    public void postDatastoreDataViaUrlTest202() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(null);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + JSON, jsonData, 202);
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + XML, xmlData, 202);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_JSON, jsonData, 202);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_XML, xmlData, 202);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.TEXT_XML, xmlData, 202);
    }

    @Test
    public void postDatastoreDataViaUrlTest500() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + JSON, jsonData, 500);
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + XML, xmlData, 500);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_JSON, jsonData, 500);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_XML, xmlData, 500);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.TEXT_XML, xmlData, 500);
    }

    @Test
    public void postDataViaUrlMountPoint() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);

        MountService mockMountService = mock(MountService.class);
        SchemaContext otherSchemaContext = schemaContextTestModule;
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(
                new DummyMountInstanceImpl.Builder().setSchemaContext(otherSchemaContext).build());

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/test-module:cont/cont1");
        Response response = target(uri).request(Draft02.MediaTypes.DATA + XML).post(
                entity(xmlData2, Draft02.MediaTypes.DATA + XML));
        // 204 code is returned when COMMITED transaction status is put as input
        // to mock method
        assertEquals(204, response.getStatus());
    }

    private void postDataViaUrlTest(String urlPrefix, String urlPath, String mediaType, String data, int responseStatus)
            throws UnsupportedEncodingException {
        String url = createUri(urlPrefix, urlPath);
        Response response = target(url).request(mediaType).post(entity(data, mediaType));
        assertEquals(responseStatus, response.getStatus());
    }

    private void mockCommitConfigurationDataPostMethod(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = null;
        if (statusName != null) {
            dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        } else {
            dummyFuture = DummyFuture.builder().build();
        }

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);
    }

    private static CompositeNodeWrapper prepareCnSnRpcOutput() throws URISyntaxException {
        CompositeNodeWrapper cnSnDataOutput = new CompositeNodeWrapper(new URI("test:module"), "output");
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("test:module"), "cont-output");
        cnSnDataOutput.addValue(cont);
        cnSnDataOutput.unwrap();
        return cnSnDataOutput;
    }

    private void mockInvokeRpc(CompositeNode compositeNode, boolean sucessful) {
        RpcResult<CompositeNode> rpcResult = new DummyRpcResult.Builder<CompositeNode>().result(compositeNode)
                .isSuccessful(sucessful).build();
        when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenReturn(rpcResult);
    }

    private void postOperationsDataViaUrl(String mediaType, CompositeNode cnSnDataOut, String dataIn, int statusCode)
            throws FileNotFoundException, UnsupportedEncodingException {
        String url = createUri("/operations/", "test-module:rpc-test");
        Response response = null;
        switch (statusCode) {
        case 200:
            mockInvokeRpc(cnSnDataOut, true);
            break;
        case 204:
            mockInvokeRpc(null, true);
            break;
        case 500:
            mockInvokeRpc(null, false);
            break;
        case 400:
            response = target(url).request(mediaType).post(Entity.entity("{}", mediaType));
            break;
        case 404:
            url = createUri("/operations/", "test-module:rpc-wrongtest");
            break;
        }
        response = response == null ? target(url).request(mediaType).post(Entity.entity(dataIn, mediaType)) : response;
        assertEquals(statusCode, response.getStatus());
    }

    private static void loadData() throws IOException, URISyntaxException {

        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces_absolute_path.xml");
        xmlDataAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        String jsonPath = RestconfImplTest.class.getResource("/parts/ietf-interfaces_interfaces.json").getPath();
        jsonData = TestUtils.loadTextFile(jsonPath);

        String jsonFullPath = RestconfImplTest.class
                .getResource("/parts/ietf-interfaces_interfaces_absolute_path.json").getPath();
        jsonDataAbsolutePath = TestUtils.loadTextFile(jsonFullPath);

        String xmlPathRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.xml")
                .getPath();
        xmlDataRpcInput = TestUtils.loadTextFile(xmlPathRpcInput);
        cnSnDataOutput = prepareCnSnRpcOutput();

        String jsonPathToRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.json")
                .getPath();
        jsonDataRpcInput = TestUtils.loadTextFile(jsonPathToRpcInput);

        String data2Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data2.xml").getPath();
        xmlData2 = TestUtils.loadTextFile(data2Input);

    }
}
