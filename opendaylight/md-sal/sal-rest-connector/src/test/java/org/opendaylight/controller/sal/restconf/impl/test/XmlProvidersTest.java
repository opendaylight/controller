package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.MediaTypes;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Charsets;

public class XmlProvidersTest extends JerseyTest {

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.yang.api+xml");

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModules(RestconfImplTest.class.getResource("/full-versions/yangs").getPath());
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    @Before
    public void logs() {
        List<LogRecord> loggedRecords = getLoggedRecords();
        for (LogRecord l : loggedRecords) {
            System.out.println(l.getMessage());
        }
    }

    @Test
    public void testStructuredDataToXmlProvider() throws FileNotFoundException, UnsupportedEncodingException {
        String uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");
        
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        CompositeNode loadedCompositeNode = TestUtils.loadCompositeNode(xmlStream);
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);
        
        Response response = target(uri).request(MEDIA_TYPE).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBadFormatXmlToCompositeNodeProvider() throws UnsupportedEncodingException, URISyntaxException {
        String uri = createUri("/operations/", "ietf-interfaces:interfaces/interface/eth0");
        
        Response response = target(uri).request(MediaTypes.API + RestconfService.XML).post(
                Entity.entity("<SimpleNode/>", MEDIA_TYPE));
        assertEquals(400, response.getStatus());
        
        response = target(uri).request(MediaTypes.API + RestconfService.XML).post(
                Entity.entity("<SimpleNode>", MEDIA_TYPE));
        assertEquals(400, response.getStatus());
    }
    
    @Test
    public void testXmlToCompositeNode404NotFound() throws UnsupportedEncodingException, URISyntaxException {
        String uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");
        
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(null);
        
        Response response = target(uri).request(MediaTypes.API+RestconfService.XML).get();
        assertEquals(404, response.getStatus());
    }
    
    @Test
    public void testXmlToCompositeNode400() throws UnsupportedEncodingException, URISyntaxException {
        String uri = createUri("/datastore/", "simple-nodes:user/name");
        
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(null);
        
        Response response = target(uri).request(MediaTypes.API+RestconfService.XML).get();
        assertEquals(400, response.getStatus());
    }
    
    @Test
    public void testRpcResultCommitedToStatusCodes() throws UnsupportedEncodingException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        String xml = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        Entity<String> entity = Entity.entity(xml, MEDIA_TYPE);
        RpcResult<TransactionStatus> rpcResult = DummyRpcResult.builder().result(TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitOperationalDataPut(any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);
        
        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/eth0");
        Response response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(200, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(204, response.getStatus());
        
        uri = createUri("/operational/", "ietf-interfaces:interfaces/interface/eth0");
        response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(200, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(204, response.getStatus());
        
        uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");
        response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(200, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(204, response.getStatus());
    }
    
    @Test
    public void testRpcResultOtherToStatusCodes() throws UnsupportedEncodingException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        String xml = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        Entity<String> entity = Entity.entity(xml, MEDIA_TYPE);
        RpcResult<TransactionStatus> rpcResult = DummyRpcResult.builder().result(TransactionStatus.FAILED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitOperationalDataPut(any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);
        
        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/eth0");
        Response response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(500, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(500, response.getStatus());
        
        uri = createUri("/operational/", "ietf-interfaces:interfaces/interface/eth0");
        response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(500, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(500, response.getStatus());
        
        uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");
        response = target(uri).request(MEDIA_TYPE).put(entity);
        assertEquals(500, response.getStatus());
        response = target(uri).request(MEDIA_TYPE).post(entity);
        assertEquals(500, response.getStatus());
    }
    
    private String createUri(String prefix, String encodedPart) throws UnsupportedEncodingException {
        return URI.create(prefix + URLEncoder.encode(encodedPart, Charsets.US_ASCII.name()).toString()).toASCIIString();
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.RECORD_LOG_LEVEL);
        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

}
