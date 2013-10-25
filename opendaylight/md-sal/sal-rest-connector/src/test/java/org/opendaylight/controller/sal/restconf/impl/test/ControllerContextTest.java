package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ControllerContextTest {

    private static final ControllerContext controllerContext = new ControllerContext();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModules(ControllerContextTest.class.getResource("/full-versions/yangs").getPath());
        assertEquals(4, allModules.size());
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void testToInstanceIdentifierList() throws FileNotFoundException {
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext.toInstanceIdentifier("ietf-interfaces:interfaces/interface/foo");
        DataSchemaNode schemaNode = instanceIdentifier.getSchemaNode();
        assertEquals(schemaNode.getQName().getLocalName(), "interface");
    }

}
