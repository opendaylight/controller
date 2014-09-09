/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.sal.restconf.impl.test.TestUtils.buildQName;
import static org.opendaylight.controller.sal.restconf.impl.test.TestUtils.loadSchemaContext;

import java.io.FileNotFoundException;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ControllerContextTest {

    /**
     * test of method {@link ControllerContext#getDataNodeContainerFor(YangInstanceIdentifier)
     * getDataNodeContainerFor()}
     */
    @Ignore
    @Test
    public void getDataNodeContainerForTest() {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        SchemaContext schemaControllerContextTest = null;
        try {
            schemaControllerContextTest = loadSchemaContext("/controller-context");
        } catch (FileNotFoundException e) {
            fail("Controller context test schema wasn't loaded.");
        }
        controllerContext.setSchemas(schemaControllerContextTest);
        DataNodeContainer dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier(
                "lst", "cont"));
        assertTrue(dataNodeContainer instanceof DataSchemaNode);
        final DataSchemaNode dataSchemaNode = (DataSchemaNode) dataNodeContainer;
        assertEquals("cont", dataSchemaNode.getQName().getLocalName());
        assertEquals("ns:cont:cont", dataSchemaNode.getQName().getNamespace().toString());

        // referring non existing node in direct child
        dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier("lst", "cont1",
                "cont11"));
        assertNull(dataNodeContainer);

        // referring non existing node which is also searched in indirect children
        dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier("lst", "cont11"));
        assertNull(dataNodeContainer);

    }

    private YangInstanceIdentifier prepareInstanceIdentifier(String... nodeNames) {
        final String ns = "ns:cont:cont";
        final String date = "2014-07-24";
        InstanceIdentifierBuilder iiBuilder = YangInstanceIdentifier.builder();
        for (String nodeName : nodeNames) {
            iiBuilder.node(buildQName(nodeName, ns, date));
        }
        return iiBuilder.toInstance();
    }

}
