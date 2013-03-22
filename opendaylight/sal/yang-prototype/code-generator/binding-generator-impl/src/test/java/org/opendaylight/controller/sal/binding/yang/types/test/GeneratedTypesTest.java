/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.model.parser.builder.ModuleBuilder;
import org.opendaylight.controller.model.parser.impl.YangModelParserImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.yang.model.api.Module;

public class GeneratedTypesTest {

    private Module resolveModuleFromFile(final String filePath) {
        try {
            final InputStream inStream = getClass().getResourceAsStream(
                    filePath);
            if (inStream != null) {
                ANTLRInputStream input = new ANTLRInputStream(inStream);
                final YangLexer lexer = new YangLexer(input);
                final CommonTokenStream tokens = new CommonTokenStream(lexer);
                final YangParser parser = new YangParser(tokens);

                final ParseTree tree = parser.yang();
                final ParseTreeWalker walker = new ParseTreeWalker();

                final YangModelParserImpl modelParser = new YangModelParserImpl();
                walker.walk(modelParser, tree);

                final ModuleBuilder genModule = modelParser.getModuleBuilder();
                final Module module = genModule.build();

                return module;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testContainerResolving() {
        final Module module = resolveModuleFromFile("/simple-container-demo.yang");
        assertTrue(module != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<GeneratedType> genTypes = bindingGen.generateTypes(module);

        assertTrue(genTypes != null);
        assertEquals(genTypes.size(), 2);

        final GeneratedType simpleContainer = genTypes.get(0);
        final GeneratedType nestedContainer = genTypes.get(1);

        assertEquals(simpleContainer.getName(), "SimpleContainer");
        assertEquals(nestedContainer.getName(), "NestedContainer");

        assertEquals(simpleContainer.getMethodDefinitions().size(), 4);
        assertEquals(nestedContainer.getMethodDefinitions().size(), 4);

        int methodsCount = 0;
        for (final MethodSignature method : simpleContainer
                .getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                method.getReturnType().getName().equals("Integer");
                methodsCount++;
            }

            if (method.getName().equals("setFoo")) {
                methodsCount++;
                final MethodSignature.Parameter param = method.getParameters()
                        .get(0);
                assertEquals(param.getName(), "foo");
                assertEquals(param.getType().getName(), "Integer");
            }

            if (method.getName().equals("getBar")) {
                method.getReturnType().getName().equals("String");
                methodsCount++;
            }

            if (method.getName().equals("getNestedContainer")) {
                method.getReturnType().getName().equals("NestedContainer");
                methodsCount++;
            }
        }
        assertEquals(methodsCount, 4);

        methodsCount = 0;
        for (final MethodSignature method : nestedContainer
                .getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                method.getReturnType().getName().equals("Short");
                methodsCount++;
            }

            if (method.getName().equals("setFoo")) {
                methodsCount++;
                final MethodSignature.Parameter param = method.getParameters()
                        .get(0);
                assertEquals(param.getName(), "foo");
                assertEquals(param.getType().getName(), "Short");
            }

            if (method.getName().equals("getBar")) {
                method.getReturnType().getName().equals("String");
                methodsCount++;
            }

            if (method.getName().equals("setBar")) {
                method.getReturnType().getName().equals("String");
                methodsCount++;
            }
        }
        assertEquals(methodsCount, 4);
    }

    @Test
    public void testLeafListResolving() {
        final Module module = resolveModuleFromFile("/simple-leaf-list-demo.yang");
        assertTrue(module != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<GeneratedType> genTypes = bindingGen.generateTypes(module);

        assertTrue(genTypes != null);
        assertEquals(genTypes.size(), 2);

        final GeneratedType simpleContainer = genTypes.get(0);
        final GeneratedType nestedContainer = genTypes.get(1);

        assertEquals(simpleContainer.getName(), "SimpleContainer");
        assertEquals(nestedContainer.getName(), "NestedContainer");

        // FIXME: uncomment after fix in DOM tree parser - LeafSchemaNode bad
        // isConfig resolving
        assertEquals(simpleContainer.getMethodDefinitions().size(), 4);
        assertEquals(nestedContainer.getMethodDefinitions().size(), 3);

        int methodsCount = 0;
        for (final MethodSignature method : simpleContainer
                .getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                method.getReturnType().getName().equals("List");
                methodsCount++;
            }

            if (method.getName().equals("setFoo")) {
                methodsCount++;
                final MethodSignature.Parameter param = method.getParameters()
                        .get(0);
                assertEquals(param.getName(), "foo");
                assertEquals(param.getType().getName(), "List");
            }

            if (method.getName().equals("getBar")) {
                method.getReturnType().getName().equals("String");
                methodsCount++;
            }

            if (method.getName().equals("getNestedContainer")) {
                method.getReturnType().getName().equals("NestedContainer");
                methodsCount++;
            }
        }
        assertEquals(methodsCount, 4);

        methodsCount = 0;
        for (final MethodSignature method : nestedContainer
                .getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                method.getReturnType().getName().equals("Short");
                methodsCount++;
            }

            if (method.getName().equals("setFoo")) {
                methodsCount++;
                final MethodSignature.Parameter param = method.getParameters()
                        .get(0);
                assertEquals(param.getName(), "foo");
                assertEquals(param.getType().getName(), "Short");
            }

            if (method.getName().equals("getBar")) {
                method.getReturnType().getName().equals("List");
                methodsCount++;
            }
        }
        assertEquals(methodsCount, 3);
    }

    @Test
    public void testListResolving() {
        final Module module = resolveModuleFromFile("/simple-list-demo.yang");
        assertTrue(module != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<GeneratedType> genTypes = bindingGen.generateTypes(module);

        assertTrue(genTypes != null);
        assertEquals(genTypes.size(), 3);
    }

    @Test
    public void testGeneratedTypes() {
        final Module module = resolveModuleFromFile("/demo-topology.yang");
        assertTrue(module != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<GeneratedType> genTypes = bindingGen.generateTypes(module);

        assertTrue(genTypes != null);
        assertEquals(genTypes.size(), 10);
    }
}
