package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
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

    private static String xmlDataAbsolutePath;
    private static String xmlDataRpcInput;
    private static CompositeNodeWrapper cnSnDataOutput;
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

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
//        enable(TestProperties.LOG_TRAFFIC);
//        enable(TestProperties.DUMP_ENTITY);
//        enable(TestProperties.RECORD_LOG_LEVEL);
//        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void postOperationsStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextTestModule);
        mockInvokeRpc(cnSnDataOutput, true);
        String uri = createUri("/operations/", "test-module:rpc-test");
        assertEquals(200, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
        
        mockInvokeRpc(null, true);
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
        
        mockInvokeRpc(null, false);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
        
        uri = createUri("/operations/", "test-module:rpc-wrongtest");
        assertEquals(404, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
    }

    @Test
    public void postConfigOnlyStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String uri = createUri("/config", "");
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(null);
        assertEquals(202, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
    }

    @Test
    public void postConfigStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String uri = createUri("/config/", "ietf-interfaces:interfaces");
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(null);
        assertEquals(202, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
    }

    @Test
    public void postDatastoreStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String uri = createUri("/datastore/", "ietf-interfaces:interfaces");
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(null);
        assertEquals(202, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
        
        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
    }

    @Test
    public void postDataViaUrlMountPoint() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(TransactionStatus.COMMITED)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitConfigurationDataPostBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont/cont1");
        assertEquals(204, post(uri, Draft02.MediaTypes.DATA + XML, xmlData2));
    }
    
    private void mockInvokeRpc(CompositeNode result, boolean sucessful) {
        RpcResult<CompositeNode> rpcResult = new DummyRpcResult.Builder<CompositeNode>().result(result)
                .isSuccessful(sucessful).build();
        when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenReturn(rpcResult);
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
    
    private int post(String uri, String mediaType, String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType)).getStatus();
    }

    private static void loadData() throws IOException, URISyntaxException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces_absolute_path.xml");
        xmlDataAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        String xmlPathRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.xml")
                .getPath();
        xmlDataRpcInput = TestUtils.loadTextFile(xmlPathRpcInput);
        cnSnDataOutput = prepareCnSnRpcOutput();
        String data2Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data2.xml").getPath();
        xmlData2 = TestUtils.loadTextFile(data2Input);
    }

    private static CompositeNodeWrapper prepareCnSnRpcOutput() throws URISyntaxException {
        CompositeNodeWrapper cnSnDataOutput = new CompositeNodeWrapper(new URI("test:module"), "output");
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("test:module"), "cont-output");
        cnSnDataOutput.addValue(cont);
        cnSnDataOutput.unwrap();
        return cnSnDataOutput;
    }
}
