/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

public class AnnotationsTest {

    private final String setSomethingString = "setSomething";

    private void assertRequireInterfaceAnnotationHasCorrectValue(
            Class<?> clazz, String methodName,
            Set<Class<?>> inspectedInterfaces,
            Class<? extends AbstractServiceInterface> expectedValue) {
        Method setter = findMethod(clazz, methodName);
        RequireInterface found = AttributeHolder
                .findRequireInterfaceAnnotation(setter, inspectedInterfaces);
        if (expectedValue == null) {
            assertNull(found);
        } else {
            assertNotNull(found);
            assertEquals(expectedValue, found.value());
        }
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        Method setter;
        try {
            setter = clazz.getMethod(methodName,
                    new Class[] { ObjectName.class });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return setter;
    }

    private void assertDescription(Class<?> clazz, String methodName,
            Set<Class<?>> exportedInterfaces, String expectedValue) {
        Method setter = findMethod(clazz, methodName);
        String found = AttributeHolder.findDescription(setter,
                exportedInterfaces);
        if (expectedValue == null) {
            assertNull(found);
        } else {
            assertNotNull(found);
            assertEquals(expectedValue, found);
        }
    }

    private void assertDescriptionOnClass(Class<?> clazz,
            Set<Class<?>> jmxInterfaces, String expectedValue) {
        String found = AbstractDynamicWrapper.findDescription(clazz,
                jmxInterfaces);
        if (expectedValue == null) {
            assertNull(found);
        } else {
            assertNotNull(found);
            assertEquals(expectedValue, found);
        }
    }

    private void assertNoDescriptionOnClass(Class<?> clazz,
            Set<Class<?>> jmxInterfaces) {
        String found = AbstractDynamicWrapper.findDescription(clazz,
                jmxInterfaces);
        assertTrue(found.isEmpty());
    }

    static final String SIMPLE = "simple";
    static final String SUBCLASS2 = "subclass2";

    @ServiceInterfaceAnnotation(value = SIMPLE, osgiRegistrationType = Executor.class)
    static interface SimpleSI extends AbstractServiceInterface {

    }

    @Description("class")
    public static class SuperClass {
        @RequireInterface(SimpleSI.class)
        @Description("descr")
        public void setSomething(ObjectName objectName) {

        }
    }

    private static Set<Class<?>> emptySetOfInterfaces() {
        return Collections.emptySet();
    }

    @Test
    public void testFindAnnotation_directly() throws Exception {
        assertRequireInterfaceAnnotationHasCorrectValue(SuperClass.class,
                setSomethingString, emptySetOfInterfaces(), SimpleSI.class);
        assertDescription(SuperClass.class, setSomethingString,
                emptySetOfInterfaces(), "descr");
        assertDescriptionOnClass(SuperClass.class, emptySetOfInterfaces(),
                "class");
    }

    public static class SubClassWithout extends SuperClass {

    }

    @Test
    public void testFindAnnotation_subclassWithout() throws Exception {
        assertRequireInterfaceAnnotationHasCorrectValue(SubClassWithout.class,
                setSomethingString, emptySetOfInterfaces(), SimpleSI.class);
        assertDescription(SubClassWithout.class, setSomethingString,
                emptySetOfInterfaces(), "descr");
        assertDescriptionOnClass(SuperClass.class, emptySetOfInterfaces(),
                "class");
    }

    public static class SubClassWithEmptyMethod extends SuperClass {
        @Override
        public void setSomething(ObjectName objectName) {

        }
    }

    @Test
    public void testOverridingWithoutAnnotation() throws Exception {
        assertRequireInterfaceAnnotationHasCorrectValue(
                SubClassWithEmptyMethod.class, setSomethingString,
                emptySetOfInterfaces(), SimpleSI.class);
        assertDescription(SubClassWithEmptyMethod.class, setSomethingString,
                emptySetOfInterfaces(), "descr");
        assertDescriptionOnClass(SubClassWithEmptyMethod.class,
                emptySetOfInterfaces(), "class");
    }

    static interface SubSI extends SimpleSI {

    }

    @ServiceInterfaceAnnotation(value = SUBCLASS2, osgiRegistrationType = ExecutorService.class)
    static interface SubSI2 extends SubSI {

    }

    public static class SubClassWithAnnotation extends SuperClass {
        @Override
        @RequireInterface(SubSI2.class)
        @Description("descr2")
        public void setSomething(ObjectName objectName) {

        }
    }

    @Test
    public void testFindAnnotation_SubClassWithAnnotation() throws Exception {
        assertDescription(SubClassWithAnnotation.class, setSomethingString,
                emptySetOfInterfaces(), "descr2\ndescr");
        try {
            assertRequireInterfaceAnnotationHasCorrectValue(
                    SubClassWithAnnotation.class, setSomethingString,
                    emptySetOfInterfaces(), SubSI2.class);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage()
                            .startsWith("Error finding @RequireInterface. More than one value specified"));
        }
    }

