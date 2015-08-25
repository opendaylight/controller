/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.notification;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationRegistry;

public class NetconfNotificationOperationServiceFactory implements NetconfOperationServiceFactory, AutoCloseable {

    private final NetconfNotificationRegistry netconfNotificationRegistry;

    private static final AutoCloseable AUTO_CLOSEABLE = new AutoCloseable() {
        @Override
        public void close() throws Exception {
            // NOOP
        }
    };

    public NetconfNotificationOperationServiceFactory(NetconfNotificationRegistry netconfNotificationRegistry) {
        this.netconfNotificationRegistry = netconfNotificationRegistry;
    }

    @Override
    public Set<Capability> getCapabilities() {
        // TODO
        // No capabilities exposed to prevent clashes with schemas from config-netconf-connector (it exposes all the schemas)
        // If the schemas exposed by config-netconf-connector are filtered, this class would expose monitoring related models
        return Collections.emptySet();
    }

    @Override
    public NetconfOperationService createService(String netconfSessionIdForReporting) {
        return new NetconfNotificationOperationService(netconfSessionIdForReporting, netconfNotificationRegistry);
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        return AUTO_CLOSEABLE;
    }

    @Override
    public void close() {
    }
}
