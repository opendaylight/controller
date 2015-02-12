/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.management.MXBean;
import org.junit.Test;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingScheduledThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.yangtools.concepts.Identifiable;

public class InterfacesHelperTest {

    public interface SuperA {

    }

    public interface SuperBMXBean {

    }

    public interface SuperC extends SuperA, SuperBMXBean {

    }

    public class SuperClass implements SuperC {

    }

    @MXBean
    public interface SubA {

    }

    @ServiceInterfaceAnnotation(value = "a", osgiRegistrationType = SuperA.class, namespace = "n", revision = "r", localName = "l")
    public interface Service extends AbstractServiceInterface{}
    @ServiceInterfaceAnnotation(value = "b", osgiRegistrationType = SuperC.class, namespace = "n", revision = "r", localName = "l")
    public interface SubService extends Service{}

    public abstract class SubClass extends SuperClass implements SubA, Module {

    }

    public abstract class SubClassWithService implements SubService, Module {

    }

    @Test
    public void testGetAllInterfaces() {
        Set<Class<?>> expected = Sets.<Class<?>> newHashSet(SuperA.class, SuperBMXBean.class, SuperC.class,
                SubA.class, Identifiable.class, Module.class);
        assertEquals(expected,
                InterfacesHelper.getAllInterfaces(SubClass.class));
    }

    @Test
    public void testGetServiceInterfaces() throws Exception {
        assertEquals(Collections.<Class<?>>emptySet(), InterfacesHelper.getServiceInterfaces(SubClass.class));
        assertEquals(Sets.<Class<?>>newHashSet(Service.class, SubService.class), InterfacesHelper.getServiceInterfaces(SubClassWithService.class));
    }

    @Test
    public void testGetOsgiRegistrationTypes() throws Exception {
        assertEquals(Collections.<Class<?>>emptySet(), InterfacesHelper.getOsgiRegistrationTypes(SubClass.class));
        assertEquals(Sets.<Class<?>>newHashSet(SuperA.class, SuperC.class),
                InterfacesHelper.getOsgiRegistrationTypes(SubClassWithService.class));
    }

    @Test
    public void testGetMXInterfaces() {
        Set<Class<?>> expected = Sets.<Class<?>> newHashSet(SuperBMXBean.class, SubA.class);
        assertEquals(expected, InterfacesHelper.getMXInterfaces(SubClass.class));
    }

    @Test
    public void testGetAllAbstractServiceInterfaceClasses(){
        Class<? extends AbstractServiceInterface> clazz = TestingScheduledThreadPoolServiceInterface.class;
        Set<Class<? extends AbstractServiceInterface>> input = new HashSet<>();
        input.add(clazz);
        Set<Class<? extends AbstractServiceInterface>> result = InterfacesHelper.getAllAbstractServiceInterfaceClasses(input);

        Set<Class<?>> expected = ImmutableSet.of((Class<?>) TestingScheduledThreadPoolServiceInterface.class,
                TestingThreadPoolServiceInterface.class
        );
        assertEquals(expected, result);
    }

}
