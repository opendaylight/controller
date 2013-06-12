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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTypeBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.java.api.generator.GeneratorJavaFile;
import org.opendaylight.controller.sal.java.api.generator.InterfaceGenerator;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class GeneratorJavaFileTest {
    private static final String FS = File.separator;
    private static final String PATH = "target/test/test-dir";
    private final File testDir = new File(PATH);

    private static final String GENERATOR_OUTPUT_PATH = "target/test/src";
    private static final File GENERATOR_OUTPUT = new File(GENERATOR_OUTPUT_PATH);
    private static final String COMPILER_OUTPUT_PATH = "target/test/bin";
    private static final File COMPILER_OUTPUT = new File(COMPILER_OUTPUT_PATH);

    @Before
    public void init() {
        assertTrue(testDir.mkdirs());
        assertTrue(COMPILER_OUTPUT.mkdirs());
        assertTrue(GENERATOR_OUTPUT.mkdirs());
    }

    @After
    public void cleanUp() {
        if(testDir.exists()) {
            deleteTestDir(testDir);
        }
        if(COMPILER_OUTPUT.exists()) {
            deleteTestDir(COMPILER_OUTPUT);
        }
        if(GENERATOR_OUTPUT.exists()) {
            deleteTestDir(GENERATOR_OUTPUT);
        }
    }

    @Test
    public void test() throws IOException {
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
        generator.generateToFile(new File(PATH));

        String[] files = new File(PATH + FS + "org" + FS + "opendaylight" + FS
                + "controller" + FS + "gen").list();
        List<String> filesList = Arrays.asList(files);

        assertEquals(3, files.length);
        assertTrue(filesList.contains("Type1.java"));
        assertTrue(filesList.contains("Type2.java"));
        assertTrue(filesList.contains("Type3.java"));
    }

    @Ignore
    @Test
    public void compilationTest() throws Exception {
        final YangParserImpl parser = new YangParserImpl();
        final BindingGenerator bindingGenerator = new BindingGeneratorImpl();

        final String resPath = getClass().getResource("/yang").getPath();
        final File sourcesDir = new File(resPath);
        final List<File> sourceFiles = new ArrayList<File>();
        final File[] fileArray = sourcesDir.listFiles();

        for (int i = 0; i < fileArray.length; ++i) {
            sourceFiles.add(fileArray[i]);
        }

        final Set<Module> modulesToBuild = parser.parseYangModels(sourceFiles);

        final SchemaContext context = parser
                .resolveSchemaContext(modulesToBuild);
        final List<Type> types = bindingGenerator.generateTypes(context);
        final Set<GeneratedType> typesToGenerate = new HashSet<GeneratedType>();
        final Set<GeneratedTransferObject> tosToGenerate = new HashSet<GeneratedTransferObject>();
        final Set<Enumeration> enumerationToGenerate = new HashSet<Enumeration>();
        for (Type type : types) {
            if (type instanceof GeneratedType
                    && !(type instanceof GeneratedTransferObject)) {
                typesToGenerate.add((GeneratedType) type);
            }

            if (type instanceof GeneratedTransferObject) {
                tosToGenerate.add((GeneratedTransferObject) type);
            }
            if (type instanceof Enumeration) {
            	enumerationToGenerate.add((Enumeration) type);            	
            }
        }

        final GeneratorJavaFile generator = new GeneratorJavaFile(
                typesToGenerate, tosToGenerate, enumerationToGenerate);
        generator.generateToFile(new File(GENERATOR_OUTPUT_PATH));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                null, null, null);

        List<File> filesList = getJavaFiles(new File(GENERATOR_OUTPUT_PATH));
        File current = new File(System.getProperty("user.dir"));
        File parentPath = current.getParentFile().getParentFile();
        File f = new File(parentPath,"yang/yang-binding/src/main/java/org/opendaylight/controller/yang/binding/DataObject.java"
        );
        filesList.add(f);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(filesList);
        Iterable<String> options = Arrays.asList(new String[]{"-d", COMPILER_OUTPUT_PATH});
        boolean compiled = compiler.getTask(null, null, null, options, null,
                compilationUnits).call();
        assertTrue(compiled);
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

    /**
     * Search recursively given directory for *.java files.
     *
     * @param directory directory to search
     * @return List of java files found
     */
    private List<File> getJavaFiles(File directory) {
        List<File> result = new ArrayList<File>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                result.addAll(getJavaFiles(file));
            } else {
                String absPath = file.getAbsolutePath();
                if (absPath.endsWith(".java")) {
                    result.add(file);
                }
            }
        }
        return result;
    }
}
