package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class CodecsExceptionsCatchingTest extends JerseyTest {

    private static RestconfImpl restConf;
    private static ControllerContext controllerContext = ControllerContext.getInstance();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        restConf = RestconfImpl.getInstance();
        controllerContext = ControllerContext.getInstance();
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/decoding-exception/yang");
        controllerContext.setGlobalSchema(schemaContext);
        restConf.setControllerContext(controllerContext);
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restConf, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void StringToNumberConversionError() {
        Response response = target("/config/number:cont").request(MediaType.APPLICATION_XML).put(
                Entity.entity("<cont xmlns=\"number\"><lf>3f</lf></cont>", MediaType.APPLICATION_XML));
        String exceptionMessage = response.readEntity(String.class);
        assertTrue(exceptionMessage.contains("Incorrect lexical representation of Integer value: 3f"));
    }
}