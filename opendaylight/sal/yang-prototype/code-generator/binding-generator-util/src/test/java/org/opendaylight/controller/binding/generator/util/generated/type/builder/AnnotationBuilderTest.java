/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

public class AnnotationBuilderTest {

    @Test
    public void generatedTypeAnnotationTest() {
        final GeneratedTypeBuilder genTypeBuilder = new GeneratedTypeBuilderImpl(
                "org.opendaylight.controller", "AnnotInterface");

        genTypeBuilder.addAnnotation("javax.management", "MXBean");
        final AnnotationTypeBuilder annotDesc = genTypeBuilder.addAnnotation(
                "javax.management", "Description");
        annotDesc.addParameter("description", "some sort of interface");

        final GeneratedType genType = genTypeBuilder.toInstance();

        assertNotNull(genType);
        assertNotNull(genType.getAnnotations());
        assertEquals(2, genType.getAnnotations().size());

        int annotCount = 0;
        for (final AnnotationType annotation : genType.getAnnotations()) {
            if (annotation.getPackageName().equals("javax.management")
                    && annotation.getName().equals("MXBean")) {
                annotCount++;
                assertEquals(0, annotation.getParameters().size());
            }
            if (annotation.getPackageName().equals("javax.management")
                    && annotation.getName().equals("Description")) {
                annotCount++;
                assertEquals(1, annotation.getParameters().size());
                AnnotationType.Parameter param = annotation
                        .getParameter("description");
                assertNotNull(param);
                assertEquals("description", param.getName());
                assertNotNull(param.getValue());
                assertEquals("some sort of interface", param.getValue());
                assertNotNull(param.getValues());
                assertTrue(param.getValues().isEmpty());
            }
        }
        assertEquals(2, annotCount);
    }

    @Test
    public void methodSignatureAnnotationTest() {
        final GeneratedTypeBuilder genTypeBuilder = new GeneratedTypeBuilderImpl(
                "org.opendaylight.controller", "TransferObject");

        final MethodSignatureBuilder methodBuilder = genTypeBuilder
                .addMethod("simpleMethod");
        methodBuilder.addReturnType(Types.typeForClass(Integer.class));
        final AnnotationTypeBuilder annotManAttr = methodBuilder
                .addAnnotation("org.springframework.jmx.export.annotation",
                        "ManagedAttribute");

        annotManAttr.addParameter("description", "\"The Name Attribute\"");
        annotManAttr.addParameter("currencyTimeLimit", "20");
        annotManAttr.addParameter("defaultValue", "\"bar\"");
        annotManAttr.addParameter("persistPolicy", "\"OnUpdate\"");

        final AnnotationTypeBuilder annotManProp = methodBuilder
                .addAnnotation("org.springframework.jmx.export.annotation",
                        "ManagedOperation");

        final List<String> typeValues = new ArrayList<String>();
        typeValues.add("\"val1\"");
        typeValues.add("\"val2\"");
        typeValues.add("\"val3\"");
        annotManProp.addParameters("types", typeValues);

        final GeneratedType genType = genTypeBuilder.toInstance();

        assertNotNull(genType);
        assertNotNull(genType.getAnnotations());
        assertNotNull(genType.getMethodDefinitions());
        assertNotNull(genType.getMethodDefinitions().get(0));
        assertNotNull(genType.getMethodDefinitions().get(0).getAnnotations());
        final List<AnnotationType> annotations = genType.getMethodDefinitions()
                .get(0).getAnnotations();
        assertEquals(2, annotations.size());
        
        int annotCount = 0;
        for (final AnnotationType annotation : annotations) {
            if (annotation.getPackageName().equals("org.springframework.jmx.export.annotation")
                    && annotation.getName().equals("ManagedAttribute")) {
                annotCount++;
                assertEquals(4, annotation.getParameters().size());
                
                assertNotNull(annotation.getParameter("description"));
                assertNotNull(annotation.getParameter("currencyTimeLimit"));
                assertNotNull(annotation.getParameter("defaultValue"));
                assertNotNull(annotation.getParameter("persistPolicy"));
                assertEquals("\"The Name Attribute\"", annotation.getParameter("description").getValue());
                assertEquals("20", annotation.getParameter("currencyTimeLimit").getValue());
                assertEquals("\"bar\"", annotation.getParameter("defaultValue").getValue());
                assertEquals("\"OnUpdate\"", annotation.getParameter("persistPolicy").getValue());
            }
            if (annotation.getPackageName().equals("org.springframework.jmx.export.annotation")
                    && annotation.getName().equals("ManagedOperation")) {
                annotCount++;
                
                assertEquals(1, annotation.getParameters().size());
                assertNotNull(annotation.getParameter("types"));
                assertEquals(3, annotation.getParameter("types").getValues().size());
            }
        }
        assertEquals(2, annotCount);
    }

