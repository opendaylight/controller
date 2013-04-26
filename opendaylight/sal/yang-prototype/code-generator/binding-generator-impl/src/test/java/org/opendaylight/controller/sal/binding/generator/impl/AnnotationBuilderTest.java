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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;

public class AnnotationBuilderTest {

    @Test
    public void generatedTypeAnnotationTest() {
        GeneratedTypeBuilder genTypeBuilder = new GeneratedTOBuilderImpl(
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
                AnnotationType.Parameter param = annotation.getParameter("description");
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
        //TODO  add test for method annotations
    }

    @Test
    public void generatedPropertyAnnotationTest() {
        //TODO add test for property annotations
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
                AnnotationType.Parameter param = annotation.getParameter("types");
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
                assertEquals("@Description(\"my notification\")", param.getValue());
            }
        }
        assertEquals(2, annotCount);
    }
}
