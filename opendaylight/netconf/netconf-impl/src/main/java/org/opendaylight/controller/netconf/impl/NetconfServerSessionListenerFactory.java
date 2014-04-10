/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouterImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.protocol.framework.SessionListenerFactory;

import static org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider.NetconfOperationProviderUtil.getNetconfSessionIdForReporting;

public class NetconfServerSessionListenerFactory implements SessionListenerFactory<NetconfServerSessionListener> {

    private final NetconfOperationProvider netconfOperationProvider;

    private final DefaultCommitNotificationProducer commitNotifier;

    private final SessionIdProvider idProvider;

    private final SessionMonitoringService monitor;

    public NetconfServerSessionListenerFactory(NetconfOperationProvider netconfOperationProvider,
                                               DefaultCommitNotificationProducer commitNotifier,
                                               SessionIdProvider idProvider, SessionMonitoringService monitor) {
        this.netconfOperationProvider = netconfOperationProvider;
        this.commitNotifier = commitNotifier;
        this.idProvider = idProvider;
        this.monitor = monitor;
    }

    @Override
    public NetconfServerSessionListener getSessionListener() {
        NetconfOperationServiceSnapshot netconfOperationServiceSnapshot = netconfOperationProvider.getSnapshot(
                getNetconfSessionIdForReporting(idProvider.getCurrentSessionId()));

        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(netconfOperationServiceSnapshot);

        NetconfOperationRouter operationRouter = NetconfOperationRouterImpl.createOperationRouter(
                netconfOperationServiceSnapshot, capabilityProvider,
                commitNotifier);

        return new NetconfServerSessionListener(operationRouter, monitor);
    }
}
