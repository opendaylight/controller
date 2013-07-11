package org.opendaylight.controller.sal.binding.generator.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.yang.types.BaseYangTypes;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class ExtendedTypedefTest {

    private final static List<File> testModels = new ArrayList<File>();

    @BeforeClass
    public static void loadTestResources() {
        final File listModelFile = new File(ExtendedTypedefTest.class.getResource("/typedef_of_typedef.yang").getPath());
        testModels.add(listModelFile);
    }

    @Test    
    public void constantGenerationTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(testModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        assertNotNull(context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        GeneratedTransferObject simpleTypedef4 = null;
        GeneratedTransferObject extendedTypedefUnion = null;
        GeneratedTransferObject unionTypedef = null;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedTransferObject) {
                if (type.getName().equals("SimpleTypedef4")) {
                    simpleTypedef4 = (GeneratedTransferObject) type;
                } else if (type.getName().equals("ExtendedTypedefUnion")) {
                    extendedTypedefUnion = (GeneratedTransferObject) type;
                } else if (type.getName().equals("UnionTypedef")) {
                    unionTypedef = (GeneratedTransferObject) type;
                }
            }
        }

        // simple-typedef4
        assertNotNull("SimpleTypedef4 not found", simpleTypedef4);
        assertNotNull("ExtendedTypedefUnion not found", extendedTypedefUnion);
        assertNotNull("UnionTypedef", unionTypedef);

        List<GeneratedProperty> properties = simpleTypedef4.getProperties();
        assertTrue("SimpleTypedef4 shouldn't have properties.", properties.isEmpty());

        GeneratedTransferObject extendTO = simpleTypedef4.getExtends();
        assertNotNull("SimpleTypedef4 should have extend.", extendTO);
        assertEquals("Incorrect extension for SimpleTypedef4.", "SimpleTypedef3", extendTO.getName());
        properties = extendTO.getProperties();
        assertTrue("SimpleTypedef3 shouldn't have properties.", properties.isEmpty());

        extendTO = extendTO.getExtends();
        assertNotNull("SimpleTypedef3 should have extend.", extendTO);
        assertEquals("Incorrect extension for SimpleTypedef3.", "SimpleTypedef2", extendTO.getName());
        properties = extendTO.getProperties();
        assertTrue("SimpleTypedef2 shouldn't have properties.", properties.isEmpty());

        extendTO = extendTO.getExtends();
        assertNotNull("SimpleTypedef2 should have extend.", extendTO);
        assertEquals("SimpleTypedef2 should be extended with SimpleTypedef1.", "SimpleTypedef1", extendTO.getName());
        properties = extendTO.getProperties();
        assertEquals("Incorrect number of properties in class SimpleTypedef1.", 1, properties.size());

        assertEquals("Incorrect property's name", "simpleTypedef1", properties.get(0).getName());
        assertEquals("Property's incorrect type", BaseYangTypes.UINT8_TYPE, properties.get(0).getReturnType());

        extendTO = extendTO.getExtends();
        assertNull("SimpleTypedef1 shouldn't have extend.", extendTO);

        // extended-typedef-union
        assertNotNull("ExtendedTypedefUnion object not found", extendedTypedefUnion);
        properties = extendedTypedefUnion.getProperties();
        assertTrue("ExtendedTypedefUnion shouldn't have any property", properties.isEmpty());

        extendTO = extendedTypedefUnion.getExtends();
        assertEquals("Incorrect extension fo ExtendedTypedefUnion.", "UnionTypedef", extendTO.getName());
        assertNull("UnionTypedef shouldn't be extended", extendTO.getExtends());
        assertEquals("Incorrect number of properties for UnionTypedef.", 4, extendTO.getProperties().size());

        GeneratedProperty simpleTypedef4Property = null;
        GeneratedProperty simpleTypedef1Property = null;
        GeneratedProperty byteTypeProperty = null;
        GeneratedProperty typedefEnumFruitProperty = null;
        for (GeneratedProperty genProperty : extendTO.getProperties()) {
            if (genProperty.getName().equals("simpleTypedef1")) {
                simpleTypedef1Property = genProperty;
            } else if (genProperty.getName().equals("simpleTypedef4")) {
                simpleTypedef4Property = genProperty;
            } else if (genProperty.getName().equals("byteType")) {
                byteTypeProperty = genProperty;
            } else if (genProperty.getName().equals("typedefEnumFruit")) {
                typedefEnumFruitProperty = genProperty;
            }
        }

        assertNotNull("simpleTypedef4 property not found in UnionTypedef", simpleTypedef4Property);
        assertNotNull("simpleTypedef1 property not found in UnionTypedef", simpleTypedef1Property);
        assertNotNull("byteType property not found in UnionTypedef", byteTypeProperty);
        assertNotNull("typedefEnumFruit property not found in UnionTypedef", typedefEnumFruitProperty);

        assertEquals("Incorrect type for property simpleTypedef4.", "SimpleTypedef4", simpleTypedef4Property
                .getReturnType().getName());
        assertEquals("Incorrect type for property simpleTypedef1.", "SimpleTypedef1", simpleTypedef1Property
                .getReturnType().getName());
        assertEquals("Incorrect type for property byteType.", "ByteType", byteTypeProperty
                .getReturnType().getName());
        assertEquals("Incorrect type for property typedefEnumFruit.", "TypedefEnumFruit", typedefEnumFruitProperty
                .getReturnType().getName());
    }

}
