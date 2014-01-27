/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class CnSnToJsonWithAugmentTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/augmentation", 5, "yang", "cont");
    }

    /**
     * Test of json output when as input are specified composite node with empty
     * data + YANG file
     */
    @Test
    public void augmentedElementsToJson() {
        CompositeNode compositeNode = TestUtils.readInputToCnSn("/cnsn-to-json/augmentation/xml/data.xml",
                XmlToCompositeNodeProvider.INSTANCE);
        TestUtils.normalizeCompositeNode(compositeNode, modules, searchedModuleName + ":" + searchedDataSchemaName);

        String jsonOutput = null;
        try {
            jsonOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, dataSchemaNode,
                    StructuredDataToJsonProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
        }
        assertNotNull(jsonOutput);

        assertTrue(jsonOutput.contains("\"augment-leaf:lf2\": \"lf2\""));
        assertTrue(jsonOutput.contains("\"augment-container:cont1\": {"));
        assertTrue(jsonOutput.contains("\"augment-container:lf11\": \"lf11\""));
        assertTrue(jsonOutput.contains("\"augment-list:lst1\": ["));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_1\""));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_2\""));
        assertTrue(jsonOutput.contains("\"augment-leaflist:lflst1\": ["));
    }
}
