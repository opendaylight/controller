package org.opendaylight.controller.sal.java.api.generator.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.java.api.generator.ClassCodeGenerator;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class ClassCodeGeneratorTest {

    private final static List<File> testModels = new ArrayList<File>();

    @BeforeClass
    public static void loadTestResources() {
        final File listModelFile = new File(ClassCodeGeneratorTest.class
                .getResource("/list-composite-key.yang").getPath());
        testModels.add(listModelFile);
    }

    @Test
    public void compositeKeyClassTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(testModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        assertNotNull(context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context);

        assertTrue(genTypes != null);
        assertEquals(8, genTypes.size());

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

                    final ClassCodeGenerator clsGen = new ClassCodeGenerator();
                    try {
                        final Writer writer = clsGen.generate(genTO);
                        assertNotNull(writer);

                        final String outputStr = writer.toString();
                        writer.close();

                        assertNotNull(outputStr);
                        assertTrue(outputStr
                                .contains("public CompositeKeyListKey(String Key2, "
                                        + "Byte Key1)"));

                    } catch (IOException e) {
                        e.printStackTrace();
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

        assertEquals(6, genTypesCount);
        assertEquals(2, genTOsCount);
    }

    @Test
    public void defaultConstructorTest() {
        final GeneratedTOBuilder toBuilder = new GeneratedTOBuilderImpl(
                "simple.pack", "DefCtor");

        GeneratedPropertyBuilder propBuilder = toBuilder.addProperty("foo");
        propBuilder.setReturnType(Types.typeForClass(String.class));
        propBuilder.setReadOnly(false);

        propBuilder = toBuilder.addProperty("bar");
        propBuilder.setReturnType(Types.typeForClass(Integer.class));
        propBuilder.setReadOnly(false);

        final GeneratedTransferObject genTO = toBuilder.toInstance();

        final ClassCodeGenerator clsGen = new ClassCodeGenerator();
        try {
            final Writer writer = clsGen.generate(genTO);
            assertNotNull(writer);

            final String outputStr = writer.toString();
            writer.close();

            assertNotNull(outputStr);
            assertTrue(outputStr.contains("public DefCtor()"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void toStringTest() {
        final GeneratedTOBuilder toBuilder = new GeneratedTOBuilderImpl(
                "simple.pack", "DefCtor");

        GeneratedPropertyBuilder propBuilder = toBuilder.addProperty("foo");
        propBuilder.setReturnType(Types.typeForClass(String.class));
        propBuilder.setReadOnly(false);
        toBuilder.addToStringProperty(propBuilder);

        propBuilder = toBuilder.addProperty("bar");
        propBuilder.setReturnType(Types.typeForClass(Integer.class));
        propBuilder.setReadOnly(false);
        toBuilder.addToStringProperty(propBuilder);
        final GeneratedTransferObject genTO = toBuilder.toInstance();
        final ClassCodeGenerator clsGen = new ClassCodeGenerator();
        try {
            final Writer writer = clsGen.generate(genTO);
            assertNotNull(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
