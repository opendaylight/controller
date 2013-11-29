/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.osgi;

import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfMonitoringActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(NetconfMonitoringActivator.class);

    private NetconfMonitoringServiceTracker monitor;

    @Override
    public void start(final BundleContext context) throws Exception {
        monitor = new NetconfMonitoringServiceTracker(context);
        monitor.open();

    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if(monitor!=null) {
            try {
                monitor.close();
            } catch (Exception e) {
                logger.warn("Ignoring exception while closing {}", monitor, e);
            }
        }
    }

    public static class NetconfMonitoringOperationServiceFactory implements NetconfOperationServiceFactory {
        private final NetconfMonitoringOperationService operationService;

        public NetconfMonitoringOperationServiceFactory(NetconfMonitoringOperationService operationService) {
            this.operationService = operationService;
        }

        @Override
        public NetconfOperationService createService(long netconfSessionId, String netconfSessionIdForReporting) {
            return operationService;
        }
    }
}
