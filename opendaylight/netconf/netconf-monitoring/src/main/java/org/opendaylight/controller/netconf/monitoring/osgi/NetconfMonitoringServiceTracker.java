/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.osgi;

import com.google.common.base.Preconditions;
import java.util.Dictionary;
import java.util.Hashtable;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.api.util.NetconfConstants;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfMonitoringServiceTracker extends ServiceTracker<NetconfMonitoringService, NetconfMonitoringService> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringServiceTracker.class);

    private ServiceRegistration<NetconfOperationServiceFactory> reg;

    NetconfMonitoringServiceTracker(final BundleContext context) {
        super(context, NetconfMonitoringService.class, null);
    }

    @Override
    public NetconfMonitoringService addingService(final ServiceReference<NetconfMonitoringService> reference) {
        Preconditions.checkState(reg == null, "Monitoring service was already added");

        final NetconfMonitoringService netconfMonitoringService = super.addingService(reference);

        final NetconfMonitoringOperationService operationService = new NetconfMonitoringOperationService(
                netconfMonitoringService);
        final NetconfOperationServiceFactory factory = new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                operationService);

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(NetconfConstants.SERVICE_NAME, NetconfConstants.NETCONF_MONITORING);
        reg = context.registerService(NetconfOperationServiceFactory.class, factory, properties);

        return netconfMonitoringService;
    }

    @Override
    public void removedService(final ServiceReference<NetconfMonitoringService> reference,
            final NetconfMonitoringService netconfMonitoringService) {
        if(reg!=null) {
            try {
                reg.unregister();
            } catch (final Exception e) {
                LOG.warn("Ignoring exception while unregistering {}", reg, e);
            }
        }
    }

}
