/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.*;

public class JsonLeafrefToCnSnTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/json-to-cnsn/leafref");
    }

    /**
     * JSON values which represents leafref are always loaded to simple node as
     * string
     */
    @Test
    public void jsonIdentityrefToCompositeNode() {
        CompositeNode compositeNode = TestUtils.readInputToCnSn("/json-to-cnsn/leafref/json/data.json", false,
                JsonToCompositeNodeProvider.INSTANCE);
        assertNotNull(compositeNode);
        TestUtils.normalizeCompositeNode(compositeNode, modules, searchedModuleName + ":" + searchedDataSchemaName);

        assertEquals("cont", compositeNode.getNodeType().getLocalName());

        SimpleNode<?> lf2 = null;
        for (Node<?> childNode : compositeNode.getChildren()) {
            if (childNode instanceof SimpleNode) {
                if (childNode.getNodeType().getLocalName().equals("lf2")) {
                    lf2 = (SimpleNode<?>) childNode;
                    break;
                }
            }
        }

        assertNotNull(lf2);
        assertTrue(lf2.getValue() instanceof String);
        assertEquals("121", (String) lf2.getValue());

    }

}
