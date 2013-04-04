/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.EnumerationType;

public class TypesResolutionTest {

    private YangModelParser parser;
    private String[] testFiles;
    private Set<Module> modules;

    @Before
    public void init() {
        parser = new YangModelParserImpl();
        testFiles = new String[5];

        File testDir = new File("src/test/resources/types");
        String[] fileList = testDir.list();
        int i = 0;
        for(String fileName : fileList) {
            File file = new File(testDir, fileName);
            testFiles[i] = file.getAbsolutePath();
            i++;
        }

        modules = parser.parseYangModels(testFiles);
        assertEquals(5, modules.size());
    }

    @Test
    public void testIetfInetTypes() {
        Module tested = findModule(modules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        assertEquals(14, typedefs.size());

        TypeDefinition<?> t1 = findTypedef(typedefs, "ip-version");
        EnumerationType en = (EnumerationType)t1.getBaseType();
        List<EnumPair> values = en.getValues();

        EnumPair value0 = values.get(0);
        assertEquals("unknown", value0.getName());
        assertEquals(0, (int)value0.getValue());

        EnumPair value1 = values.get(1);
        assertEquals("ipv4", value1.getName());
        assertEquals(1, (int)value1.getValue());

        EnumPair value2 = values.get(2);
        assertEquals("ipv6", value2.getName());
        assertEquals(2, (int)value2.getValue());
    }

    private Module findModule(Set<Module> modules, String name) {
        for(Module module : modules) {
            if(module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    private TypeDefinition<?> findTypedef(Set<TypeDefinition<?>> typedefs, String name) {
        for(TypeDefinition<?> td : typedefs) {
            if(td.getQName().getLocalName().equals(name)) {
                return td;
            }
        }
        return null;
    }

}
