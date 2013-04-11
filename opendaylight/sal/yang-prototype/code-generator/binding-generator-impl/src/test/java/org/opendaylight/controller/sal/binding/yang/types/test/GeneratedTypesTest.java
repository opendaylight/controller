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

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.parser.impl.YangModelParserImpl;

public class GeneratedTypesTest {

    private SchemaContext resolveSchemaContextFromFiles(
            final String... yangFiles) {
        final YangModelParser parser = new YangModelParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangFiles);

        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void testMultipleModulesResolving() {
        final String topologyPath = getClass().getResource(
                "/abstract-topology.yang").getPath();
        final String typesPath = getClass().getResource(
                "/ietf-inet-types@2010-09-24.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(
                topologyPath, typesPath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(11, genTypes.size());
    }
    
    @Test
    public void testLeafrefResolving() {
        final String topologyPath = getClass().getResource(
                "/leafref-test-models/abstract-topology@2013-02-08.yang")
                .getPath();
        final String interfacesPath = getClass().getResource(
                "/leafref-test-models/ietf-interfaces@2012-11-15.yang")
                .getPath();
//        final String ifTypePath = getClass().getResource(
//                "/leafref-test-models/iana-if-type@2012-06-05.yang").getPath();
        final String inetTypesPath = getClass().getResource(
                "/leafref-test-models/ietf-inet-types@2010-09-24.yang")
                .getPath();
        final String yangTypesPath = getClass().getResource(
                "/leafref-test-models/ietf-yang-types@2010-09-24.yang")
                .getPath();

        assertTrue(topologyPath != null);
        assertTrue(interfacesPath != null);
//        assertTrue(ifTypePath != null);
        assertTrue(inetTypesPath != null);
        assertTrue(yangTypesPath != null);

//        final SchemaContext context = resolveSchemaContextFromFiles(
//                topologyPath, interfacesPath, ifTypePath, inetTypesPath, yangTypesPath);
        final SchemaContext context = resolveSchemaContextFromFiles(
                topologyPath, interfacesPath, inetTypesPath, yangTypesPath);
        assertTrue(context != null);
        assertEquals(4, context.getModules().size());
        
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);
        
        assertEquals(21, genTypes.size());
        assertTrue(genTypes != null);
        
        for (final Type genType : genTypes) {
            if (genType.getName().equals("Interface") && genType instanceof GeneratedType) {
//                System.out.println(((GeneratedType)genType).getMethodDefinitions().toString());
            } else if (genType.getName().equals("NetworkLink") && genType instanceof GeneratedType) {
//                System.out.println(((GeneratedType)genType).getMethodDefinitions().toString());
            } 
        }
    }

    @Test
    public void testContainerResolving() {
        final String filePath = getClass().getResource(
                "/simple-container-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(2, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(0);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(1);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(4, simpleContainer.getMethodDefinitions().size());
        assertEquals(4, nestedContainer.getMethodDefinitions().size());

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
                assertEquals("foo", param.getName());
                assertEquals("Integer", param.getType().getName());
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
        assertEquals(4, methodsCount);

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
                assertEquals("foo", param.getName());
                assertEquals("Short", param.getType().getName());
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
        assertEquals(4, methodsCount);
    }

    @Test
    public void testLeafListResolving() {
        final String filePath = getClass().getResource(
                "/simple-leaf-list-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(2, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(0);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(1);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(4, simpleContainer.getMethodDefinitions().size());
        assertEquals(3, nestedContainer.getMethodDefinitions().size());

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
                assertEquals("foo", param.getName());
                assertEquals("List", param.getType().getName());
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
        assertEquals(4, methodsCount);

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
                assertEquals("foo", param.getName());
                assertEquals("Short", param.getType().getName());
            }

            if (method.getName().equals("getBar")) {
                method.getReturnType().getName().equals("List");
                methodsCount++;
            }
        }
        assertEquals(3, methodsCount);
    }

