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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class UnionTypeDefTest {
    private final static List<File> yangModels = new ArrayList<>();
    private final static String yangModelsFolder = AugmentedTypeTest.class
            .getResource("/union-test-models").getPath();

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
    public void unionTypeResolvingTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        assertNotNull("context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());
    }
}
