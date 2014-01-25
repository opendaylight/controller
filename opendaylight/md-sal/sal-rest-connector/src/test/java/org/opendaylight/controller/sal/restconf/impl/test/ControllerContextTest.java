package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ControllerContextTest {

    private static final ControllerContext controllerContext = ControllerContext.getInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext.setSchemas(schemaContext);
    }

    @After
    public void releaseMountService() {
        controllerContext.setMountService(null);
    }

    @Test
    public void testToInstanceIdentifierList() throws FileNotFoundException {
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext
                .toInstanceIdentifier("simple-nodes:userWithoutClass/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "userWithoutClass");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:userWithoutClass/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "userWithoutClass");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user/foo/boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user//boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

    }

    @Test
    public void testToInstanceIdentifierListWithNullKey() {
        exception.expect(ResponseException.class);
        exception.expectMessage("HTTP 400 Bad Request");
        controllerContext.toInstanceIdentifier("simple-nodes:user/null/boo");
    }

    @Test
    public void testToInstanceIdentifierListWithMissingKey() {
        exception.expect(ResponseException.class);
        exception.expectMessage("HTTP 400 Bad Request");
        controllerContext.toInstanceIdentifier("simple-nodes:user/foo");
    }

    @Test
    public void testToInstanceIdentifierContainer() throws FileNotFoundException {
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:users");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "users");
        assertTrue(instanceIdentifier.getSchemaNode() instanceof ContainerSchemaNode);
        assertEquals(2, ((ContainerSchemaNode) instanceIdentifier.getSchemaNode()).getChildNodes().size());
    }

    @Test
    public void testToInstanceIdentifierChoice() throws FileNotFoundException {
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext
                .toInstanceIdentifier("simple-nodes:food/nonalcoholic");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "nonalcoholic");
    }

    @Test
    public void testToInstanceIdentifierChoiceException() {
        exception.expect(ResponseException.class);
        exception.expectMessage("HTTP 400 Bad Request");
        controllerContext.toInstanceIdentifier("simple-nodes:food/snack");
    }

    @Test
    public void testToInstanceIdentifierCaseException() {
        exception.expect(ResponseException.class);
        exception.expectMessage("HTTP 400 Bad Request");
        controllerContext.toInstanceIdentifier("simple-nodes:food/sports-arena");
    }

    @Test
    public void testToInstanceIdentifierChoiceCaseException() {
        exception.expect(ResponseException.class);
        exception.expectMessage("HTTP 400 Bad Request");
        controllerContext.toInstanceIdentifier("simple-nodes:food/snack/sports-arena");
    }

}
