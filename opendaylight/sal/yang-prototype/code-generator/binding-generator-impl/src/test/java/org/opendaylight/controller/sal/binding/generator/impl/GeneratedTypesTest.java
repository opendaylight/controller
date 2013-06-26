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

    private SchemaContext resolveSchemaContextFromFiles(final String... yangFiles) {
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
        final String topologyPath = getClass().getResource("/abstract-topology.yang").getPath();
        final String typesPath = getClass().getResource("/ietf-inet-types@2010-09-24.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(topologyPath, typesPath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(29, genTypes.size());
    }

    @Test
    public void testLeafrefResolving() {
        final String topologyPath = getClass().getResource("/leafref-test-models/abstract-topology@2013-02-08.yang")
                .getPath();
        final String interfacesPath = getClass().getResource("/leafref-test-models/ietf-interfaces@2012-11-15.yang")
                .getPath();
        // final String ifTypePath = getClass().getResource(
        // "/leafref-test-models/iana-if-type@2012-06-05.yang").getPath();
        final String inetTypesPath = getClass().getResource("/leafref-test-models/ietf-inet-types@2010-09-24.yang")
                .getPath();
        final String yangTypesPath = getClass().getResource("/leafref-test-models/ietf-yang-types@2010-09-24.yang")
                .getPath();

        assertTrue(topologyPath != null);
        assertTrue(interfacesPath != null);
        // assertTrue(ifTypePath != null);
        assertTrue(inetTypesPath != null);
        assertTrue(yangTypesPath != null);

        // final SchemaContext context = resolveSchemaContextFromFiles(
        // topologyPath, interfacesPath, ifTypePath, inetTypesPath,
        // yangTypesPath);
        final SchemaContext context = resolveSchemaContextFromFiles(topologyPath, interfacesPath, inetTypesPath,
                yangTypesPath);
        assertTrue(context != null);
        assertEquals(4, context.getModules().size());

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertEquals(57, genTypes.size());
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
        final List<MethodSignature> gtNetworkLinkMethods = gtNetworkLink.getMethodDefinitions();
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
        final List<MethodSignature> gtSourceMethods = gtSource.getMethodDefinitions();
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
        final List<MethodSignature> gtDestMethods = gtDest.getMethodDefinitions();
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
        final List<MethodSignature> gtTunnelMethods = gtTunnel.getMethodDefinitions();
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
        final List<GeneratedProperty> gtTunnelKeyProps = gtTunnelKey.getProperties();
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
        final String filePath = getClass().getResource("/simple-container-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(4, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(1);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(2);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(5, simpleContainer.getMethodDefinitions().size());
        assertEquals(4, nestedContainer.getMethodDefinitions().size());

        int setFooMethodCounter = 0;
        int getFooMethodCounter = 0;
        int getBarMethodCounter = 0;
        int getNestedContainerCounter = 0;

        String getFooMethodReturnTypeName = "";
        String setFooMethodInputParamName = "";
        String setFooMethodInputParamTypeName = "";
        String getBarMethodReturnTypeName = "";
        String getNestedContainerReturnTypeName = "";
        for (final MethodSignature method : simpleContainer.getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                getFooMethodCounter++;
                getFooMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("setFoo")) {
                setFooMethodCounter++;
                final MethodSignature.Parameter param = method.getParameters().get(0);
                setFooMethodInputParamName = param.getName();
                setFooMethodInputParamTypeName = param.getType().getName();
            }

            if (method.getName().equals("getBar")) {
                getBarMethodCounter++;
                getBarMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("getNestedContainer")) {
                getNestedContainerCounter++;
                getNestedContainerReturnTypeName = method.getReturnType().getName();
            }
        }

        assertTrue("Method getFoo doesn't occure 1 time.", getFooMethodCounter == 1);
        assertTrue("Method getFoo has incorrect return type.", getFooMethodReturnTypeName.equals("Integer"));

        assertTrue("Method setFoo doesn't occure 1 time.", setFooMethodCounter == 1);
        assertTrue("Method setFoo has incorrect param name.", setFooMethodInputParamName.equals("foo"));
        assertTrue("Method setFoo has incorrect param type.", setFooMethodInputParamTypeName.equals("Integer"));

        assertTrue("Method getBar doesn't occure 1 time.", getBarMethodCounter == 1);
        assertTrue("Method getBar has incorrect param type.", getBarMethodReturnTypeName.equals("String"));

        assertTrue("Method getNestedContainer doesn't occure 1 time.", getNestedContainerCounter == 1);
        assertTrue("Method getNestedContainer has incorrect param type.",
                getNestedContainerReturnTypeName.equals("NestedContainer"));

        setFooMethodCounter = 0;
        getFooMethodCounter = 0;
        getBarMethodCounter = 0;
        int setBarMethodCounter = 0;

        getFooMethodReturnTypeName = "";
        setFooMethodInputParamName = "";
        setFooMethodInputParamTypeName = "";
        getBarMethodReturnTypeName = "";
        String setBarMethodInputParamName = "";
        String setBarMethodInputParamTypeName = "";

        for (final MethodSignature method : nestedContainer.getMethodDefinitions()) {

            if (method.getName().equals("getFoo")) {
                getFooMethodCounter++;
                getFooMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("setFoo")) {
                setFooMethodCounter++;
                final MethodSignature.Parameter param = method.getParameters().get(0);
                setFooMethodInputParamName = param.getName();
                setFooMethodInputParamTypeName = param.getType().getName();
            }

            if (method.getName().equals("getBar")) {
                getBarMethodCounter++;
                getBarMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("setBar")) {
                setBarMethodCounter++;
                final MethodSignature.Parameter param = method.getParameters().get(0);
                setBarMethodInputParamName = param.getName();
                setBarMethodInputParamTypeName = param.getType().getName();
            }
        }

        assertEquals(1, getFooMethodCounter);
        assertEquals(getFooMethodReturnTypeName, "Short");

        assertEquals(1, setFooMethodCounter);
        assertEquals(setFooMethodInputParamName, "foo");
        assertEquals(setFooMethodInputParamTypeName, "Short");

        assertEquals(1, getBarMethodCounter);
        assertEquals(getBarMethodReturnTypeName, "String");

        assertEquals(1, setBarMethodCounter);
        assertEquals(setBarMethodInputParamName, "bar");
        assertEquals(setBarMethodInputParamTypeName, "String");
    }

    @Test
    public void testLeafListResolving() {
        final String filePath = getClass().getResource("/simple-leaf-list-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(4, genTypes.size());

        final GeneratedType simpleContainer = (GeneratedType) genTypes.get(1);
        final GeneratedType nestedContainer = (GeneratedType) genTypes.get(2);

        assertEquals("SimpleContainer", simpleContainer.getName());
        assertEquals("NestedContainer", nestedContainer.getName());
        assertEquals(5, simpleContainer.getMethodDefinitions().size());
        assertEquals(3, nestedContainer.getMethodDefinitions().size());

        int setFooMethodCounter = 0;
        int getFooMethodCounter = 0;
        int getBarMethodCounter = 0;
        int getNestedContainerCounter = 0;

        String getFooMethodReturnTypeName = "";
        String setFooMethodInputParamName = "";
        String setFooMethodInputParamTypeName = "";
        String getBarMethodReturnTypeName = "";
        String getNestedContainerReturnTypeName = "";
        for (final MethodSignature method : simpleContainer.getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                getFooMethodCounter++;
                getFooMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("setFoo")) {
                setFooMethodCounter++;
                final MethodSignature.Parameter param = method.getParameters().get(0);
                setFooMethodInputParamName = param.getName();
                setFooMethodInputParamTypeName = param.getType().getName();
            }

            if (method.getName().equals("getBar")) {
                getBarMethodCounter++;
                getBarMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("getNestedContainer")) {
                getNestedContainerCounter++;
                getNestedContainerReturnTypeName = method.getReturnType().getName();
            }
        }

        assertEquals(1, getFooMethodCounter);
        assertEquals(getFooMethodReturnTypeName, "List");

        assertEquals(1, setFooMethodCounter);
        assertEquals(setFooMethodInputParamName, "foo");
        assertEquals(setFooMethodInputParamTypeName, "List");

        assertEquals(1, getBarMethodCounter);
        assertEquals(getBarMethodReturnTypeName, "String");

        assertEquals(1, getNestedContainerCounter);
        assertEquals(getNestedContainerReturnTypeName, "NestedContainer");

        setFooMethodCounter = 0;
        getFooMethodCounter = 0;
        getBarMethodCounter = 0;

        getFooMethodReturnTypeName = "";
        setFooMethodInputParamName = "";
        setFooMethodInputParamTypeName = "";
        getBarMethodReturnTypeName = "";

        for (final MethodSignature method : nestedContainer.getMethodDefinitions()) {
            if (method.getName().equals("getFoo")) {
                getFooMethodCounter++;
                getFooMethodReturnTypeName = method.getReturnType().getName();
            }

            if (method.getName().equals("setFoo")) {
                setFooMethodCounter++;
                final MethodSignature.Parameter param = method.getParameters().get(0);
                setFooMethodInputParamName = param.getName();
                setFooMethodInputParamTypeName = param.getType().getName();
            }

            if (method.getName().equals("getBar")) {
                getBarMethodCounter++;
                getBarMethodReturnTypeName = method.getReturnType().getName();
            }
        }

        assertEquals(1, getFooMethodCounter);
        assertEquals(getFooMethodReturnTypeName, "Short");

        assertEquals(1, setFooMethodCounter);
        assertEquals(setFooMethodInputParamName, "foo");
        assertEquals(setFooMethodInputParamTypeName, "Short");

        assertEquals(1, getBarMethodCounter);
        assertEquals(getBarMethodReturnTypeName, "List");
    }

    @Test
    public void testListResolving() {
        final String filePath = getClass().getResource("/simple-list-demo.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(6, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;

        int listParentContainerMethodsCount = 0;
        int simpleListMethodsCount = 0;
        int listChildContainerMethodsCount = 0;
        int listKeyClassCount = 0;

        int getSimpleListKeyMethodCount = 0;
        int getListChildContainerMethodCount = 0;
        int getFooMethodCount = 0;
        int setFooMethodCount = 0;
        int getSimpleLeafListMethodCount = 0;
        int setSimpleLeafListMethodCount = 0;
        int getBarMethodCount = 0;

        String getSimpleListKeyMethodReturnTypeName = "";
        String getListChildContainerMethodReturnTypeName = "";

        int listKeyClassPropertyCount = 0;
        String listKeyClassPropertyName = "";
        String listKeyClassPropertyTypeName = "";
        boolean listKeyClassPropertyReadOnly = false;

        int hashMethodParameterCount = 0;
        String hashMethodParameterName = "";
        String hashMethodParameterReturnTypeName = "";

        int equalMethodParameterCount = 0;
        String equalMethodParameterName = "";
        String equalMethodParameterReturnTypeName = "";

        for (final Type type : genTypes) {
            if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
                final GeneratedType genType = (GeneratedType) type;
                if (genType.getName().equals("ListParentContainer")) {
                    listParentContainerMethodsCount = genType.getMethodDefinitions().size();
                    genTypesCount++;
                } else if (genType.getName().equals("SimpleList")) {
                    simpleListMethodsCount = genType.getMethodDefinitions().size();
                    final List<MethodSignature> methods = genType.getMethodDefinitions();
                    for (final MethodSignature method : methods) {
                        if (method.getName().equals("getSimpleListKey")) {
                            getSimpleListKeyMethodCount++;
                            getSimpleListKeyMethodReturnTypeName = method.getReturnType().getName();
                        } else if (method.getName().equals("getListChildContainer")) {
                            getListChildContainerMethodCount++;
                            getListChildContainerMethodReturnTypeName = method.getReturnType().getName();
                        } else if (method.getName().equals("getFoo")) {
                            getFooMethodCount++;
                        } else if (method.getName().equals("setFoo")) {
                            setFooMethodCount++;
                        } else if (method.getName().equals("getSimpleLeafList")) {
                            getSimpleLeafListMethodCount++;
                        } else if (method.getName().equals("setSimpleLeafList")) {
                            setSimpleLeafListMethodCount++;
                        } else if (method.getName().equals("getBar")) {
                            getBarMethodCount++;
                        }
                    }
                    genTypesCount++;
                } else if (genType.getName().equals("ListChildContainer")) {
                    listChildContainerMethodsCount = genType.getMethodDefinitions().size();
                    genTypesCount++;
                }
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;
                final List<GeneratedProperty> properties = genTO.getProperties();
                final List<GeneratedProperty> hashProps = genTO.getHashCodeIdentifiers();
                final List<GeneratedProperty> equalProps = genTO.getEqualsIdentifiers();

                listKeyClassCount++;
                listKeyClassPropertyCount = properties.size();
                listKeyClassPropertyName = properties.get(0).getName();
                listKeyClassPropertyTypeName = properties.get(0).getReturnType().getName();
                listKeyClassPropertyReadOnly = properties.get(0).isReadOnly();

                hashMethodParameterCount = hashProps.size();
                hashMethodParameterName = hashProps.get(0).getName();
                hashMethodParameterReturnTypeName = hashProps.get(0).getReturnType().getName();

                equalMethodParameterCount = equalProps.size();
                equalMethodParameterName = equalProps.get(0).getName();
                equalMethodParameterReturnTypeName = equalProps.get(0).getReturnType().getName();

            }
        }

        assertEquals(2, listParentContainerMethodsCount);
        assertEquals(2, listChildContainerMethodsCount);
        assertEquals(1, getSimpleListKeyMethodCount);
        assertEquals(1, listKeyClassCount);

        assertEquals(1, listKeyClassPropertyCount);
        assertEquals("ListKey", listKeyClassPropertyName);
        assertEquals("Byte", listKeyClassPropertyTypeName);
        assertEquals(true, listKeyClassPropertyReadOnly);
        assertEquals(1, hashMethodParameterCount);
        assertEquals("ListKey", hashMethodParameterName);
        assertEquals("Byte", hashMethodParameterReturnTypeName);
        assertEquals(1, equalMethodParameterCount);
        assertEquals("ListKey", equalMethodParameterName);
        assertEquals("Byte", equalMethodParameterReturnTypeName);

        assertEquals("SimpleListKey", getSimpleListKeyMethodReturnTypeName);

        assertEquals(1, getListChildContainerMethodCount);
        assertEquals("ListChildContainer", getListChildContainerMethodReturnTypeName);
        assertEquals(1, getFooMethodCount);
        assertEquals(1, setFooMethodCount);
        assertEquals(1, getSimpleLeafListMethodCount);
        assertEquals(1, setSimpleLeafListMethodCount);
        assertEquals(1, getBarMethodCount);

        assertEquals(8, simpleListMethodsCount);
    }

    @Test
    public void testListCompositeKeyResolving() {
        final String filePath = getClass().getResource("/list-composite-key.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);

        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(8, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;

        int compositeKeyListKeyPropertyCount = 0;
        int compositeKeyListKeyCount = 0;
        int innerListKeyPropertyCount = 0;

        for (final Type type : genTypes) {
            if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;

                if (genTO.getName().equals("CompositeKeyListKey")) {
                    compositeKeyListKeyCount++;
                    final List<GeneratedProperty> properties = genTO.getProperties();
                    for (final GeneratedProperty prop : properties) {
                        if (prop.getName().equals("Key1")) {
                            compositeKeyListKeyPropertyCount++;
                        } else if (prop.getName().equals("Key2")) {
                            compositeKeyListKeyPropertyCount++;
                        }
                    }
                    genTOsCount++;
                } else if (genTO.getName().equals("InnerListKey")) {
                    final List<GeneratedProperty> properties = genTO.getProperties();
                    innerListKeyPropertyCount = properties.size();
                    genTOsCount++;
                }
            }
        }
        assertEquals(1, compositeKeyListKeyCount);
        assertEquals(2, compositeKeyListKeyPropertyCount);

        assertEquals(1, innerListKeyPropertyCount);

        assertEquals(6, genTypesCount);
        assertEquals(2, genTOsCount);
    }

    @Test
    public void testGeneratedTypes() {
        final String filePath = getClass().getResource("/demo-topology.yang").getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(filePath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(15, genTypes.size());

        int genTypesCount = 0;
        int genTOsCount = 0;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
                genTypesCount++;
            } else if (type instanceof GeneratedTransferObject) {
                genTOsCount++;
            }
        }

        assertEquals(12, genTypesCount);
        assertEquals(3, genTOsCount);
    }
}
