/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouter;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouterImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.protocol.framework.SessionListenerFactory;

public class NetconfServerSessionListenerFactory implements SessionListenerFactory<NetconfServerSessionListener> {

    private final CommitNotifier commitNotifier;
    private final SessionMonitoringService monitor;
    private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;
    private final CapabilityProvider capabilityProvider;

    public NetconfServerSessionListenerFactory(final CommitNotifier commitNotifier,
                                               final SessionMonitoringService monitor,
                                               final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot,
                                               final CapabilityProvider capabilityProvider) {

        this.commitNotifier = commitNotifier;
        this.monitor = monitor;
        this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;
        this.capabilityProvider = capabilityProvider;
    }

    @Override
    public NetconfServerSessionListener getSessionListener() {
        NetconfOperationRouter operationRouter = new NetconfOperationRouterImpl(netconfOperationServiceSnapshot, capabilityProvider, commitNotifier);
        return new NetconfServerSessionListener(operationRouter, monitor, netconfOperationServiceSnapshot);
    }
}
