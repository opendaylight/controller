/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.text.ParseException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.parser.impl.YangModelParserImpl;

public class YangModelBuilderTest {

    private Set<Module> builtModules;

    @Before
    public void init() {
        builtModules = parseModules();
    }

    @Test
    public void testAugment() throws ParseException {
        for(Module module : builtModules) {
            if(module.getName().equals("types2")) {
                Set<AugmentationSchema> augmentations = module.getAugmentations();
                assertEquals(1, augmentations.size());
                AugmentationSchema augment = augmentations.iterator().next();
                LeafSchemaNode augmentedLeaf = (LeafSchemaNode)augment.getDataChildByName("ds0ChannelNumber");
                assertNotNull(augmentedLeaf);
                assertTrue(augmentedLeaf.isAugmenting());
            } else if(module.getName().equals("types1")) {
                DataNodeContainer interfaces = (DataNodeContainer)module.getDataChildByName("interfaces");
                DataNodeContainer ifEntry = (DataNodeContainer)interfaces.getDataChildByName("ifEntry");
                assertNotNull(ifEntry);
            } else {
                fail("unexpected module");
            }
        }
    }

    private Set<Module> parseModules() {
        String yangFilesDir = "src/test/resources/model";
        File resourceDir = new File(yangFilesDir);

        String[] dirList = resourceDir.list();
        String[] absFiles = new String[dirList.length];

        int i = 0;
        for (String fileName : dirList) {
            File abs = new File(resourceDir, fileName);
            absFiles[i] = abs.getAbsolutePath();
            i++;
        }

        YangModelParserImpl parser = new YangModelParserImpl();
        Set<Module> modules = parser.parseYangModels(absFiles);

        return modules;
    }

}