    public static interface HasSomeMethod {
        void setSomething(ObjectName objectName);
    }

    public static class SubClassWithoutMethodWithInterface extends SuperClass
            implements HasSomeMethod {

    }

    @Test
    public void testFindAnnotation_SubClassWithoutMethodWithInterface()
            throws Exception {
        assertRequireInterfaceAnnotationHasCorrectValue(
                SubClassWithoutMethodWithInterface.class, setSomethingString,
                emptySetOfInterfaces(), SimpleSI.class);
        assertDescription(SubClassWithoutMethodWithInterface.class,
                setSomethingString, emptySetOfInterfaces(), "descr");
    }

    static abstract class SuperClassWithInterface implements HasSomeMethod {
        @Override
        @RequireInterface(SubSI2.class)
        @Description("descr")
        public void setSomething(ObjectName objectName) {

        }
    }

    @Description("class")
    public static class SubClassOfSuperClassWithInterface extends
            SuperClassWithInterface {

    }

    @Test
    public void testFindAnnotation_SubClassOfSuperClassWithInterface()
            throws Exception {
        assertRequireInterfaceAnnotationHasCorrectValue(
                SubClassOfSuperClassWithInterface.class, setSomethingString,
                emptySetOfInterfaces(), SubSI2.class);
        assertDescription(SubClassOfSuperClassWithInterface.class,
                setSomethingString, emptySetOfInterfaces(), "descr");
        assertDescriptionOnClass(SubClassOfSuperClassWithInterface.class,
                emptySetOfInterfaces(), "class");
    }

    @Test
    public void testFindAnnotation2() throws Exception {
        assertNoDescriptionOnClass(SuperClassWithInterface.class,
                emptySetOfInterfaces());
    }

    @Description("class")
    static interface HasSomeMethodWithAnnotations {

        @RequireInterface(SubSI2.class)
        @Description("descr")
        void setSomething(ObjectName objectName);
    }

    static class HasSomeMethodWithAnnotationsImpl implements
            HasSomeMethodWithAnnotations {
        @Override
        public void setSomething(ObjectName objectName) {
        }

    }

    @Test
    public void testHasSomeMethodWithAnnotationsImpl() {
        HashSet<Class<?>> exportedInterfaces = Sets
                .<Class<?>> newHashSet(HasSomeMethodWithAnnotations.class);
        assertRequireInterfaceAnnotationHasCorrectValue(
                HasSomeMethodWithAnnotationsImpl.class, setSomethingString,
                exportedInterfaces, SubSI2.class);

        assertDescription(HasSomeMethodWithAnnotationsImpl.class,
                setSomethingString, exportedInterfaces, "descr");

        assertDescriptionOnClass(
                HasSomeMethodWithAnnotationsImpl.class,
                new HashSet<Class<?>>(Arrays
                        .asList(HasSomeMethodWithAnnotations.class)), "class");
    }

}
