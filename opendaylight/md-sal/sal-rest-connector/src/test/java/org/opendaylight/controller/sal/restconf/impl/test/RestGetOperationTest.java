package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.JSON;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.mount.MountService;
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
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestGetOperationTest extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;

    @BeforeClass
    public static void init() {
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
    public void logs() {
        List<LogRecord> loggedRecords = getLoggedRecords();
        for (LogRecord l : loggedRecords) {
            System.out.println(l.getMessage());
        }
    }

    @Test
    public void getDataViaUrlTest() throws FileNotFoundException, UnsupportedEncodingException {
        CompositeNode loadedCompositeNode = prepareCompositeNodeWithIetfInterfacesInterfacesData();
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);

        getDatastoreDataViaUrlTest();
        getConfigDataViaUrl();
        getOperationalDataViaUrl();
    }

    @Test
    public void getDatastoreDataAllTest() throws UnsupportedEncodingException {
        getDatastoreAllDataTest(Draft01.MediaTypes.DATASTORE + XML);
        getDatastoreAllDataTest(Draft01.MediaTypes.DATASTORE + JSON);
    }

    @Test
    public void getModulesDataTest() throws UnsupportedEncodingException {
        getModulesDataTest(Draft01.MediaTypes.API + JSON);
        getModulesDataTest(Draft01.MediaTypes.API + XML);
        getModulesDataTest(Draft02.MediaTypes.API + JSON);
        getModulesDataTest(Draft02.MediaTypes.API + XML);
    }

    @Test
    public void getDataWithUrlDataNoExistTest() throws UnsupportedEncodingException, URISyntaxException {
        String uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");

        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(null);

        Response response = target(uri).request(Draft01.MediaTypes.DATA + RestconfService.XML).get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void getDataWithUrlMountPoint() throws UnsupportedEncodingException, FileNotFoundException,
            URISyntaxException {
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(
                prepareCnDataForMountPointTest());

        MountService mockMountService = mock(MountService.class);
        SchemaContext otherSchemaContext = TestUtils.loadSchemaContext("/full-versions/yangs2");
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(
                new DummyMountInstanceImpl.Builder().setSchemaContext(otherSchemaContext).build());

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/test-module:cont/cont1");
        Response response = target(uri).request(Draft02.MediaTypes.DATA + XML).get();
        assertEquals(200, response.getStatus());
    }

    private void getDatastoreDataViaUrlTest() throws FileNotFoundException, UnsupportedEncodingException {
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + JSON);
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + XML);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_JSON);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_XML);
        getDataWithUrl("/datastore/", MediaType.TEXT_XML);
    }

    private void getOperationalDataViaUrl() throws UnsupportedEncodingException {
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + JSON);
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + XML);
        getDataWithUrl("/operational/", MediaType.APPLICATION_JSON);
        getDataWithUrl("/operational/", MediaType.APPLICATION_XML);
        getDataWithUrl("/operational/", MediaType.TEXT_XML);
    }

    private void getConfigDataViaUrl() throws UnsupportedEncodingException {
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + JSON);
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + XML);
        getDataWithUrl("/config/", MediaType.APPLICATION_JSON);
        getDataWithUrl("/config/", MediaType.APPLICATION_XML);
        getDataWithUrl("/config/", MediaType.TEXT_XML);
    }

    private void getDataWithUrl(String mediaTypePrefix, String mediaType) throws UnsupportedEncodingException {
        String uri = createUri(mediaTypePrefix, "wrong-module:interfaces/interface/eth0");
        Response response = target(uri).request(mediaType).get();
        assertEquals("Status is incorrect for media type " + mediaType + ".", 400, response.getStatus());

        uri = createUri(mediaTypePrefix, "ietf-interfaces:interfaces/interface/eth0");
        response = target(uri).request(mediaType).get();
        assertEquals("Status is incorrect for media type " + mediaType + ".", 200, response.getStatus());
    }

    private void getModulesDataTest(String mediaType) throws UnsupportedEncodingException {
        String uri = createUri("/modules", "");
        Response response = target(uri).request(mediaType).get();

        assertEquals("Status is incorrect for media type " + mediaType + ".", 500, response.getStatus());
    }

    private void getDatastoreAllDataTest(String mediaType) throws UnsupportedEncodingException {
        String uri = createUri("/datastore", "");
        Response response = target(uri).request(mediaType).get();

        assertEquals(500, response.getStatus());
    }

    private CompositeNode prepareCnDataForMountPointTest() throws URISyntaxException {
        CompositeNodeWrapper cont1 = new CompositeNodeWrapper(new URI("test:module"), "cont1");
        SimpleNodeWrapper lf11 = new SimpleNodeWrapper(new URI("test:module"), "lf11", "lf11 value");
        cont1.addValue(lf11);
        return cont1.unwrap();
    }

    private CompositeNode prepareCompositeNodeWithIetfInterfacesInterfacesData() {
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
