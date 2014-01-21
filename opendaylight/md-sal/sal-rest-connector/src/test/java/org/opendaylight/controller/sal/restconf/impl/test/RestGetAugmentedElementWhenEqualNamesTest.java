package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestGetAugmentedElementWhenEqualNamesTest {

    @Ignore
    @Test
    public void getDataWithUrlMountPoint() throws UnsupportedEncodingException, URISyntaxException,
            FileNotFoundException {
        boolean exceptionCaught = false;

        SchemaContext schemaContextTestModule = TestUtils.loadSchemaContext("/common/augment/yang");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextTestModule);

        try {
            InstanceIdWithSchemaNode instanceIdentifierA = controllerContext
                    .toInstanceIdentifier("main:cont/augment-main-a:cont1");
            InstanceIdWithSchemaNode instanceIdentifierB = controllerContext
                    .toInstanceIdentifier("main:cont/augment-main-b:cont1");

            assertEquals("ns:augment:main:a", instanceIdentifierA.getSchemaNode().getQName().getNamespace().toString());
            assertEquals("ns:augment:main:b", instanceIdentifierB.getSchemaNode().getQName().getNamespace());
        } catch (ResponseException e) {
            exceptionCaught = true;
        }

        assertFalse(exceptionCaught);

    }

}
