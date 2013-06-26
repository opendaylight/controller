package org.opendaylight.controller.sal.binding.generator.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;

public class GeneratedTypesStringTest {

    private final static List<File> testModels = new ArrayList<File>();

    @BeforeClass
    public static void loadTestResources() {
        final File listModelFile = new File(GeneratedTypesStringTest.class.getResource("/simple-string-demo.yang")
                .getPath());
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

        boolean typedefStringFound = false;
        boolean constantRegExListFound = false;
        boolean constantRegExListTypeGeneric = false;
        boolean constantRegExListTypeContainer = false;
        boolean noStringInReqExListFound = false;
        boolean constantRegExListValueOK = false;
        boolean constantRegExListTypeOneGeneric = false;
        for (final Type type : genTypes) {
            if (type instanceof GeneratedTransferObject) {
                final GeneratedTransferObject genTO = (GeneratedTransferObject) type;

                if (genTO.getName().equals("TypedefString")) {
                    typedefStringFound = true;

                    List<Constant> constants = genTO.getConstantDefinitions();
                    for (Constant con : constants) {
                        if (con.getName().equals("PATTERN_CONSTANTS")) {
                            constantRegExListFound = true;
                        } else
                            break;
                        ParameterizedType pType;
                        if (con.getType() instanceof ParameterizedType) {
                            pType = (ParameterizedType) con.getType();
                        } else
                            break;

                        Type[] types;
                        if (pType.getName().equals("List")) {
                            constantRegExListTypeContainer = true;
                            types = pType.getActualTypeArguments();
                        } else
                            break;

                        if (types.length == 1) {
                            constantRegExListTypeOneGeneric = true;
                        } else
                            break;

                        if (types[0].getName().equals("String")) {
                            constantRegExListTypeGeneric = true;
                        } else
                            break;

                        if (con.getValue() instanceof List) {
                            constantRegExListValueOK = true;
                        } else
                            break;

                        for (Object obj : (List<?>) con.getValue()) {
                            if (!(obj instanceof String)) {
                                noStringInReqExListFound = true;
                                break;
                            }
                        }

                    }
                }
            }

        }

        assertTrue("Typedef >>TypedefString<< wasn't found", typedefStringFound);
        assertTrue("Constant PATTERN_CONSTANTS is missing in TO", constantRegExListFound);
        assertTrue("Constant PATTERN_CONSTANTS doesn't have correct container type", constantRegExListTypeContainer);
        assertTrue("Constant PATTERN_CONSTANTS has more than one generic type", constantRegExListTypeOneGeneric);
        assertTrue("Constant PATTERN_CONSTANTS doesn't have correct generic type", constantRegExListTypeGeneric);
        assertTrue("Constant PATTERN_CONSTANTS doesn't contain List object", constantRegExListValueOK);
        assertTrue("In list found other type than String", !noStringInReqExListFound);

    }

}
