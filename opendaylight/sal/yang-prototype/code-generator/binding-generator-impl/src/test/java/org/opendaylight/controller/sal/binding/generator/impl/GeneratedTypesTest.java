/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.model.api.*;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class GeneratedTypesTest {

    private SchemaContext resolveSchemaContextFromFiles(
            final String... yangFiles) {
        final YangModelParser parser = new YangParserImpl();

        final List<File> inputFiles = new ArrayList<File>();
        for (int i = 0; i < yangFiles.length; ++i) {
            inputFiles.add(new File(yangFiles[i]));
        }

        final Set<Module> modules = parser.parseYangModels(inputFiles);
        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void testLeafEnumResolving() {
        final String ietfInterfacesPath = getClass().getResource(
                "/enum-test-models/ietf-interfaces@2012-11-15.yang").getPath();
        final String ifTypePath = getClass().getResource(
                "/enum-test-models/iana-if-type@2012-06-05.yang").getPath();
        final String yangTypesPath = getClass().getResource(
                "/enum-test-models/ietf-yang-types@2010-09-24.yang").getPath();

        final SchemaContext context = resolveSchemaContextFromFiles(
                ietfInterfacesPath, ifTypePath, yangTypesPath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);
        assertTrue(genTypes != null);
    }

    @Test
    public void testTypedefEnumResolving() {
        final String ianaIfTypePath = getClass().getResource(
                "/leafref-test-models/iana-if-type@2012-06-05.yang").getPath();

        final SchemaContext context = resolveSchemaContextFromFiles(ianaIfTypePath);
        assertTrue(context != null);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);
        assertTrue(genTypes != null);
        assertEquals(2, genTypes.size());

        final Type type = genTypes.get(1);
        assertTrue(type instanceof GeneratedTransferObject);

        final GeneratedTransferObject genTransObj = (GeneratedTransferObject) type;
        final List<GeneratedProperty> properties = genTransObj.getProperties();
        assertNotNull(properties);
        assertEquals(1, properties.size());

        GeneratedProperty property = properties.get(0);
        assertNotNull(property);
        assertNotNull(property.getReturnType());

        assertTrue(property.getReturnType() instanceof Enumeration);
        final Enumeration enumer = (Enumeration) property.getReturnType();
        assertEquals(272, enumer.getValues().size());
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
        assertEquals(24, genTypes.size());
    }

    @Test
    public void testLeafrefResolving() {
        final String topologyPath = getClass().getResource(
                "/leafref-test-models/abstract-topology@2013-02-08.yang")
                .getPath();
        final String interfacesPath = getClass().getResource(
                "/leafref-test-models/ietf-interfaces@2012-11-15.yang")
                .getPath();
        // final String ifTypePath = getClass().getResource(
        // "/leafref-test-models/iana-if-type@2012-06-05.yang").getPath();
        final String inetTypesPath = getClass().getResource(
                "/leafref-test-models/ietf-inet-types@2010-09-24.yang")
                .getPath();
        final String yangTypesPath = getClass().getResource(
                "/leafref-test-models/ietf-yang-types@2010-09-24.yang")
                .getPath();

        assertTrue(topologyPath != null);
        assertTrue(interfacesPath != null);
        // assertTrue(ifTypePath != null);
        assertTrue(inetTypesPath != null);
        assertTrue(yangTypesPath != null);

        // final SchemaContext context = resolveSchemaContextFromFiles(
        // topologyPath, interfacesPath, ifTypePath, inetTypesPath,
        // yangTypesPath);
        final SchemaContext context = resolveSchemaContextFromFiles(
                topologyPath, interfacesPath, inetTypesPath, yangTypesPath);
        assertTrue(context != null);
        assertEquals(4, context.getModules().size());

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertEquals(46, genTypes.size());
        assertTrue(genTypes != null);

        int resolvedLeafrefCount = 0;
        for (final Type type : genTypes) {
            if (type.getName().equals("InterfaceKey")
                    && type instanceof GeneratedTransferObject) {
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;
                final List<GeneratedProperty> properties = genTO
                        .getProperties();

                assertTrue(properties != null);
                for (final GeneratedProperty property : properties) {
                    if (property.getName().equals("InterfaceId")) {
                        assertTrue(property.getReturnType() != null);
                        assertFalse(property.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(property.getReturnType().getName()
                                .equals("String"));
                        resolvedLeafrefCount++;
                    }
                }

            } else if (type.getName().equals("Interface")
                    && type instanceof GeneratedType) {
                final GeneratedType genType = (GeneratedType) type;
                final List<MethodSignature> methods = genType
                        .getMethodDefinitions();

                assertTrue(methods != null);
                for (final MethodSignature method : methods) {
                    if (method.getName().equals("getInterfaceKey")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("InterfaceKey"));
                        resolvedLeafrefCount++;
                    } else if (method.getName().equals("getHigherLayerIf")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("List"));
                        resolvedLeafrefCount++;
                    }
                }
            } else if (type.getName().equals("NetworkLink")
                    && type instanceof GeneratedType) {
                final GeneratedType genType = (GeneratedType) type;
                final List<MethodSignature> methods = genType
                        .getMethodDefinitions();
                assertTrue(methods != null);
                for (MethodSignature method : methods) {
                    if (method.getName().equals("getInterface")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("String"));
                        resolvedLeafrefCount++;
                    }
                }
            } else if ((type.getName().equals("SourceNode") || type.getName()
                    .equals("DestinationNode"))
                    && type instanceof GeneratedType) {
                final GeneratedType genType = (GeneratedType) type;
                final List<MethodSignature> methods = genType
                        .getMethodDefinitions();
                assertTrue(methods != null);
                for (MethodSignature method : methods) {
                    if (method.getName().equals("getId")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("Uri"));
                        resolvedLeafrefCount++;
                    }
                }
            } else if (type.getName().equals("Tunnel")
                    && type instanceof GeneratedType) {
                final GeneratedType genType = (GeneratedType) type;
                final List<MethodSignature> methods = genType
                        .getMethodDefinitions();
                assertTrue(methods != null);
                for (MethodSignature method : methods) {
                    if (method.getName().equals("getTunnelKey")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("TunnelKey"));
                        resolvedLeafrefCount++;
                    }
                }
            } else if (type.getName().equals("TunnelKey")
                    && type instanceof GeneratedTransferObject) {
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;
                final List<GeneratedProperty> properties = genTO
                        .getProperties();

                assertTrue(properties != null);
                for (final GeneratedProperty property : properties) {
                    if (property.getName().equals("TunnelId")) {
                        assertTrue(property.getReturnType() != null);
                        assertFalse(property.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(property.getReturnType().getName()
                                .equals("Uri"));
                        resolvedLeafrefCount++;
                    }
                }
            }
        }
        assertEquals(10, resolvedLeafrefCount);
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
        assertEquals(3, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(0);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(1);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(5, simpleContainer.getMethodDefinitions().size());
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
        assertEquals(3, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(0);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(1);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(5, simpleContainer.getMethodDefinitions().size());
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
        assertEquals(5, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType &&
                    !(type instanceof GeneratedTransferObject)) {
                final GeneratedType genType = (GeneratedType) type;
                if (genType.getName().equals("ListParentContainer")) {
                    assertEquals(2, genType.getMethodDefinitions().size());
                    genTypesCount++;
                } else if (genType.getName().equals("SimpleList")) {
                    assertEquals(8, genType.getMethodDefinitions().size());
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
        assertEquals(7, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType &&
                    !(type instanceof GeneratedTransferObject)) {
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

        assertEquals(5, genTypesCount);
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
        assertEquals(14, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
            }
        }

        assertEquals(11, genTypesCount);
        assertEquals(3, genTOsCount);
    }
}
