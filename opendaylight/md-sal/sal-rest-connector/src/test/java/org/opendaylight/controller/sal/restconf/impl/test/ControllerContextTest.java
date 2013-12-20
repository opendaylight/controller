package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ControllerContextTest {

    private static final ControllerContext controllerContext = ControllerContext.getInstance();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void testToInstanceIdentifierList() throws FileNotFoundException {
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext
                .toInstanceIdentifier("simple-nodes:userWithoutClass/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "userWithoutClass");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:userWithoutClass/foo/full-name");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "full-name");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user/foo/boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user//boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:users/user/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user/null/boo");
        assertNull(instanceIdentifier);

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:user/foo");
        assertNull(instanceIdentifier);

    }

    @Test
    public void testToInstanceIdentifierMountPoint() throws FileNotFoundException {
        try {
            String mountPointPath = "simple-nodes:user/foo/boo";
            String nestedPath = "simple-nodes:user/foo/boo/simple-nodes:users";
            InstanceIdWithSchemaNode mountInstanceIdentifier = controllerContext.toInstanceIdentifier(mountPointPath);
            assertEquals("user", mountInstanceIdentifier.getSchemaNode().getQName().getLocalName());

            MountInstance mountInstance = mock(MountInstance.class);
            MountService mountService = mock(MountService.class);

            controllerContext.setMountService(mountService);
            // when(mountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(null);

            when(mountService.getMountPoint(eq(mountInstanceIdentifier.getInstanceIdentifier()))).thenReturn(
                    mountInstance);

            when(mountInstance.getSchemaContext()).thenReturn(controllerContext.getGlobalSchema());

            InstanceIdWithSchemaNode mountedInstanceIdentifier = controllerContext.toInstanceIdentifier(nestedPath);
            assertEquals("users", mountedInstanceIdentifier.getSchemaNode().getQName().getLocalName());

            mountedInstanceIdentifier = controllerContext.toInstanceIdentifier(mountPointPath + "/" + mountPointPath);
            assertEquals("user", mountedInstanceIdentifier.getSchemaNode().getQName().getLocalName());

            mountedInstanceIdentifier = controllerContext
                    .toInstanceIdentifier("simple-nodes:user/foo/var/simple-nodes:users");
            assertNull(mountedInstanceIdentifier);

        } finally {
            controllerContext.setMountService(null);
        }

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
        InstanceIdWithSchemaNode instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:food/beer");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "beer");

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:food/snack");
        assertNull(instanceIdentifier);

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:food/sports-arena");
        assertNull(instanceIdentifier);

        instanceIdentifier = controllerContext.toInstanceIdentifier("simple-nodes:food/snack/sports-arena");
        assertNull(instanceIdentifier);

    }

}
