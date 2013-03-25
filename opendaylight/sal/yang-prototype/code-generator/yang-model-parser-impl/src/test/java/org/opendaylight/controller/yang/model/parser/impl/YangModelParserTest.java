/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.model.util.Int32;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;

public class YangModelParserTest {

    private final String testFile1 = "src/test/resources/model/testfile1.yang";
    private final String testFile2 = "src/test/resources/model/testfile2.yang";
    private YangModelParser tested;

    @Before
    public void init() throws IOException {
        tested = new YangModelParserImpl();
    }

    @Test
    public void testAugment() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m2 = null;
        for(Module m : modules) {
            if(m.getName().equals("types2")) {
                m2 = m;
            }
        }
        assertNotNull(m2);

        AugmentationSchema augment = m2.getAugmentations().iterator().next();
        assertNotNull(augment);
    }

    @Test
    public void testAugmentTarget() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        ContainerSchemaNode container = (ContainerSchemaNode)m1.getDataChildByName("interfaces");
        assertNotNull(container);

        ListSchemaNode list = (ListSchemaNode)container.getDataChildByName("ifEntry");
        assertNotNull(list);

        LeafSchemaNode leaf = (LeafSchemaNode)list.getDataChildByName("ds0ChannelNumber");
        assertNotNull(leaf);
    }

    @Test
    public void testTypeDef() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("testleaf");
        assertTrue(testleaf.getType().getBaseType() instanceof Int32);
    }

}
