/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestGetAugmentedElementWhenEqualNamesTest {
    
    private static ControllerContext controllerContext = ControllerContext.getInstance();
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @BeforeClass
    public static void init() throws FileNotFoundException {
        SchemaContext schemaContextTestModule = TestUtils.loadSchemaContext("/common/augment/yang");
        controllerContext.setSchemas(schemaContextTestModule);
    }

    @Test
    public void augmentedNodesInUri() {
        InstanceIdWithSchemaNode iiWithData = controllerContext.toInstanceIdentifier("main:cont/augment-main-a:cont1");
        assertEquals("ns:augment:main:a", iiWithData.getSchemaNode().getQName().getNamespace().toString());
        iiWithData = controllerContext.toInstanceIdentifier("main:cont/augment-main-b:cont1");
        assertEquals("ns:augment:main:b", iiWithData.getSchemaNode().getQName().getNamespace().toString());
    }
    
    @Test
    public void nodeWithoutNamespaceHasMoreAugments() {
        boolean exceptionCaught = false;
        try {
            controllerContext.toInstanceIdentifier("main:cont/cont1");
        } catch (ResponseException e) {
            assertTrue(((String) e.getResponse().getEntity()).contains("is added as augment from more than one module"));
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

}
