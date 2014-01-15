package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
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
import java.util.logging.Level;

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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestGetOperationTest extends JerseyTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;

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
    }

    @Before
    public void logs() {
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
     * Tests {@link RestconfImpl#readData() readAllData()} method of
     * RestconfImpl with url {@code "/datastore/ identifier}"}. Status codes 200
     * is tested.
     */
    @Test
    public void getDatastoreDataViaUrlTest200() throws FileNotFoundException, UnsupportedEncodingException {
        mockReadOperationalDataMethod();
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + JSON, 200);
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + XML, 200);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_JSON, 200);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_XML, 200);
        getDataWithUrl("/datastore/", MediaType.TEXT_XML, 200);
    }

    /**
     * Tests {@link RestconfImpl#readData() readAllData()} method of
     * RestconfImpl with url {@code "/datastore/ identifier}"}. Status codes 400
     * is tested.
     */
    @Test
    public void getDatastoreDataViaUrlTest400() throws FileNotFoundException, UnsupportedEncodingException {
        mockReadOperationalDataMethod();
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + JSON, 400);
        getDataWithUrl("/datastore/", Draft01.MediaTypes.DATA + XML, 400);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_JSON, 400);
        getDataWithUrl("/datastore/", MediaType.APPLICATION_XML, 400);
        getDataWithUrl("/datastore/", MediaType.TEXT_XML, 400);
    }

    /**
     * Tests {@link RestconfImpl#readOperationalData(String)
     * readOperationalData(String)} method of RestconfImpl with url
     * {@code "/operational/...identifier..."}. Status codes 200 is tested.
     */
    @Test
    public void getOperationalDataViaUrl200() throws UnsupportedEncodingException {
        mockReadOperationalDataMethod();
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + JSON, 200);
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + XML, 200);
        getDataWithUrl("/operational/", MediaType.APPLICATION_JSON, 200);
        getDataWithUrl("/operational/", MediaType.APPLICATION_XML, 200);
        getDataWithUrl("/operational/", MediaType.TEXT_XML, 200);
    }

    /**
     * Tests {@link RestconfImpl#readOperationalData(String)
     * readOperationalData(String)} method of RestconfImpl with url
     * {@code "/operational/...identifier..."}. Status codes 400 is tested.
     */
    @Test
    public void getOperationalDataViaUrl400() throws UnsupportedEncodingException {
        mockReadOperationalDataMethod();
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + JSON, 400);
        getDataWithUrl("/operational/", Draft02.MediaTypes.DATA + XML, 400);
        getDataWithUrl("/operational/", MediaType.APPLICATION_JSON, 400);
        getDataWithUrl("/operational/", MediaType.APPLICATION_XML, 400);
        getDataWithUrl("/operational/", MediaType.TEXT_XML, 400);
    }

    /**
     * Tests {@link RestconfImpl#readOperationalData
     * #readConfigurationData(String) readConfigurationData(String)} method of
     * RestconfImpl with url {@code "/config/...identifier..."}. Status codes
     * 200 is tested.
     */
    @Test
    public void getConfigDataViaUrl200() throws UnsupportedEncodingException {
        mockReadConfigurationDataMethod();
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + JSON, 200);
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + XML, 200);
        getDataWithUrl("/config/", MediaType.APPLICATION_JSON, 200);
        getDataWithUrl("/config/", MediaType.APPLICATION_XML, 200);
        getDataWithUrl("/config/", MediaType.TEXT_XML, 200);
    }

    /**
     * Tests {@link RestconfImpl#readOperationalData
     * #readConfigurationData(String) readConfigurationData(String)} method of
     * RestconfImpl with url {@code "/config/...identifier..."}. Status codes
     * 400 is tested.
     */
    @Test
    public void getConfigDataViaUrl400() throws UnsupportedEncodingException {
        mockReadConfigurationDataMethod();
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + JSON, 400);
        getDataWithUrl("/config/", Draft02.MediaTypes.DATA + XML, 400);
        getDataWithUrl("/config/", MediaType.APPLICATION_JSON, 400);
        getDataWithUrl("/config/", MediaType.APPLICATION_XML, 400);
        getDataWithUrl("/config/", MediaType.TEXT_XML, 400);
    }

    /**
     * Tests {@link RestconfImpl#readAllData() readAllData()} method of
     * RestconfImpl with url {@code "/datastore"}. Currently the method isn't
     * supported so it returns 500
     */
    @Test
    public void getDatastoreDataAllTest500() throws UnsupportedEncodingException {
        getDatastoreAllDataTest(Draft01.MediaTypes.DATASTORE + XML);
        getDatastoreAllDataTest(Draft01.MediaTypes.DATASTORE + JSON);
    }

    /**
     * 
     * Tests {@link RestconfImpl#getModules getModules} method of RestconfImpl
     * with uri {@code "/modules"}. Currently the method isn't supported so it
     * returns 500
     */
    @Test
    public void getModulesDataTest500() throws UnsupportedEncodingException {
        getModulesDataTest(Draft01.MediaTypes.API + JSON);
        getModulesDataTest(Draft01.MediaTypes.API + XML);
        getModulesDataTest(Draft02.MediaTypes.API + JSON);
        getModulesDataTest(Draft02.MediaTypes.API + XML);
    }

    /**
     * Test of request for not existing data. Returning status code 404
     */
    @Test
    public void getDataWithUrlNoExistingDataTest404() throws UnsupportedEncodingException, URISyntaxException {
        String uri = createUri("/datastore/", "ietf-interfaces:interfaces/interface/eth0");

        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(null);

        Response response = target(uri).request(Draft01.MediaTypes.DATA + RestconfService.XML).get();
        assertEquals(404, response.getStatus());
    }

    /**
     * MountPoint test. URI represents mount point.
     */
    @Test
    public void getDataWithUrlMountPoint() throws UnsupportedEncodingException, FileNotFoundException,
            URISyntaxException {
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(
                prepareCnDataForMountPointTest());

        MountService mockMountService = mock(MountService.class);

        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(
                new DummyMountInstanceImpl.Builder().setSchemaContext(schemaContextTestModule).build());

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/0/test-module:cont/cont1");
        Response response = target(uri).request(Draft02.MediaTypes.DATA + XML).get();
        assertEquals(200, response.getStatus());
    }

    private void getDataWithUrl(String mediaTypePrefix, String mediaType, int statusCode)
            throws UnsupportedEncodingException {
        String uri = null;
        switch (statusCode) {
        case 400:
            uri = createUri(mediaTypePrefix, "wrong-module:interfaces/interface/eth0");
            break;
        case 200:
            uri = createUri(mediaTypePrefix, "ietf-interfaces:interfaces/interface/eth0");
            break;
        }
        Response response = target(uri).request(mediaType).get();
        assertEquals("Status is incorrect for media type " + mediaType + ".", statusCode, response.getStatus());

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

    private void mockReadOperationalDataMethod() {
        CompositeNode loadedCompositeNode = prepareCompositeNodeWithIetfInterfacesInterfacesData();
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);
    }

    private void mockReadConfigurationDataMethod() {
        CompositeNode loadedCompositeNode = prepareCompositeNodeWithIetfInterfacesInterfacesData();
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);
    }
}
