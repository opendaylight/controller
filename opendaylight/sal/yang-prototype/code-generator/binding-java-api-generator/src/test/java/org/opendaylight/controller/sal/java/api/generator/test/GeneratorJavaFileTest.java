/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.impl.GeneratedTypeBuilderImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.java.api.generator.GeneratorJavaFile;
import org.opendaylight.controller.sal.java.api.generator.InterfaceGenerator;

public class GeneratorJavaFileTest {

    private static final String FS = File.separator;
    private static final String PATH = "test-dir";
    private final File testDir = new File(PATH);

    @Before
    public void init() {
        assertTrue(testDir.mkdir());
    }

    @After
    public void cleanUp() {
        deleteTestDir(testDir);
    }

    @Test
    public void test() {
        final Set<GeneratedType> types = new HashSet<GeneratedType>();
        GeneratedType t1 = createGeneratedType(
                "org.opendaylight.controller.gen", "Type1");
        GeneratedType t2 = createGeneratedType(
                "org.opendaylight.controller.gen", "Type2");
        GeneratedType t3 = createGeneratedType(
                "org.opendaylight.controller.gen", "Type3");
        types.add(t1);
        types.add(t2);
        types.add(t3);
        GeneratorJavaFile generator = new GeneratorJavaFile(
                new InterfaceGenerator(), types);
        generator.generateToFile(PATH);

        // path: test-dir/com/cisco/yang
        String[] files = new File(PATH + FS + "org" + FS + "opendaylight" + FS + "controller" + FS + "gen").list();
        List<String> filesList = Arrays.asList(files);

        assertEquals(3, files.length);
        assertTrue(filesList.contains("Type1.java"));
        assertTrue(filesList.contains("Type2.java"));
        assertTrue(filesList.contains("Type3.java"));
    }

    private GeneratedType createGeneratedType(String pkgName, String name) {
        GeneratedTypeBuilder builder = new GeneratedTypeBuilderImpl(pkgName,
                name);
        return builder.toInstance();
    }

    private void deleteTestDir(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteTestDir(f);
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to clean up after test");
        }
    }

}
