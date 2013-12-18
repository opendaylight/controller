package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Charsets;

public class ReadConfAndOperDataTest extends JerseyTest {

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static final MediaType MEDIA_TYPE_DRAFT02 = new MediaType("application", "yang.data+xml");

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModules(RestconfImplTest.class.getResource("/full-versions/yangs")
                .getPath());
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
    public void testReadConfigurationData() throws UnsupportedEncodingException, FileNotFoundException {

        String uri = createUri("/config/", "ietf-interfaces:interfaces/interface/eth0");

        CompositeNode loadedCompositeNode = TestUtils.loadCompositeNodeWithXmlTreeBuilder("/parts/ietf-interfaces_interfaces.xml");
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);

        Response response = target(uri).request(MEDIA_TYPE_DRAFT02).get();
        assertEquals(200, response.getStatus());
        
        uri = createUri("/config/", "ietf-interfaces:interfaces/interface/example");
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(null);
        
        response = target(uri).request(MEDIA_TYPE_DRAFT02).get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testReadOperationalData() throws UnsupportedEncodingException, FileNotFoundException {
        String uri = createUri("/operational/", "ietf-interfaces:interfaces/interface/eth0");

        CompositeNode loadedCompositeNode = TestUtils.loadCompositeNodeWithXmlTreeBuilder("/parts/ietf-interfaces_interfaces.xml");
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);

        Response response = target(uri).request(MEDIA_TYPE_DRAFT02).get();
        assertEquals(200, response.getStatus());
        
        uri = createUri("/config/", "ietf-interfaces:interfaces/interface/example");
        when(brokerFacade.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(null);
        
        response = target(uri).request(MEDIA_TYPE_DRAFT02).get();
        assertEquals(404, response.getStatus());
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
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                XmlToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }
}
