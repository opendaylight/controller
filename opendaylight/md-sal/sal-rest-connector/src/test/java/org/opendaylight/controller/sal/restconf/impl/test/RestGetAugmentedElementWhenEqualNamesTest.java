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
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class RestGetAugmentedElementWhenEqualNamesTest {

    private static ControllerContext controllerContext = ControllerContext.getInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void init() throws FileNotFoundException, URISyntaxException, ReactorException {
        final SchemaContext schemaContextTestModule = TestUtils.loadSchemaContext("/common/augment/yang");
        controllerContext.setSchemas(schemaContextTestModule);
    }

    @Test
    public void augmentedNodesInUri() {
        InstanceIdentifierContext<?> iiWithData = controllerContext.toInstanceIdentifier("main:cont/augment-main-a:cont1");
        assertEquals("ns:augment:main:a", iiWithData.getSchemaNode().getQName().getNamespace().toString());
        iiWithData = controllerContext.toInstanceIdentifier("main:cont/augment-main-b:cont1");
        assertEquals("ns:augment:main:b", iiWithData.getSchemaNode().getQName().getNamespace().toString());
    }

    @Test
    public void nodeWithoutNamespaceHasMoreAugments() {
        try {
            controllerContext.toInstanceIdentifier("main:cont/cont1");
            fail("Expected exception");
        } catch (final RestconfDocumentedException e) {
            assertTrue(e.getErrors().get(0).getErrorMessage().contains("is added as augment from more than one module"));
        }
    }
}