    @Test
    public void generatedPropertyAnnotationTest() {
        final GeneratedTOBuilder genTOBuilder = new GeneratedTOBuilderImpl(
                "org.opendaylight.controller", "AnnotInterface");

        final GeneratedPropertyBuilder propertyBuilder = genTOBuilder
                .addProperty("simpleProperty");
        propertyBuilder.addReturnType(Types.typeForClass(Integer.class));
        final AnnotationTypeBuilder annotManAttr = propertyBuilder
                .addAnnotation("org.springframework.jmx.export.annotation",
                        "ManagedAttribute");

        annotManAttr.addParameter("description", "\"The Name Attribute\"");
        annotManAttr.addParameter("currencyTimeLimit", "20");
        annotManAttr.addParameter("defaultValue", "\"bar\"");
        annotManAttr.addParameter("persistPolicy", "\"OnUpdate\"");

        final AnnotationTypeBuilder annotManProp = propertyBuilder
                .addAnnotation("org.springframework.jmx.export.annotation",
                        "ManagedOperation");

        final List<String> typeValues = new ArrayList<String>();
        typeValues.add("\"val1\"");
        typeValues.add("\"val2\"");
        typeValues.add("\"val3\"");
        annotManProp.addParameters("types", typeValues);

        final GeneratedTransferObject genTransObj = genTOBuilder.toInstance();

        assertNotNull(genTransObj);
        assertNotNull(genTransObj.getAnnotations());
        assertNotNull(genTransObj.getProperties());
        assertNotNull(genTransObj.getProperties().get(0));
        assertNotNull(genTransObj.getProperties().get(0).getAnnotations());
        final List<AnnotationType> annotations = genTransObj.getProperties()
                .get(0).getAnnotations();
        assertEquals(2, annotations.size());
        
        int annotCount = 0;
        for (final AnnotationType annotation : annotations) {
            if (annotation.getPackageName().equals("org.springframework.jmx.export.annotation")
                    && annotation.getName().equals("ManagedAttribute")) {
                annotCount++;
                assertEquals(4, annotation.getParameters().size());
                
                assertNotNull(annotation.getParameter("description"));
                assertNotNull(annotation.getParameter("currencyTimeLimit"));
                assertNotNull(annotation.getParameter("defaultValue"));
                assertNotNull(annotation.getParameter("persistPolicy"));
                assertEquals("\"The Name Attribute\"", annotation.getParameter("description").getValue());
                assertEquals("20", annotation.getParameter("currencyTimeLimit").getValue());
                assertEquals("\"bar\"", annotation.getParameter("defaultValue").getValue());
                assertEquals("\"OnUpdate\"", annotation.getParameter("persistPolicy").getValue());
            }
            if (annotation.getPackageName().equals("org.springframework.jmx.export.annotation")
                    && annotation.getName().equals("ManagedOperation")) {
                annotCount++;
                
                assertEquals(1, annotation.getParameters().size());
                assertNotNull(annotation.getParameter("types"));
                assertEquals(3, annotation.getParameter("types").getValues().size());
            }
        }
        assertEquals(2, annotCount);
    }

    @Test
    public void generatedTransfeObjectAnnotationTest() {
        final GeneratedTOBuilder genTypeBuilder = new GeneratedTOBuilderImpl(
                "org.opendaylight.controller", "AnnotClassCache");

        genTypeBuilder.addAnnotation("javax.management", "MBean");
        final AnnotationTypeBuilder annotNotify = genTypeBuilder.addAnnotation(
                "javax.management", "NotificationInfo");

        final List<String> notifyList = new ArrayList<String>();
        notifyList.add("\"my.notif.type\"");
        annotNotify.addParameters("types", notifyList);
        annotNotify.addParameter("description",
                "@Description(\"my notification\")");

        GeneratedTransferObject genTO = genTypeBuilder.toInstance();

        assertNotNull(genTO);
        assertNotNull(genTO.getAnnotations());
        assertEquals(2, genTO.getAnnotations().size());

        int annotCount = 0;
        for (final AnnotationType annotation : genTO.getAnnotations()) {
            if (annotation.getPackageName().equals("javax.management")
                    && annotation.getName().equals("MBean")) {
                annotCount++;
                assertEquals(0, annotation.getParameters().size());
            }
            if (annotation.getPackageName().equals("javax.management")
                    && annotation.getName().equals("NotificationInfo")) {
                annotCount++;
                assertEquals(2, annotation.getParameters().size());
                AnnotationType.Parameter param = annotation
                        .getParameter("types");
                assertNotNull(param);
                assertEquals("types", param.getName());
                assertNull(param.getValue());
                assertNotNull(param.getValues());
                assertEquals(1, param.getValues().size());
                assertEquals("\"my.notif.type\"", param.getValues().get(0));

                param = annotation.getParameter("description");
                assertNotNull(param);
                assertEquals("description", param.getName());
                assertNotNull(param.getValue());
                assertEquals("@Description(\"my notification\")",
                        param.getValue());
            }
        }
        assertEquals(2, annotCount);
    }
}