    @Test
    public void testListResolving() {
        final String filePath = getClass()
                .getResource("/simple-list-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(4, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType) {
                final GeneratedType genType = (GeneratedType) type;
                if (genType.getName().equals("ListParentContainer")) {
                    assertEquals(2, genType.getMethodDefinitions().size());
                    genTypesCount++;
                } else if (genType.getName().equals("SimpleList")) {
                    assertEquals(7, genType.getMethodDefinitions().size());
                    final List<MethodSignature> methods = genType
                            .getMethodDefinitions();
                    int methodsCount = 0;
                    for (final MethodSignature method : methods) {
                        if (method.getName().equals("getSimpleListKey")) {
                            assertEquals("SimpleListKey", method
                                    .getReturnType().getName());
                            methodsCount++;
                        } else if (method.getName().equals(
                                "getListChildContainer")) {
                            assertEquals("ListChildContainer", method
                                    .getReturnType().getName());
                            methodsCount++;
                        } else if (method.getName().equals("getFoo")) {
                            methodsCount++;
                        } else if (method.getName().equals("setFoo")) {
                            methodsCount++;
                        } else if (method.getName().equals("getSimpleLeafList")) {
                            methodsCount++;
                        } else if (method.getName().equals("setSimpleLeafList")) {
                            methodsCount++;
                        } else if (method.getName().equals("getBar")) {
                            methodsCount++;
                        }
                    }
                    assertEquals(7, methodsCount);
                    genTypesCount++;
                } else if (genType.getName().equals("ListChildContainer")) {
                    assertEquals(2, genType.getMethodDefinitions().size());
                    genTypesCount++;
                }
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;
                final List<GeneratedProperty> properties = genTO
                        .getProperties();
                final List<GeneratedProperty> hashProps = genTO
                        .getHashCodeIdentifiers();
                final List<GeneratedProperty> equalProps = genTO
                        .getEqualsIdentifiers();

                assertEquals(1, properties.size());
                assertEquals("ListKey", properties.get(0).getName());
                assertEquals("Byte", properties.get(0).getReturnType()
                        .getName());
                assertEquals(true, properties.get(0).isReadOnly());
                assertEquals(1, hashProps.size());
                assertEquals("ListKey", hashProps.get(0).getName());
                assertEquals("Byte", hashProps.get(0).getReturnType().getName());
                assertEquals(1, equalProps.size());
                assertEquals("ListKey", equalProps.get(0).getName());
                assertEquals("Byte", equalProps.get(0).getReturnType()
                        .getName());
            }
        }
        assertEquals(3, genTypesCount);
        assertEquals(1, genTOsCount);
    }

    @Test
    public void testListCompositeKeyResolving() {
        final String filePath = getClass().getResource(
                "/list-composite-key.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);

        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(6, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;

                if (genTO.getName().equals("CompositeKeyListKey")) {
                    final List<GeneratedProperty> properties = genTO
                            .getProperties();
                    int propertyCount = 0;
                    for (final GeneratedProperty prop : properties) {
                        if (prop.getName().equals("Key1")) {
                            propertyCount++;
                        } else if (prop.getName().equals("Key2")) {
                            propertyCount++;
                        }
                    }
                    assertEquals(2, propertyCount);
                    genTOsCount++;
                } else if (genTO.getName().equals("InnerListKey")) {
                    final List<GeneratedProperty> properties = genTO
                            .getProperties();
                    assertEquals(1, properties.size());
                    genTOsCount++;
                }
            }
        }

        assertEquals(4, genTypesCount);
        assertEquals(2, genTOsCount);
    }

    @Test
    public void testGeneratedTypes() {
        final String filePath = getClass().getResource("/demo-topology.yang")
                .getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(13, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
            }
        }

        assertEquals(10, genTypesCount);
        assertEquals(3, genTOsCount);
    }
}
