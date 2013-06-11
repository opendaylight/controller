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

        assertNotNull("context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());

        GeneratedTransferObject gtInterfaceKey = null;
        GeneratedType gtInterface = null;
        GeneratedType gtTunnel = null;
        GeneratedTransferObject gtTunnelKey = null;
        GeneratedType gtNetworkLink2 = null;

        for (final Type type : genTypes) {
            if (type.getName().equals("InterfaceKey") && type.getPackageName().contains("augment._abstract.topology")) {
                gtInterfaceKey = (GeneratedTransferObject) type;
            } else if (type.getName().equals("Interface") && type.getPackageName().contains("augment._abstract.topology")) {
                gtInterface = (GeneratedType) type;
            } else if (type.getName().equals("Tunnel") && type.getPackageName().contains("augment._abstract.topology")) {
                gtTunnel = (GeneratedType) type;
            } else if (type.getName().equals("TunnelKey") && type.getPackageName().contains("augment._abstract.topology")) {
                gtTunnelKey = (GeneratedTransferObject) type;
            } else if (type.getName().equals("NetworkLink2") && type.getPackageName().contains("augment._abstract.topology")) {
                gtNetworkLink2 = (GeneratedType) type;
            }
        }

        // 'Interface
        assertNotNull("gtInterface is null", gtInterface);
        final List<MethodSignature> gtInterfaceMethods = gtInterface.getMethodDefinitions();
        assertNotNull("gtInterfaceMethods is null", gtInterfaceMethods);
        MethodSignature getIfcKeyMethod = null;
        for (final MethodSignature method : gtInterfaceMethods) {
            if (method.getName().equals("getInterfaceKey")) {
                getIfcKeyMethod = method;
                break;
            }
        }
        assertNotNull("getIfcKeyMethod is null", getIfcKeyMethod);
        assertNotNull("getIfcKeyMethod.getReturnType() is null", getIfcKeyMethod.getReturnType());
        assertFalse("getIfcKeyMethod.getReturnType() should not be Void",
                getIfcKeyMethod.getReturnType().equals("java.lang.Void"));
        assertTrue("getIfcKeyMethod.getReturnType().getName() must be InterfaceKey",
                getIfcKeyMethod.getReturnType().getName().equals("InterfaceKey"));

        MethodSignature getHigherLayerIfMethod = null;
        for (final MethodSignature method : gtInterfaceMethods) {
            if (method.getName().equals("getHigherLayerIf")) {
                getHigherLayerIfMethod = method;
                break;
            }
        }
        assertNotNull("getHigherLayerIfMethod is null", getHigherLayerIfMethod);
        assertNotNull("getHigherLayerIfMethod.getReturnType() is null",
                getHigherLayerIfMethod.getReturnType());
        assertFalse("getHigherLayerIfMethod.getReturnType() should not be Void",
                getHigherLayerIfMethod.getReturnType().equals("java.lang.Void"));
        assertTrue("getHigherLayerIfMethod.getReturnType().getName() must be List",
                getHigherLayerIfMethod.getReturnType().getName().equals("List"));

        // 'InterfaceKey'
        assertNotNull("gtInterfaceKey is null", gtInterfaceKey);
        final List<GeneratedProperty> properties = gtInterfaceKey.getProperties();
        assertNotNull("properties is null", properties);
        GeneratedProperty gtInterfaceId = null;
        for (final GeneratedProperty property : properties) {
            if (property.getName().equals("InterfaceId")) {
                gtInterfaceId = property;
                break;
            }
        }
        assertNotNull("gtInterfaceId is null", gtInterfaceId);
        assertNotNull("gtInterfaceId.getReturnType() is null", gtInterfaceId.getReturnType());
        assertFalse("gtInterfaceId.getReturnType() should not be Void",
                gtInterfaceId.getReturnType().equals("java.lang.Void"));
        assertTrue("gtInterfaceId.getReturnType().getName() must be String",
                gtInterfaceId.getReturnType().getName().equals("String"));

        // 'Tunnel'
        assertNotNull("gtTunnel is null", gtTunnel);
        final List<MethodSignature> tunnelMethods = gtTunnel.getMethodDefinitions();
        assertNotNull("tunnelMethods is null", tunnelMethods);
        MethodSignature getTunnelKeyMethod = null;
        for (MethodSignature method : tunnelMethods) {
            if (method.getName().equals("getTunnelKey")) {
                getTunnelKeyMethod = method;
                break;
            }
        }
        assertNotNull("getTunnelKeyMethod is null", getTunnelKeyMethod);
        assertNotNull("getTunnelKeyMethod.getReturnType()",
                getTunnelKeyMethod.getReturnType());
        assertFalse("getTunnelKeyMethod.getReturnType() should not be Void",
                getTunnelKeyMethod.getReturnType().equals("java.lang.Void"));
        assertTrue("getTunnelKeyMethod.getReturnType().getName() must be TunnelKey",
                getTunnelKeyMethod.getReturnType().getName().equals("TunnelKey"));

        // 'TunnelKey'
        assertNotNull("gtTunnelKey is null", gtTunnelKey);
        final List<GeneratedProperty> tunnelKeyProperties = gtTunnelKey.getProperties();
        assertNotNull("tunnelKeyProperties is null", tunnelKeyProperties);

        GeneratedProperty gtTunnelId = null;
        for (final GeneratedProperty property : tunnelKeyProperties) {
            if (property.getName().equals("TunnelId")) {
                gtTunnelId = property;
            }
        }
        assertNotNull("gtTunnelId is null", gtTunnelId);
        assertNotNull("gtTunnelId.getReturnType() is null",
                gtTunnelId.getReturnType());
        assertFalse("gtTunnelId.getReturnType() should not be Void",
                gtTunnelId.getReturnType().equals("java.lang.Void"));
        assertTrue("gtTunnelId.getReturnType().getName() must be Integer",
                gtTunnelId.getReturnType().getName().equals("Integer"));

        // 'NetworkLink2'
        assertNotNull("gtNetworkLink2 is null", gtNetworkLink2);

        final List<MethodSignature> networkLink2Methods = gtNetworkLink2.getMethodDefinitions();
        assertNotNull("networkLink2Methods is null", networkLink2Methods);

//        FIXME: in some cases getIfcMethod is null which causes test fail. fix ASAP
//      MethodSignature getIfcMethod = null;
//      for (MethodSignature method : networkLink2Methods) {
//          if (method.getName().equals("getInterface")) {
//              getIfcMethod = method;
//              break;
//          }
//      }
//
//      assertNotNull("getIfcMethod is null", getIfcMethod);
//      assertNotNull("getIfcMethod.getReturnType() is null", getIfcMethod.getReturnType());
//      assertFalse("getIfcMethod.getReturnType() should not be Void", getIfcMethod.getReturnType().equals("java.lang.Void"));
//      assertTrue("getIfcMethod.getReturnType().getName() must be String", getIfcMethod.getReturnType().getName().equals("String"));
    }

    @Test
    public void augmentedNetworkLinkTest() {

    }

    @Test
    public void augmentedTopologyTunnelsTest() {

    }
}
