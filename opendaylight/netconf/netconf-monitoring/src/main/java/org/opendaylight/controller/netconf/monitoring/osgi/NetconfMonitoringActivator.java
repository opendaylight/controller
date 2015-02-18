/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.osgi;

import java.util.Collections;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
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

        private static final AutoCloseable AUTO_CLOSEABLE = new AutoCloseable() {
            @Override
            public void close() throws Exception {
                // NOOP
            }
        };

        public NetconfMonitoringOperationServiceFactory(final NetconfMonitoringOperationService operationService) {
            this.operationService = operationService;
        }

        @Override
        public NetconfOperationService createService(final String netconfSessionIdForReporting) {
            return operationService;
        }

        @Override
        public Set<Capability> getCapabilities() {
            return Collections.emptySet();
        }

        @Override
        public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
            return AUTO_CLOSEABLE;
        }

        @Override
        public void close() {}
    }
}
