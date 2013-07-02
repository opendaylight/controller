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
import org.opendaylight.controller.binding.generator.util.ReferencedTypeImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class GenEnumResolvingTest {

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

        assertEquals("Expected count of all Generated Types from yang models " +
                "is 22", 25, genTypes.size());

        GeneratedType genInterface = null;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType) {
                if (type.getName().equals("Interface")) {
                    genInterface = (GeneratedType) type;
                }
            }
        }
        assertNotNull("Generated Type Interface is not present in list of " +
                "Generated Types", genInterface);

        Enumeration linkUpDownTrapEnable = null;
        Enumeration operStatus = null;
        final List<Enumeration> enums = genInterface.getEnumerations();
        assertNotNull("Generated Type Interface cannot contain NULL reference" +
                " to Enumeration types!", enums);
        assertEquals("Generated Type Interface MUST contain 2 Enumeration " +
                "Types", 2, enums.size());
        for (final Enumeration e : enums) {
            if (e.getName().equals("LinkUpDownTrapEnable")) {
                linkUpDownTrapEnable = e;
            } else if (e.getName().equals("OperStatus")) {
                operStatus = e;
            }
        }

        assertNotNull("Expected Enum LinkUpDownTrapEnable, but was NULL!",
                linkUpDownTrapEnable);
        assertNotNull("Expected Enum OperStatus, but was NULL!", operStatus);

        assertNotNull("Enum LinkUpDownTrapEnable MUST contain Values definition " +
                "not NULL reference!", linkUpDownTrapEnable.getValues());
        assertNotNull("Enum OperStatus MUST contain Values definition not " +
                "NULL reference!", operStatus.getValues());
        assertEquals("Enum LinkUpDownTrapEnable MUST contain 2 values!", 2,
                linkUpDownTrapEnable.getValues().size());
        assertEquals("Enum OperStatus MUST contain 7 values!", 7,
                operStatus.getValues().size());

        final List<MethodSignature> methods = genInterface
                .getMethodDefinitions();

        assertNotNull("Generated Interface cannot contain NULL reference for " +
                "Method Signature Definitions!", methods);

        assertEquals("Expected count of method signature definitions is 14",
                14, methods.size());
        Enumeration ianaIfType = null;
        for (final MethodSignature method : methods) {
            if (method.getName().equals("getType")) {
                if (method.getReturnType() instanceof Enumeration) {
                    ianaIfType = (Enumeration)method.getReturnType();
                }
            }
        }

        assertNotNull("Method getType MUST return Enumeration Type, " +
                "not NULL reference!", ianaIfType);
        assertEquals("Enumeration getType MUST contain 272 values!", 272,
                ianaIfType.getValues().size());
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
        assertEquals(3, genTypes.size());

        final Type type = genTypes.get(1);
        assertTrue(type instanceof Enumeration);

        final Enumeration enumer = (Enumeration) type;
        assertEquals("Enumeration type MUST contain 272 values!", 272,
                enumer.getValues().size());
    }

    @Test
    public void testLeafrefEnumResolving() {
        final String ietfInterfacesPath = getClass().getResource(
                "/enum-test-models/ietf-interfaces@2012-11-15.yang").getPath();
        final String ifTypePath = getClass().getResource(
                "/enum-test-models/iana-if-type@2012-06-05.yang").getPath();
        final String yangTypesPath = getClass().getResource(
                "/enum-test-models/ietf-yang-types@2010-09-24.yang").getPath();
        final String topologyPath = getClass().getResource(
                "/enum-test-models/abstract-topology@2013-02-08.yang")
                .getPath();
        final String inetTypesPath = getClass().getResource(
                "/enum-test-models/ietf-inet-types@2010-09-24.yang")
                .getPath();
        final SchemaContext context = resolveSchemaContextFromFiles(
                ietfInterfacesPath, ifTypePath, yangTypesPath, topologyPath,
                inetTypesPath);

        assertNotNull(context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);
        assertNotNull(genTypes);
        assertTrue(!genTypes.isEmpty());

        GeneratedType genInterface = null;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedType) {
                if (type.getPackageName().equals("org.opendaylight.yang.gen.v1.urn.model._abstract.topology.rev201328.topology.interfaces")
                        && type.getName().equals("Interface")) {
                    genInterface = (GeneratedType) type;
                }
            }
        }
        assertNotNull("Generated Type Interface is not present in list of " +
                "Generated Types", genInterface);

        Type linkUpDownTrapEnable = null;
        Type operStatus = null;
        final List<MethodSignature> methods = genInterface.getMethodDefinitions();
        assertNotNull("Generated Type Interface cannot contain NULL reference" +
                " to Enumeration types!", methods);
        assertEquals("Generated Type Interface MUST contain 4 Methods ",
                4, methods.size());
        for (final MethodSignature method : methods) {
            if (method.getName().equals("getLinkUpDownTrapEnable")) {
                linkUpDownTrapEnable = method.getReturnType();
            } else if (method.getName().equals("getOperStatus")) {
                operStatus = method.getReturnType();
            }
        }

        assertNotNull("Expected Referenced Enum LinkUpDownTrapEnable, but was NULL!",
                linkUpDownTrapEnable);
        assertTrue("Expected LinkUpDownTrapEnable of type ReferencedTypeImpl",
                linkUpDownTrapEnable instanceof ReferencedTypeImpl);
        assertEquals(linkUpDownTrapEnable.getPackageName(),
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev20121115.interfaces.Interface");

        assertNotNull("Expected Referenced Enum OperStatus, but was NULL!",
                operStatus);
        assertTrue("Expected OperStatus of type ReferencedTypeImpl",
                operStatus instanceof  ReferencedTypeImpl);
        assertEquals(operStatus.getPackageName(),
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev20121115.interfaces.Interface");
    }
}
