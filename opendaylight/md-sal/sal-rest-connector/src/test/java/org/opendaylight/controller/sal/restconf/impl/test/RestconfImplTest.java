package org.opendaylight.controller.sal.restconf.impl.test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestconfImplTest {

    private static final RestconfImpl restconfImpl = RestconfImpl.getInstance();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModules(RestconfImplTest.class.getResource("/full-versions/yangs").getPath());
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        restconfImpl.setControllerContext(controllerContext);
    }

    @Test
    public void testExample() throws FileNotFoundException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        CompositeNode loadedCompositeNode = TestUtils.loadCompositeNode(xmlStream);
        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class))).thenReturn(loadedCompositeNode);
        assertEquals(loadedCompositeNode, brokerFacade.readOperationalData(null));
    }

}
