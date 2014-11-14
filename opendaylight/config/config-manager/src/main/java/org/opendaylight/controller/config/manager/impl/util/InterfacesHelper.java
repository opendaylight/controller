/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.management.JMX;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;

public final class InterfacesHelper {

    private InterfacesHelper() {
    }

    public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        if (clazz.isInterface()) {
            throw new IllegalArgumentException(clazz
                    + " should not be an interface");
        }
        // getInterfaces gets interfaces implemented directly by this class
        Set<Class<?>> toBeInspected = new HashSet<>();
        while (clazz.equals(Object.class) == false) {
            toBeInspected.addAll(Arrays.asList(clazz.getInterfaces()));
            // get parent class
            clazz = clazz.getSuperclass();
        }
        return getAllSuperInterfaces(toBeInspected);

    }

    private static Set<Class<?>> getAllSuperInterfaces(final Set<? extends Class<?>> ifcs) {
        Set<Class<?>> interfaces = new HashSet<>(ifcs); // create copy to modify
        // each interface can extend other interfaces
        Set<Class<?>> result = new HashSet<>();
        while (!interfaces.isEmpty()) {
            Iterator<Class<?>> iterator = interfaces.iterator();
            Class<?> ifc = iterator.next();
            iterator.remove();
            if (ifc.isInterface() == false)  {
                throw new IllegalArgumentException(ifc + " should be an interface");
            }
            interfaces.addAll(Arrays.asList(ifc.getInterfaces()));
            result.add(ifc);
        }
        return result;
    }

    /**
     * Get interfaces that this class is derived from that are JMX interfaces.
     */
    public static Set<Class<?>> getMXInterfaces(
            final Class<? extends Module> configBeanClass) {
        Set<Class<?>> allInterfaces = getAllInterfaces(configBeanClass);
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : allInterfaces) {
            if (JMX.isMXBeanInterface(clazz)) {
                result.add(clazz);
            }
        }
        return result;
    }

    /**
     * Get all implemented interfaces that have
     * {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation}
     * annotation.
     */
    public static Set<Class<?>> getServiceInterfaces(
            final Class<? extends Module> configBeanClass) {
        Set<Class<?>> allInterfaces = getAllInterfaces(configBeanClass);
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : allInterfaces) {
            if (AbstractServiceInterface.class.isAssignableFrom(clazz)) {
                ServiceInterfaceAnnotation annotation = clazz
                        .getAnnotation(ServiceInterfaceAnnotation.class);
                if (annotation != null) {
                    result.add(clazz);
                }
            }
        }
        return result;
    }

    public static Set<Class<? extends AbstractServiceInterface>> getAllAbstractServiceClasses(final Class<? extends Module> configBeanClass) {

        Set<Class<? extends AbstractServiceInterface>> foundGeneratedSIClasses = new HashSet<>();
        for (Class<?> clazz : getAllInterfaces(configBeanClass)) {
            if (AbstractServiceInterface.class.isAssignableFrom(clazz) && AbstractServiceInterface.class.equals(clazz) == false) {
                foundGeneratedSIClasses.add((Class<? extends AbstractServiceInterface>) clazz);
            }
        }
        return getAllAbstractServiceInterfaceClasses(foundGeneratedSIClasses);
    }


    /**
     * Get OSGi registration types under which config bean instance should be
     * registered. This is specified in
     * {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation#osgiRegistrationType()}
     */
    public static Set<Class<?>> getOsgiRegistrationTypes(
            final Class<? extends Module> configBeanClass) {
        Set<Class<?>> serviceInterfaces = getServiceInterfaces(configBeanClass);
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : serviceInterfaces) {
            ServiceInterfaceAnnotation annotation = clazz
                    .getAnnotation(ServiceInterfaceAnnotation.class);
            result.add(annotation.osgiRegistrationType());
        }
        return result;
    }

    public static Set<String> getQNames(final Set<ServiceInterfaceAnnotation> siAnnotations) {
        Set<String> qNames = new HashSet<>();
        for (ServiceInterfaceAnnotation sia: siAnnotations) {
            qNames.add(sia.value());
        }
        return ImmutableSet.copyOf(qNames);
    }

    public static Set<ServiceInterfaceAnnotation> getServiceInterfaceAnnotations(final ModuleFactory factory) {
        Set<Class<? extends AbstractServiceInterface>> implementedServiceIntefaces = Collections.unmodifiableSet(factory.getImplementedServiceIntefaces());
        return getServiceInterfaceAnnotations(implementedServiceIntefaces);
    }

    private static Set<ServiceInterfaceAnnotation> getServiceInterfaceAnnotations(final Set<Class<? extends AbstractServiceInterface>> implementedServiceIntefaces) {
        Set<Class<? extends AbstractServiceInterface>> inspected = getAllAbstractServiceInterfaceClasses(implementedServiceIntefaces);
        Set<ServiceInterfaceAnnotation> result = new HashSet<>();
        // SIs can form hierarchies, inspect superclass until it does not extend AbstractSI
        for (Class<?> clazz : inspected) {
            ServiceInterfaceAnnotation annotation = clazz.getAnnotation(ServiceInterfaceAnnotation.class);
            if (annotation != null) {
                result.add(annotation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    static Set<Class<? extends AbstractServiceInterface>> getAllAbstractServiceInterfaceClasses(
            final Set<Class<? extends AbstractServiceInterface>> directlyImplementedAbstractSIs) {

        Set<Class<?>> allInterfaces = getAllSuperInterfaces(directlyImplementedAbstractSIs);
        Set<Class<? extends AbstractServiceInterface>> result = new HashSet<>();
        for(Class<?> ifc: allInterfaces){
            if (AbstractServiceInterface.class.isAssignableFrom(ifc) &&
                    ifc.equals(AbstractServiceInterface.class) == false) {
                result.add((Class<? extends AbstractServiceInterface>) ifc);
            }

        }
        return result;
    }
}
