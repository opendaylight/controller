/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.netconf.api.util.NetconfConstants;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration<?> osgiRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        ServiceTrackerCustomizer<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory> schemaServiceTrackerCustomizer = new ServiceTrackerCustomizer<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory>() {

            @Override
            public ConfigSubsystemFacadeFactory addingService(ServiceReference<ConfigSubsystemFacadeFactory> reference) {
                LOG.debug("Got addingService(SchemaContextProvider) event");
                // Yang store service should not be registered multiple times
                ConfigSubsystemFacadeFactory configSubsystemFacade = reference.getBundle().getBundleContext().getService(reference);
                osgiRegistration = startNetconfServiceFactory(configSubsystemFacade, context);
                return configSubsystemFacade;
            }

            @Override
            public void modifiedService(ServiceReference<ConfigSubsystemFacadeFactory> reference, ConfigSubsystemFacadeFactory service) {
                LOG.warn("Config manager facade was modified unexpectedly");
            }

            @Override
            public void removedService(ServiceReference<ConfigSubsystemFacadeFactory> reference, ConfigSubsystemFacadeFactory service) {
                LOG.warn("Config manager facade was removed unexpectedly");
            }
        };

        ServiceTracker<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory> schemaContextProviderServiceTracker =
                new ServiceTracker<>(context, ConfigSubsystemFacadeFactory.class, schemaServiceTrackerCustomizer);
        schemaContextProviderServiceTracker.open();
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        if (osgiRegistration != null) {
            osgiRegistration.unregister();
        }
    }

    private ServiceRegistration<NetconfOperationServiceFactory> startNetconfServiceFactory(final ConfigSubsystemFacadeFactory configSubsystemFacade, final BundleContext context) {
        final NetconfOperationServiceFactoryImpl netconfOperationServiceFactory = new NetconfOperationServiceFactoryImpl(configSubsystemFacade);
        // Add properties to autowire with netconf-impl instance for cfg subsystem
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(NetconfConstants.SERVICE_NAME, NetconfConstants.CONFIG_NETCONF_CONNECTOR);
        return context.registerService(NetconfOperationServiceFactory.class, netconfOperationServiceFactory, properties);
    }

}
