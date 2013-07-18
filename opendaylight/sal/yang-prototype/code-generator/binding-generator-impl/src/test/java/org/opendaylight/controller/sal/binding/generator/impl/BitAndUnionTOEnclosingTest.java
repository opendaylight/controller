/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class BitAndUnionTOEnclosingTest {

    private final static List<File> testModels = new ArrayList<File>();

    @BeforeClass
    public static void loadTestResources() {
        final File listModelFile = new File(ExtendedTypedefTest.class.getResource("/bit_and_union_in_leaf.yang")
                .getPath());
        testModels.add(listModelFile);
    }

    @Test
    public void bitAndUnionEnclosingTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(testModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        assertNotNull(context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        GeneratedType parentContainer = null;

        for (Type type : genTypes) {
            if ((type instanceof GeneratedType) && !(type instanceof GeneratedTransferObject)) {
                GeneratedType genTO = (GeneratedType) type;
                if (genTO.getName().equals("ParentContainer")) {
                    parentContainer = genTO;
                    break;
                }
            }
        }

        assertNotNull("Parent container object wasn't found.", parentContainer);

        GeneratedTransferObject bitLeaf = null;
        GeneratedTransferObject unionLeaf = null;
        List<GeneratedType> enclosedTypes = parentContainer.getEnclosedTypes();
        for (GeneratedType genType : enclosedTypes) {
            if (genType instanceof GeneratedTransferObject) {
                if (genType.getName().equals("BitLeaf")) {
                    bitLeaf = (GeneratedTransferObject) genType;
                } else if (genType.getName().equals("UnionLeaf")) {
                    unionLeaf = (GeneratedTransferObject) genType;
                }
            }
        }

        assertNotNull("BitLeaf TO builder wasn't found.", bitLeaf);
        assertNotNull("UnionLeaf TO builder wasn't found.", unionLeaf);

        assertEquals("BitLeaf has incorrect package name.",
                "org.opendaylight.yang.gen.v1.urn.bit.union.in.leaf.rev2013626.ParentContainer",
                bitLeaf.getPackageName());
        assertEquals("UnionLeaf has incorrect package name.",
                "org.opendaylight.yang.gen.v1.urn.bit.union.in.leaf.rev2013626.ParentContainer",
                bitLeaf.getPackageName());

        List<GeneratedProperty> propertiesBitLeaf = bitLeaf.getProperties();
        GeneratedProperty firstBitProperty = null;
        GeneratedProperty secondBitProperty = null;
        GeneratedProperty thirdBitProperty = null;
        for (GeneratedProperty genProperty : propertiesBitLeaf) {
            if (genProperty.getName().equals("firstBit")) {
                firstBitProperty = genProperty;
            } else if (genProperty.getName().equals("secondBit")) {
                secondBitProperty = genProperty;
            } else if (genProperty.getName().equals("thirdBit")) {
                thirdBitProperty = genProperty;
            }
        }

        assertNotNull("firstBit property wasn't found", firstBitProperty);
        assertNotNull("secondBit property wasn't found", secondBitProperty);
        assertNotNull("thirdBit property wasn't found", thirdBitProperty);

        assertEquals("firstBit property has incorrect type", "Boolean", firstBitProperty.getReturnType().getName());
        assertEquals("secondBit property has incorrect type", "Boolean", secondBitProperty.getReturnType().getName());
        assertEquals("thirdBit property has incorrect type", "Boolean", thirdBitProperty.getReturnType().getName());

        GeneratedProperty uint32Property = null;
        GeneratedProperty stringProperty = null;
        GeneratedProperty uint8Property = null;
        List<GeneratedProperty> propertiesUnionLeaf = unionLeaf.getProperties();
        for (GeneratedProperty genProperty : propertiesUnionLeaf) {
            if (genProperty.getName().equals("int32")) {
                uint32Property = genProperty;
            } else if (genProperty.getName().equals("string")) {
                stringProperty = genProperty;
            } else if (genProperty.getName().equals("uint8")) {
                uint8Property = genProperty;
            }
        }

        assertNotNull("uint32 property wasn't found", uint32Property);
        assertNotNull("string property wasn't found", stringProperty);
        assertNotNull("uint8 property wasn't found", uint8Property);

        assertEquals("uint32 property has incorrect type", "Integer", uint32Property.getReturnType().getName());
        assertEquals("string property has incorrect type", "String", stringProperty.getReturnType().getName());
        assertEquals("uint8 property has incorrect type", "Short", uint8Property.getReturnType().getName());

    }
}
