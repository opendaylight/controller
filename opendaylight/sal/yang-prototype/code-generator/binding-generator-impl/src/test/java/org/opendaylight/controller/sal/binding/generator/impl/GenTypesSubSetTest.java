/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GenTypesSubSetTest {

    private final static List<File> yangModels = new ArrayList<>();
    private final static String yangModelsFolder = AugmentedTypeTest.class
            .getResource("/leafref-test-models").getPath();

    @BeforeClass
    public static void loadTestResources() {
        final File augFolder = new File(yangModelsFolder);

        for (final File fileEntry : augFolder.listFiles()) {
            if (fileEntry.isFile()) {
                yangModels.add(fileEntry);
            }
        }
    }

    @Test
    public void genTypesFromSubsetOfTwoModulesTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        final Set<Module> toGenModules = new HashSet<>();
        for (final Module module : modules) {
            if (module.getName().equals("abstract-topology")) {
                toGenModules.add(module);
            } else if (module.getName().equals("ietf-interfaces")) {
                toGenModules.add(module);
            }
        }

        assertEquals("Set of to Generate Modules must contain 2 modules", 2,
                toGenModules.size());
        assertNotNull("Schema Context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context, toGenModules);
        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());
        assertEquals("Expected Generated Types from provided sub set of " +
                "modules should be 23!", 23,
                genTypes.size());
    }

    @Test
    public void genTypesFromSubsetOfThreeModulesTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        final Set<Module> toGenModules = new HashSet<>();
        for (final Module module : modules) {
            if (module.getName().equals("abstract-topology")) {
                toGenModules.add(module);
            } else if (module.getName().equals("ietf-interfaces")) {
                toGenModules.add(module);
            } else if (module.getName().equals("iana-if-type")) {
                toGenModules.add(module);
            }
        }

        assertEquals("Set of to Generate Modules must contain 3 modules", 3,
                toGenModules.size());

        assertNotNull("Schema Context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context, toGenModules);
        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());
        assertEquals("Expected Generated Types from provided sub set of "  +
                "modules should be 25!", 25, genTypes.size());
    }
}
