/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.osgi;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfMonitoringActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringActivator.class);

    private NetconfMonitoringServiceTracker monitor;

    @Override
    public void start(final BundleContext context)  {
        monitor = new NetconfMonitoringServiceTracker(context);
        monitor.open();
    }

    @Override
    public void stop(final BundleContext context) {
        if(monitor!=null) {
            try {
                monitor.close();
            } catch (final Exception e) {
                LOG.warn("Ignoring exception while closing {}", monitor, e);
            }
        }
    }

    public static class NetconfMonitoringOperationServiceFactory implements NetconfOperationServiceFactory, AutoCloseable {

        private final NetconfMonitoringOperationService operationService;

        private static final Set<Capability> CAPABILITIES = Sets.<Capability>newHashSet(new Capability() {

            @Override
            public String getCapabilityUri() {
                return MonitoringConstants.URI;
            }

            @Override
            public Optional<String> getModuleNamespace() {
                return Optional.of(MonitoringConstants.NAMESPACE);
            }

            @Override
            public Optional<String> getModuleName() {
                return Optional.of(MonitoringConstants.MODULE_NAME);
            }

            @Override
            public Optional<String> getRevision() {
                return Optional.of(MonitoringConstants.MODULE_REVISION);
            }

            @Override
            public Optional<String> getCapabilitySchema() {
                return Optional.absent();
            }

            @Override
            public Collection<String> getLocation() {
                return Collections.emptyList();
            }
        });

        private static final AutoCloseable AUTO_CLOSEABLE = new AutoCloseable() {
            @Override
            public void close() throws Exception {
                // NOOP
            }
        };

        private final List<CapabilityListener> listeners = new ArrayList<>();

        public NetconfMonitoringOperationServiceFactory(final NetconfMonitoringOperationService operationService) {
            this.operationService = operationService;
        }

        @Override
        public NetconfOperationService createService(final String netconfSessionIdForReporting) {
            return operationService;
        }

        @Override
        public Set<Capability> getCapabilities() {
            return CAPABILITIES;
        }

        @Override
        public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
            listener.onCapabilitiesAdded(getCapabilities());
            listeners.add(listener);
            return AUTO_CLOSEABLE;
        }

        @Override
        public void close() {
            for (final CapabilityListener listener : listeners) {
                listener.onCapabilitiesRemoved(getCapabilities());
            }
        }
    }
}
