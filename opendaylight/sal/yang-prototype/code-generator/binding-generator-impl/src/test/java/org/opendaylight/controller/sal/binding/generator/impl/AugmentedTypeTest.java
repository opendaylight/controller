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

import org.junit.BeforeClass;
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

public class AugmentedTypeTest {

    private final static List<File> augmentModels = new ArrayList<>();
    private final static String augmentFolderPath = AugmentedTypeTest.class
            .getResource("/augment-test-models").getPath();

    @BeforeClass
    public static void loadTestResources() {
        final File augFolder = new File(augmentFolderPath);

        for (final File fileEntry : augFolder.listFiles()) {
            if (fileEntry.isFile()) {
                augmentModels.add(fileEntry);
            }
        }
    }

    @Test
    public void augmentedAbstractTopologyTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(augmentModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        assertNotNull(context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertNotNull(genTypes);
        assertTrue(!genTypes.isEmpty());

        int resolvedAugmentsCount = 0;
        for (final Type type : genTypes) {
            assertNotNull(type);
            if (type.getName().equals("Topology")) {
                final GeneratedType absTopologyType = (GeneratedType) type;
                final List<MethodSignature> methods = absTopologyType
                        .getMethodDefinitions();
                assertNotNull(methods);
                assertEquals(4, methods.size());
            } else if (type.getName().equals("InterfaceKey")
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
                        resolvedAugmentsCount++;
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
                        resolvedAugmentsCount++;
                    } else if (method.getName().equals("getHigherLayerIf")) {
                        assertTrue(method.getReturnType() != null);
                        assertFalse(method.getReturnType().equals(
                                "java.lang.Void"));
                        assertTrue(method.getReturnType().getName()
                                .equals("List"));
                        resolvedAugmentsCount++;
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
                        resolvedAugmentsCount++;
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
                        resolvedAugmentsCount++;
                    }
                }
            } else if (type.getName().equals("NetworkLink2")
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
                        resolvedAugmentsCount++;
                    }
                }
            }
        }
        assertEquals(7, resolvedAugmentsCount);
    }

    @Test
    public void augmentedNetworkLinkTest() {

    }

    @Test
    public void augmentedTopologyTunnelsTest() {

    }
}
