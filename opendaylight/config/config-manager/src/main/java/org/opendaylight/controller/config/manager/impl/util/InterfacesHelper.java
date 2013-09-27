/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.JMX;

import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.spi.Module;

public class InterfacesHelper {

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
        // each interface can extend other interfaces
        Set<Class<?>> inspected = new HashSet<>();
        while (toBeInspected.size() > 0) {
            Iterator<Class<?>> iterator = toBeInspected.iterator();
            Class<?> ifc = iterator.next();
            iterator.remove();
            toBeInspected.addAll(Arrays.asList(ifc.getInterfaces()));
            inspected.add(ifc);
        }
        return inspected;
    }

    /**
     * Get interfaces that this class is derived from that are JMX interfaces.
     */
    public static Set<Class<?>> getMXInterfaces(
            Class<? extends Module> configBeanClass) {
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
            Class<? extends Module> configBeanClass) {
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

    /**
     * Get OSGi registration types under which config bean instance should be
     * registered. This is specified in
     * {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation#osgiRegistrationType()}
     */
    public static Set<Class<?>> getOsgiRegistrationTypes(
            Class<? extends Module> configBeanClass) {
        Set<Class<?>> serviceInterfaces = getServiceInterfaces(configBeanClass);
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : serviceInterfaces) {
            ServiceInterfaceAnnotation annotation = clazz
                    .getAnnotation(ServiceInterfaceAnnotation.class);
            result.add(annotation.osgiRegistrationType());
        }
        return result;
    }

}
