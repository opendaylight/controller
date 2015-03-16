/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring;

import java.util.Collections;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;

public class NetconfMonitoringOperationServiceFactory implements NetconfOperationServiceFactory, AutoCloseable {

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
        // TODO
        // No capabilities exposed to prevent clashes with schemas from config-netconf-connector (it exposes all the schemas)
        // If the schemas exposed by config-netconf-connector are filtered, this class would expose monitoring related models
        return Collections.emptySet();
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        return AUTO_CLOSEABLE;
    }

    @Override
    public void close() {}
}
