package org.opendaylight.controller.sal.restconf.impl.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;

import com.google.common.base.Charsets;

public class RestConfigDataTest extends JerseyTest {

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static MountService mountService;
    private static SchemaContext schemaContext;

    private static final MediaType MEDIA_TYPE_XML_DRAFT02 = new MediaType("application", "yang.data+xml");

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> modules = TestUtils.loadModulesFrom("/test-config-data/yang1");
        schemaContext = TestUtils.loadSchemaContext(modules);
        initMocking();
    }
    
    private static void initMocking() {
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        mountService = mock(MountService.class);
        controllerContext.setMountService(mountService);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    @Test
    public void createConfigurationDataTest() throws UnsupportedEncodingException, ParseException {
        initMocking();
        String URI_1 = createUri("/config", "");
        String URI_2 = createUri("/config/", "");
        String URI_3 = createUri("/config/", "test-interface:interfaces/");
        String URI_4 = createUri("/config/", "test-interface:interfaces/");
        String URI_5 = createUri("/config/", "test-interface:interfaces/test-interface2:class");

        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);

        ArgumentCaptor<InstanceIdentifier> instanceIdCaptor = ArgumentCaptor.forClass(InstanceIdentifier.class);
        ArgumentCaptor<CompositeNode> compNodeCaptor = ArgumentCaptor.forClass(CompositeNode.class);

        // Test URI_1
        Entity<String> entity = createEntity("/test-config-data/xml/test-interface.xml");
        Response response = target(URI_1).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(204, response.getStatus());
        verify(brokerFacade).commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        String identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces]";
        assertEquals("Bad format URI", identifier, instanceIdCaptor.getValue().getPath().toString());

        // Test URI_2
        response = target(URI_2).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(204, response.getStatus());
        verify(brokerFacade, times(2))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        assertEquals("Bad format URI", identifier, instanceIdCaptor.getValue().getPath().toString());

        // Test URI_3
        entity = createEntity("/test-config-data/xml/test-interface2.xml");
        response = target(URI_3).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(204, response.getStatus());
        verify(brokerFacade, times(3))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());

        identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces, (urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interface[{(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)name=eth0}]]";
        assertEquals("Bad format URI", identifier, instanceIdCaptor.getValue().getPath().toString());

        // Test URI_4
        Set<Module> modules2 = TestUtils.loadModulesFrom("/test-config-data/yang2");
        SchemaContext schemaContext2 = TestUtils.loadSchemaContext(modules2);
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContext2);
        when(mountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        entity = createEntity("/test-config-data/xml/test-interface3.xml");
        response = target(URI_4).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(204, response.getStatus());
        verify(brokerFacade, times(4))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces, (urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)class]";
        assertEquals("Bad format URI", identifier, instanceIdCaptor.getValue().getPath().toString());

        // Test URI_5
        response = target(URI_5).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(204, response.getStatus());
        verify(brokerFacade, times(5))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces, (urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)class, (urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)class]";
        assertEquals("Bad format URI", identifier, instanceIdCaptor.getValue().getPath().toString());
    }
    
    @Test
    public void testExistingData() throws UnsupportedEncodingException {
        initMocking();
        String URI_1 = createUri("/config", "");
        String URI_2 = createUri("/config/", "");
        String URI_3 = createUri("/config/", "test-interface:interfaces/");
        String URI_4 = createUri("/config/", "test-interface:interfaces/");
        String URI_5 = createUri("/config/", "test-interface:interfaces/test-interface2:class");

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(null);

        // Test URI_1
        Entity<String> entity = createEntity("/test-config-data/xml/test-interface.xml");
        Response response = target(URI_1).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(202, response.getStatus());

        // Test URI_2
        response = target(URI_2).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(202, response.getStatus());

        // Test URI_3
        entity = createEntity("/test-config-data/xml/test-interface2.xml");
        response = target(URI_3).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(202, response.getStatus());

        // Test URI_4
        Set<Module> modules2 = TestUtils.loadModulesFrom("/test-config-data/yang2");
        SchemaContext schemaContext2 = TestUtils.loadSchemaContext(modules2);
        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContext2);
        when(mountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        entity = createEntity("/test-config-data/xml/test-interface3.xml");
        response = target(URI_4).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(202, response.getStatus());

        // Test URI_5
        response = target(URI_5).request(MEDIA_TYPE_XML_DRAFT02).post(entity);
        assertEquals(202, response.getStatus());
    }

    private String createUri(String prefix, String encodedPart) throws UnsupportedEncodingException {
        return URI.create(prefix + URLEncoder.encode(encodedPart, Charsets.US_ASCII.name()).toString()).toASCIIString();
    }

    private Entity<String> createEntity(final String relativePathToXml) {
        InputStream inputStream = XmlMapper.class.getResourceAsStream(relativePathToXml);
        String xml = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(inputStream));
        Entity<String> entity = Entity.entity(xml, MEDIA_TYPE_XML_DRAFT02);

        return entity;
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.RECORD_LOG_LEVEL);
        set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                XmlToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }
}
