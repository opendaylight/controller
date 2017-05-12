/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import com.google.common.base.Preconditions;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers instantiated beans as OSGi services and unregisters these services
 * if beans are destroyed.
 */
public class BeanToOsgiServiceManager {
    private static final String SERVICE_NAME_OSGI_PROP = "name";

    /**
     * To be called for every created, reconfigured and recreated config bean.
     * It is expected that before using this method OSGi service registry will
     * be cleaned from previous registrations.
     */
    public OsgiRegistration registerToOsgi(final AutoCloseable instance, final ModuleIdentifier moduleIdentifier,
                                           final BundleContext bundleContext,
                                           final Map<ServiceInterfaceAnnotation, String /* service ref name */> serviceNamesToAnnotations) {
        return new OsgiRegistration(instance, moduleIdentifier, bundleContext, serviceNamesToAnnotations);
    }

    public static class OsgiRegistration implements AutoCloseable {
        private static final Logger LOG = LoggerFactory.getLogger(OsgiRegistration.class);

        @GuardedBy("this")
        private AutoCloseable instance;
        private final ModuleIdentifier moduleIdentifier;
        @GuardedBy("this")
        private final Set<ServiceRegistration<?>> serviceRegistrations;
        @GuardedBy("this")
        private final Map<ServiceInterfaceAnnotation, String /* service ref name */> serviceNamesToAnnotations;

        public OsgiRegistration(final AutoCloseable instance, final ModuleIdentifier moduleIdentifier,
                                final BundleContext bundleContext,
                                final Map<ServiceInterfaceAnnotation, String /* service ref name */> serviceNamesToAnnotations) {
            this.instance = instance;
            this.moduleIdentifier = moduleIdentifier;
            this.serviceNamesToAnnotations = serviceNamesToAnnotations;
            this.serviceRegistrations = registerToSR(instance, bundleContext, serviceNamesToAnnotations);
        }

        private static Set<ServiceRegistration<?>> registerToSR(final AutoCloseable instance, final BundleContext bundleContext,
                                                                final Map<ServiceInterfaceAnnotation, String /* service ref name */> serviceNamesToAnnotations) {
            Set<ServiceRegistration<?>> serviceRegistrations = new HashSet<>();
            for (Entry<ServiceInterfaceAnnotation, String /* service ref name */> entry : serviceNamesToAnnotations.entrySet()) {
                ServiceInterfaceAnnotation annotation = entry.getKey();
                Class<?> requiredInterface = annotation.osgiRegistrationType();

                if(!annotation.registerToOsgi()) {
                    LOG.debug("registerToOsgi for service interface {} is false - not registering", requiredInterface);
                    continue;
                }

                Preconditions.checkState(requiredInterface.isInstance(instance), instance.getClass().getName() +
                        " instance should implement " + requiredInterface.getName());
                Dictionary<String, String> propertiesForOsgi = createProps(entry.getValue());
                ServiceRegistration<?> serviceRegistration = bundleContext
                        .registerService(requiredInterface.getName(), instance, propertiesForOsgi);
                serviceRegistrations.add(serviceRegistration);
            }
            return serviceRegistrations;
        }

        @Override
        public synchronized void close() {
            for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
                try {
                    serviceRegistration.unregister();
                } catch(final IllegalStateException e) {
                    LOG.trace("Cannot unregister {}", serviceRegistration, e);
                }
            }
            serviceRegistrations.clear();
        }

        public synchronized void updateRegistrations(final Map<ServiceInterfaceAnnotation, String /* service ref name */> newAnnotationMapping,
                                                     final BundleContext bundleContext, final AutoCloseable newInstance) {
            boolean notEquals = !this.instance.equals(newInstance);
            notEquals |= !newAnnotationMapping.equals(serviceNamesToAnnotations);
            if (notEquals) {
                // FIXME: changing from old state to new state can be improved by computing the diff
                LOG.debug("Detected change in service registrations for {}: old: {}, new: {}", moduleIdentifier,
                        serviceNamesToAnnotations, newAnnotationMapping);
                close();
                this.instance = newInstance;
                Set<ServiceRegistration<?>> newRegs = registerToSR(instance, bundleContext, newAnnotationMapping);
                serviceRegistrations.clear();
                serviceRegistrations.addAll(newRegs);
            }
        }

        private static Dictionary<String, String> createProps(final String serviceName) {
            Hashtable<String, String> result = new Hashtable<>();
            result.put(SERVICE_NAME_OSGI_PROP, serviceName);
            return result;
        }
    }
}
