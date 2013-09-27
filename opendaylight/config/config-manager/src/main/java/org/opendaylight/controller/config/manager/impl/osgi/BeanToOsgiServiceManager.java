/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.manager.impl.util.InterfacesHelper;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers instantiated beans as OSGi services and unregisters these services
 * if beans are destroyed.
 */
public class BeanToOsgiServiceManager {
    // name of properties submitted to osgi
    static final String INSTANCE_NAME_OSGI_PROP = "instanceName";
    static final String IMPLEMENTATION_NAME_OSGI_PROP = "implementationName";

    private final BundleContext bundleContext;

    public BeanToOsgiServiceManager(BundleContext context) {
        this.bundleContext = context;
    }

    /**
     * To be called for every created, reconfigured and recreated config bean.
     * It is expected that before using this method OSGi service registry will
     * be cleaned from previous registrations.
     */
    public OsgiRegistration registerToOsgi(
            Class<? extends Module> configBeanClass, AutoCloseable instance,
            ModuleIdentifier moduleIdentifier) {
        try {
            final Set<Class<?>> configuresInterfaces = InterfacesHelper
                    .getOsgiRegistrationTypes(configBeanClass);
            checkInstanceImplementing(instance, configuresInterfaces);

            // bundleContext.registerService blows up with empty 'clazzes'
            if (configuresInterfaces.isEmpty() == false) {
                final Dictionary<String, ?> propertiesForOsgi = getPropertiesForOsgi(moduleIdentifier);
                final ServiceRegistration<?> serviceRegistration = bundleContext
                        .registerService(classesToNames(configuresInterfaces), instance, propertiesForOsgi);
                return new OsgiRegistration(serviceRegistration);
            } else {
                return new OsgiRegistration();
            }
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Error while registering instance into OSGi Service Registry: "
                            + moduleIdentifier, e);
        }
    }

    private static String[] classesToNames(Set<Class<?>> cfgs) {
        String[] result = new String[cfgs.size()];
        int i = 0;
        for (Class<?> cfg : cfgs) {
            result[i] = cfg.getName();
            i++;
        }
        return result;
    }

    private void checkInstanceImplementing(AutoCloseable instance,
            Set<Class<?>> configures) {
        Set<Class<?>> missing = new HashSet<>();
        for (Class<?> requiredIfc : configures) {
            if (requiredIfc.isInstance(instance) == false) {
                missing.add(requiredIfc);
            }
        }
        if (missing.isEmpty() == false) {
            throw new IllegalStateException(
                    instance.getClass()
                            + " does not implement following interfaces as announced by "
                            + ServiceInterfaceAnnotation.class.getName()
                            + " annotation :" + missing);
        }
    }

    private static Dictionary<String, ?> getPropertiesForOsgi(
            ModuleIdentifier moduleIdentifier) {
        Hashtable<String, String> table = new Hashtable<>();
        table.put(IMPLEMENTATION_NAME_OSGI_PROP,
                moduleIdentifier.getFactoryName());
        table.put(INSTANCE_NAME_OSGI_PROP, moduleIdentifier.getInstanceName());
        return table;
    }

    public static class OsgiRegistration implements AutoCloseable {
        private final ServiceRegistration<?> serviceRegistration;

        public OsgiRegistration(ServiceRegistration<?> serviceRegistration) {
            this.serviceRegistration = serviceRegistration;
        }

        public OsgiRegistration() {
            this.serviceRegistration = null;
        }

        @Override
        public void close() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

}
