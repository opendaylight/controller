/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.osgi;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class NetconfMonitoringServiceTracker extends ServiceTracker<NetconfMonitoringService, NetconfMonitoringService> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfMonitoringServiceTracker.class);

    private ServiceRegistration<NetconfOperationServiceFactory> reg;

    NetconfMonitoringServiceTracker(BundleContext context) {
        super(context, NetconfMonitoringService.class, null);
    }

    @Override
    public NetconfMonitoringService addingService(ServiceReference<NetconfMonitoringService> reference) {
        Preconditions.checkState(reg == null, "Monitoring service was already added");

        NetconfMonitoringService netconfMonitoringService = super.addingService(reference);

        final NetconfMonitoringOperationService operationService = new NetconfMonitoringOperationService(
                netconfMonitoringService);
        NetconfOperationServiceFactory factory = new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                operationService);

        Dictionary<String, ?> props = new Hashtable<>();
        reg = context.registerService(NetconfOperationServiceFactory.class, factory, props);

        return netconfMonitoringService;
    }

    @Override
    public void removedService(ServiceReference<NetconfMonitoringService> reference,
            NetconfMonitoringService netconfMonitoringService) {
        if(reg!=null) {
            try {
                reg.unregister();
            } catch (Exception e) {
                logger.warn("Ignoring exception while unregistering {}", reg, e);
            }
        }
    }

}
