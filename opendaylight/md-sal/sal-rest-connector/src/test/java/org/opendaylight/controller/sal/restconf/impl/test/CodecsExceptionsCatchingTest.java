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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.impl.RestSchemaContextImpl;
import org.opendaylight.controller.rest.errors.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.rest.providers.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.providers.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.rest.providers.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.rest.providers.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.services.RestconfServiceData;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class CodecsExceptionsCatchingTest extends JerseyTest {

    private static RestconfServiceData restDataServ;
    private static RestSchemaContextImpl controllerContext = new RestSchemaContextImpl();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        final RestBrokerFacade broker = Mockito.mock(RestBrokerFacade.class);
        final SchemaContext schemaContext = TestUtils.loadSchemaContext("/decoding-exception/yang");
        controllerContext.setGlobalSchema(schemaContext);
        restDataServ = new RestconfServiceDataImpl(broker, controllerContext);
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restDataServ, new NormalizedNodeJsonBodyWriter(),
                new NormalizedNodeXmlBodyWriter(), new XmlNormalizedNodeBodyReader(controllerContext),
                new JsonNormalizedNodeBodyReader(controllerContext));
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    @Test
    @Ignore // TODO RestconfDocumentedExceptionMapper needs be fixed before
    public void StringToNumberConversionError() {
        final Response response = target("/config/number:cont").request(MediaType.APPLICATION_XML).put(
                Entity.entity("<cont xmlns=\"number\"><lf>3f</lf></cont>", MediaType.APPLICATION_XML));
        final String exceptionMessage = response.readEntity(String.class);
        assertTrue(exceptionMessage.contains("invalid-value"));
    }
}