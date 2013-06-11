/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

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
        assertEquals(27, genTypes.size());
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

        assertEquals(53, genTypes.size());
        assertTrue(genTypes != null);

        GeneratedTransferObject gtIfcKey = null;
        GeneratedType gtIfc = null;
        GeneratedType gtNetworkLink = null;
        GeneratedType gtSource = null;
        GeneratedType gtDest = null;
        GeneratedType gtTunnel = null;
        GeneratedTransferObject gtTunnelKey = null;
        for (final Type type : genTypes) {
            String name = type.getName();
            if ("InterfaceKey".equals(name)) {
                gtIfcKey = (GeneratedTransferObject) type;
            } else if ("Interface".equals(name)) {
                gtIfc = (GeneratedType) type;
            } else if ("NetworkLink".equals(name)) {
                gtNetworkLink = (GeneratedType) type;
            } else if ("SourceNode".equals(name)) {
                gtSource = (GeneratedType) type;
            } else if ("DestinationNode".equals(name)) {
                gtDest = (GeneratedType) type;
            } else if ("Tunnel".equals(name)) {
                gtTunnel = (GeneratedType) type;
            } else if ("TunnelKey".equals(name)) {
                gtTunnelKey = (GeneratedTransferObject) type;
            }
        }

        assertNotNull(gtIfcKey);
        assertNotNull(gtIfc);
        assertNotNull(gtNetworkLink);
        assertNotNull(gtSource);
        assertNotNull(gtDest);
        assertNotNull(gtTunnel);
        assertNotNull(gtTunnelKey);

        // InterfaceId
        final List<GeneratedProperty> gtIfcKeyProps = gtIfcKey.getProperties();
        assertNotNull(gtIfcKeyProps);
        GeneratedProperty ifcIdProp = null;
        for (final GeneratedProperty property : gtIfcKeyProps) {
            if (property.getName().equals("InterfaceId")) {
                ifcIdProp = property;
            }
        }
        assertNotNull(ifcIdProp);
        Type ifcIdPropType = ifcIdProp.getReturnType();
        assertNotNull(ifcIdPropType);
        assertFalse(ifcIdPropType.equals("java.lang.Void"));
        assertTrue(ifcIdPropType.getName().equals("String"));

        // Interface
        final List<MethodSignature> gtIfcMethods = gtIfc.getMethodDefinitions();
        assertNotNull(gtIfcMethods);
        MethodSignature getIfcKey = null;
        MethodSignature getHigherLayerIf = null;
        for (final MethodSignature method : gtIfcMethods) {
            if (method.getName().equals("getInterfaceKey")) {
                getIfcKey = method;
            } else if (method.getName().equals("getHigherLayerIf")) {
                getHigherLayerIf = method;
            }
        }
        assertNotNull(getIfcKey);
        Type getIfcKeyType = getIfcKey.getReturnType();
        assertNotNull(getIfcKeyType);
        assertFalse(getIfcKeyType.equals("java.lang.Void"));
        assertTrue(getIfcKeyType.getName().equals("InterfaceKey"));

        assertNotNull(getHigherLayerIf);
        Type getHigherLayerIfType = getHigherLayerIf.getReturnType();
        assertNotNull(getHigherLayerIfType);
        assertFalse(getHigherLayerIfType.equals("java.lang.Void"));
        assertTrue(getHigherLayerIfType.getName().equals("List"));

        // NetworkLink
        final List<MethodSignature> gtNetworkLinkMethods = gtNetworkLink
                .getMethodDefinitions();
        assertNotNull(gtNetworkLinkMethods);
        MethodSignature getIfc = null;
        for (MethodSignature method : gtNetworkLinkMethods) {
            if (method.getName().equals("getInterface")) {
                getIfc = method;
            }
        }
        assertNotNull(getIfc);
        Type getIfcType = getIfc.getReturnType();
        assertNotNull(getIfcType);
        assertFalse(getIfcType.equals("java.lang.Void"));
        assertTrue(getIfcType.getName().equals("String"));

        // SourceNode
        final List<MethodSignature> gtSourceMethods = gtSource
                .getMethodDefinitions();
        assertNotNull(gtSourceMethods);
        MethodSignature getIdSource = null;
        for (MethodSignature method : gtSourceMethods) {
            if (method.getName().equals("getId")) {
                getIdSource = method;
            }
        }
        assertNotNull(getIdSource);
        Type getIdType = getIdSource.getReturnType();
        assertNotNull(getIdType);
        assertFalse(getIdType.equals("java.lang.Void"));
        assertTrue(getIdType.getName().equals("Uri"));

        // DestinationNode
        final List<MethodSignature> gtDestMethods = gtDest
                .getMethodDefinitions();
        assertNotNull(gtDestMethods);
        MethodSignature getIdDest = null;
        for (MethodSignature method : gtDestMethods) {
            if (method.getName().equals("getId")) {
                getIdDest = method;
            }
        }
        assertNotNull(getIdDest);
        Type getIdDestType = getIdDest.getReturnType();
        assertNotNull(getIdDestType);
        assertFalse(getIdDestType.equals("java.lang.Void"));
        assertTrue(getIdDestType.getName().equals("Uri"));

        // Tunnel
        final List<MethodSignature> gtTunnelMethods = gtTunnel
                .getMethodDefinitions();
        assertNotNull(gtTunnelMethods);
        MethodSignature getTunnelKey = null;
        for (MethodSignature method : gtTunnelMethods) {
            if (method.getName().equals("getTunnelKey")) {
                getTunnelKey = method;
            }
        }
        assertNotNull(getTunnelKey);
        Type getTunnelKeyType = getTunnelKey.getReturnType();
        assertNotNull(getTunnelKeyType);
        assertFalse(getTunnelKeyType.equals("java.lang.Void"));
        assertTrue(getTunnelKeyType.getName().equals("TunnelKey"));

        // TunnelKey
        final List<GeneratedProperty> gtTunnelKeyProps = gtTunnelKey
                .getProperties();
        assertNotNull(gtTunnelKeyProps);
        GeneratedProperty tunnelId = null;
        for (final GeneratedProperty property : gtTunnelKeyProps) {
            if (property.getName().equals("TunnelId")) {
                tunnelId = property;
            }
        }
        assertNotNull(tunnelId);
        Type tunnelIdType = tunnelId.getReturnType();
        assertNotNull(tunnelIdType);
        assertFalse(tunnelIdType.equals("java.lang.Void"));
        assertTrue(tunnelIdType.getName().equals("Uri"));
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

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(1);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(2);

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

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(1);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(2);

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
            if (type instanceof GeneratedType
                    && !(type instanceof GeneratedTransferObject)) {
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
            if (type instanceof GeneratedType
                    && !(type instanceof GeneratedTransferObject)) {
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
            if (type instanceof GeneratedType
                    && !(type instanceof GeneratedTransferObject)) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
            }
        }

        assertEquals(11, genTypesCount);
        assertEquals(3, genTOsCount);
    }
}
