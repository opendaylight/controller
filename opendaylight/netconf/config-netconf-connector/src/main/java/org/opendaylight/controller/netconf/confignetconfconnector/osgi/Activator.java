/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import static com.google.common.base.Preconditions.checkState;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.rpc.context.rev130617.RpcContextRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.Modules;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.util.FilteringSchemaContextProxy;
import org.opendaylight.yangtools.yang.model.util.FilteringSchemaContextProxy.ModuleId;
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

    private BundleContext context;
    private ServiceRegistration<?> osgiRegistration;
    private ConfigRegistryLookupThread configRegistryLookup = null;
    private final Set<ModuleId> ModuleIds = new HashSet<>();

    @Override
    public void start(final BundleContext context) throws Exception {
        this.context = context;

        ServiceTrackerCustomizer<SchemaContextProvider, ConfigRegistryLookupThread> customizer = new ServiceTrackerCustomizer<SchemaContextProvider, ConfigRegistryLookupThread>() {
            @Override
            public ConfigRegistryLookupThread addingService(final ServiceReference<SchemaContextProvider> reference) {
                LOG.debug("Got addingService(SchemaContextProvider) event, starting ConfigRegistryLookupThread");
                checkState(configRegistryLookup == null, "More than one onYangStoreAdded received");

                //for config, config (rpc-context) yang schemas visible only
                ModuleIds.add(new ModuleId("config", Modules.QNAME.getRevision()));
                ModuleIds.add(new ModuleId("rpc-context", RpcContextRef.QNAME.getRevision()));

                SchemaContextProvider schemaContextProvider = new SchemaContextProvider() {
                    @Override
                    public SchemaContext getSchemaContext() {
                        return new FilteringSchemaContextProxy(reference.getBundle().getBundleContext().getService(reference).getSchemaContext(),
                                ModuleIds, new HashSet<ModuleId>());
                    }
                };

                YangStoreServiceImpl yangStoreService = new YangStoreServiceImpl(schemaContextProvider);
                configRegistryLookup = new ConfigRegistryLookupThread(yangStoreService);
                configRegistryLookup.start();
                return configRegistryLookup;
            }

            @Override
            public void modifiedService(ServiceReference<SchemaContextProvider> reference, ConfigRegistryLookupThread configRegistryLookup) {
                LOG.debug("Got modifiedService(SchemaContextProvider) event");
                configRegistryLookup.yangStoreService.refresh();

            }

            @Override
            public void removedService(ServiceReference<SchemaContextProvider> reference, ConfigRegistryLookupThread configRegistryLookup) {
                configRegistryLookup.interrupt();
                if (osgiRegistration != null) {
                    osgiRegistration.unregister();
                }
                osgiRegistration = null;
                Activator.this.configRegistryLookup = null;
            }
        };

        ServiceTracker<SchemaContextProvider, ConfigRegistryLookupThread> listenerTracker = new ServiceTracker<>(context, SchemaContextProvider.class, customizer);
        listenerTracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        if (configRegistryLookup != null) {
            configRegistryLookup.interrupt();
        }
    }

    private class ConfigRegistryLookupThread extends Thread {
        private final YangStoreServiceImpl yangStoreService;

        private ConfigRegistryLookupThread(YangStoreServiceImpl yangStoreService) {
            super("config-registry-lookup");
            this.yangStoreService = yangStoreService;
        }

        @Override
        public void run() {
            NetconfOperationServiceFactoryImpl factory = new NetconfOperationServiceFactoryImpl(yangStoreService);
            LOG.debug("Registering into OSGi");
            Dictionary<String, String> properties = new Hashtable<>();
            properties.put("name", "config-netconf-connector");
            osgiRegistration = context.registerService(NetconfOperationServiceFactory.class, factory, properties);
        }
    }
}
