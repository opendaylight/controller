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
import java.net.URI;
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
import org.opendaylight.controller.sal.rest.api.Draft01;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
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
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPostOperationTest extends JerseyTest {

    String xmlData;
    String xmlDataAbsolutePath;
    String jsonData;
    String jsonDataAbsolutePath;

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = ControllerContext.getInstance();
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
        prepareEntities();

    }

    @Test
    public void postOperationalDataViaUrl() throws URISyntaxException, IOException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/yangs2");
        controllerContext.setSchemas(schemaContext);

        String xmlPathRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.xml")
                .getPath();
        String xmlDataRpcInput = TestUtils.loadTextFile(xmlPathRpcInput);
        CompositeNodeWrapper cnSnDataOutput = prepareCnSnRpcOutput();

        String jsonPathToRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.json")
                .getPath();
        String jsonDataRpcInput = TestUtils.loadTextFile(jsonPathToRpcInput);
        postOperationalDataViaUrl(Draft02.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput);
        postOperationalDataViaUrl(Draft01.MediaTypes.DATA + JSON, cnSnDataOutput, jsonDataRpcInput);
        postOperationalDataViaUrl(MediaType.APPLICATION_JSON, cnSnDataOutput, jsonDataRpcInput);

        postOperationalDataViaUrl(Draft01.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput);
        postOperationalDataViaUrl(Draft02.MediaTypes.DATA + XML, cnSnDataOutput, xmlDataRpcInput);
        postOperationalDataViaUrl(MediaType.APPLICATION_XML, cnSnDataOutput, xmlDataRpcInput);
        postOperationalDataViaUrl(MediaType.TEXT_XML, cnSnDataOutput, xmlDataRpcInput);
    }

    private CompositeNodeWrapper prepareCnSnRpcOutput() throws URISyntaxException {
        CompositeNodeWrapper cnSnDataOutput = new CompositeNodeWrapper(new URI("test:module"), "output");
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("test:module"), "cont-output");
        cnSnDataOutput.addValue(cont);
        cnSnDataOutput.unwrap();
        return cnSnDataOutput;
    }

    private void postOperationalDataViaUrl(String mediaType, CompositeNode cnSnDataOut, String dataIn)
            throws FileNotFoundException, UnsupportedEncodingException {
        String uri = createUri("/operations/", "test-module:rpc-test");
        mockInvokeRpc(cnSnDataOut, true);
        Response response = target(uri).request(mediaType).post(Entity.entity(dataIn, mediaType));
        assertEquals(200, response.getStatus());

        mockInvokeRpc(null, true);
        response = target(uri).request(mediaType).post(Entity.entity(dataIn, mediaType));
        assertEquals(204, response.getStatus());

        mockInvokeRpc(null, false);
        response = target(uri).request(mediaType).post(Entity.entity(dataIn, mediaType));
        assertEquals(500, response.getStatus());

        response = target(uri).request(mediaType).post(Entity.entity("{}", mediaType));
        assertEquals(400, response.getStatus());

        uri = createUri("/operations/", "test-module:rpc-wrongtest");
        response = target(uri).request(mediaType).post(Entity.entity(dataIn, mediaType));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void postDataViaUriTest() throws UnsupportedEncodingException, FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/yangs");
        controllerContext.setSchemas(schemaContext);
        postDataViaUriReturnStatusCodeTest(TransactionStatus.COMMITED, 204);
        postDataViaUriReturnStatusCodeTest(TransactionStatus.FAILED, 500);
        postDataViaUriReturnStatusCodeTest(null, 202);
    }

    private void postDataViaUriReturnStatusCodeTest(TransactionStatus statusName, int statusCode)
            throws UnsupportedEncodingException {
        mockCommitConfigurationDataPostMethod(statusName);
        postConfigDataViaUrlTest(statusCode);
        postDatastoreDataViaUrlTest(statusCode);
        postConfigDataViaUrlConfigOnlyTest(statusCode);
    }

    private void postConfigDataViaUrlConfigOnlyTest(int responseStatus) throws UnsupportedEncodingException {
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + JSON, jsonDataAbsolutePath, responseStatus);
        postDataViaUrlTest("/config", "", Draft02.MediaTypes.DATA + XML, xmlDataAbsolutePath, responseStatus);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_JSON, jsonDataAbsolutePath, responseStatus);
        postDataViaUrlTest("/config", "", MediaType.APPLICATION_XML, xmlDataAbsolutePath, responseStatus);
        postDataViaUrlTest("/config", "", MediaType.TEXT_XML, xmlDataAbsolutePath, responseStatus);
    }

    private void postDatastoreDataViaUrlTest(int responseStatus) throws UnsupportedEncodingException {
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + JSON, jsonData, responseStatus);
        postDataViaUrlTest("/datastore/", urlPath, Draft01.MediaTypes.DATA + XML, xmlData, responseStatus);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_JSON, jsonData, responseStatus);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.APPLICATION_XML, xmlData, responseStatus);
        postDataViaUrlTest("/datastore/", urlPath, MediaType.TEXT_XML, xmlData, responseStatus);
    }

    private void postConfigDataViaUrlTest(int responseStatus) throws UnsupportedEncodingException {
        String urlPath = "ietf-interfaces:interfaces";
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + JSON, jsonData, responseStatus);
        postDataViaUrlTest("/config/", urlPath, Draft02.MediaTypes.DATA + XML, xmlData, responseStatus);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_JSON, jsonData, responseStatus);
        postDataViaUrlTest("/config/", urlPath, MediaType.APPLICATION_XML, xmlData, responseStatus);
        postDataViaUrlTest("/config/", urlPath, MediaType.TEXT_XML, xmlData, responseStatus);
    }

    private void postDataViaUrlTest(String urlPrefix, String urlPath, String mediaType, String data, int responseStatus)
            throws UnsupportedEncodingException {
        String uri = createUri(urlPrefix, urlPath);
        Response response = target(uri).request(mediaType).post(entity(data, mediaType));
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

    private void mockInvokeRpc(CompositeNode compositeNode, boolean sucessful) {
        RpcResult<CompositeNode> rpcResult = new DummyRpcResult.Builder<CompositeNode>().result(compositeNode)
                .isSuccessful(sucessful).build();
        when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenReturn(rpcResult);
    }

    private void prepareEntities() throws IOException {

        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces_absolute_path.xml");
        xmlDataAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));

        String jsonPath = RestconfImplTest.class.getResource("/parts/ietf-interfaces_interfaces.json").getPath();
        jsonData = TestUtils.loadTextFile(jsonPath);

        String jsonFullPath = RestconfImplTest.class
                .getResource("/parts/ietf-interfaces_interfaces_absolute_path.json").getPath();
        jsonDataAbsolutePath = TestUtils.loadTextFile(jsonFullPath);
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
