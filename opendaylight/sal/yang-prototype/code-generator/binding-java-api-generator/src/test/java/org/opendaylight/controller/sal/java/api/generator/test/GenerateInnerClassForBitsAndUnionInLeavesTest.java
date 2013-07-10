package org.opendaylight.controller.sal.java.api.generator.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.java.api.generator.ClassCodeGenerator;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class GenerateInnerClassForBitsAndUnionInLeavesTest {

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
    public void testInnerClassCreationForBitsAndUnionsInLeafes() {
        final String yangTypesPath = getClass().getResource("/bit_and_union_in_leaf.yang").getPath();

        final SchemaContext context = resolveSchemaContextFromFiles(yangTypesPath);
        assertTrue(context != null);

        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);
        assertTrue(genTypes != null);

        boolean parentContainerFound = false;
        boolean bitLeafTOFound = false;
        boolean unionLeafTOFound = false;

        boolean firstBitPropertyFound = false;
        boolean secondBitPropertyFound = false;
        boolean thirdBitPropertyFound = false;

        boolean firstBitPropertyTypeOK = false;
        boolean secondBitPropertyTypeOK = false;
        boolean thirdBitPropertyTypeOK = false;

        boolean int32UnionPropertyFound = false;
        boolean int32UnionPropertyTypeOK = false;
        boolean stringUnionPropertyFound = false;
        boolean stringUnionPropertyTypeOK = false;
        boolean uint8UnionPropertyFound = false;
        boolean uint8UnionPropertyTypeOK = false;

        for (Type type : genTypes) {
            if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
                if (type.getName().equals("ParentContainer")) {
                    parentContainerFound = true;
                    GeneratedType parentContainer = (GeneratedType) type;
                    List<GeneratedType> enclosedTypes = parentContainer.getEnclosedTypes();
                    for (GeneratedType genType : enclosedTypes) {
                        if (genType instanceof GeneratedTransferObject) {
                            if (genType.getName().equals("BitLeaf")) {
                                bitLeafTOFound = true;
                                GeneratedTransferObject bitLeafTO = (GeneratedTransferObject) genType;

                                List<GeneratedProperty> bitLeafProperties = bitLeafTO.getProperties();
                                for (GeneratedProperty bitLeafProperty : bitLeafProperties) {
                                    String bitLeafPropertyType = bitLeafProperty.getReturnType().getName();
                                    if (bitLeafProperty.getName().equals("firstBit")) {
                                        firstBitPropertyFound = true;
                                        if (bitLeafPropertyType.equals("Boolean")) {
                                            firstBitPropertyTypeOK = true;
                                        }
                                    } else if (bitLeafProperty.getName().equals("secondBit")) {
                                        secondBitPropertyFound = true;
                                        if (bitLeafPropertyType.equals("Boolean")) {
                                            secondBitPropertyTypeOK = true;
                                        }
                                    } else if (bitLeafProperty.getName().equals("thirdBit")) {
                                        thirdBitPropertyFound = true;
                                        if (bitLeafPropertyType.equals("Boolean")) {
                                            thirdBitPropertyTypeOK = true;
                                        }
                                    }

                                }

                            } else if (genType.getName().equals("UnionLeaf")) {
                                unionLeafTOFound = true;
                                GeneratedTransferObject unionLeafTO = (GeneratedTransferObject) genType;

                                List<GeneratedProperty> unionLeafProperties = unionLeafTO.getProperties();
                                for (GeneratedProperty unionLeafProperty : unionLeafProperties) {
                                    String unionLeafPropertyType = unionLeafProperty.getReturnType().getName();
                                    if (unionLeafProperty.getName().equals("int32")) {
                                        int32UnionPropertyFound = true;
                                        if (unionLeafPropertyType.equals("Integer")) {
                                            int32UnionPropertyTypeOK = true;
                                        }
                                    } else if (unionLeafProperty.getName().equals("string")) {
                                        stringUnionPropertyFound = true;
                                        if (unionLeafPropertyType.equals("String")) {
                                            stringUnionPropertyTypeOK = true;
                                        }
                                    } else if (unionLeafProperty.getName().equals("uint8")) {
                                        uint8UnionPropertyFound = true;
                                        if (unionLeafPropertyType.equals("Short")) {
                                            uint8UnionPropertyTypeOK = true;
                                        }
                                    }

                                }

                            }
                        }
                    }
                }
            }
        }
        assertTrue(parentContainerFound);

        assertTrue(bitLeafTOFound);
        assertTrue(firstBitPropertyFound);
        assertTrue(secondBitPropertyFound);
        assertTrue(thirdBitPropertyFound);

        assertTrue(firstBitPropertyTypeOK);
        assertTrue(secondBitPropertyTypeOK);
        assertTrue(thirdBitPropertyTypeOK);

        assertTrue(unionLeafTOFound);
        assertTrue(int32UnionPropertyFound);
        assertTrue(int32UnionPropertyTypeOK);
        assertTrue(stringUnionPropertyFound);
        assertTrue(stringUnionPropertyTypeOK);
        assertTrue(uint8UnionPropertyFound);
        assertTrue(uint8UnionPropertyTypeOK);

    }
}