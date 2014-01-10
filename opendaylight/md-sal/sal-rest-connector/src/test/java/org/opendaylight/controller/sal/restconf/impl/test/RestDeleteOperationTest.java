package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.createUri;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestDeleteOperationTest extends JerseyTest {

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static final MediaType MEDIA_TYPE_DRAFT02 = new MediaType("application", "yang.data+xml");

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/test-config-data/yang1");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
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
                XmlToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void testDeleteConfigurationData() throws UnsupportedEncodingException, FileNotFoundException {
        String uri2 = createUri("/config/", "test-interface:interfaces");

        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitConfigurationDataDelete(any(InstanceIdentifier.class))).thenReturn(dummyFuture);

        Response response = target(uri2).request(MEDIA_TYPE_DRAFT02).delete();
        assertEquals(200, response.getStatus());

        rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(TransactionStatus.FAILED).build();
        dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();

        when(brokerFacade.commitConfigurationDataDelete(any(InstanceIdentifier.class))).thenReturn(dummyFuture);

        response = target(uri2).request(MEDIA_TYPE_DRAFT02).delete();
        assertEquals(500, response.getStatus());
    }

}
