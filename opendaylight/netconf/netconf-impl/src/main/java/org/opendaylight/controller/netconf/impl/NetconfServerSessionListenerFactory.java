/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouterImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshot;
import org.opendaylight.protocol.framework.SessionListenerFactory;

public class NetconfServerSessionListenerFactory implements SessionListenerFactory<NetconfServerSessionListener> {

    private final NetconfOperationServiceFactoryListener factoriesListener;

    private final DefaultCommitNotificationProducer commitNotifier;

    private final SessionIdProvider idProvider;

    public NetconfServerSessionListenerFactory(NetconfOperationServiceFactoryListener factoriesListener,
            DefaultCommitNotificationProducer commitNotifier,
            SessionIdProvider idProvider) {
        this.factoriesListener = factoriesListener;
        this.commitNotifier = commitNotifier;
        this.idProvider = idProvider;
    }

    @Override
    public NetconfServerSessionListener getSessionListener() {
        NetconfOperationServiceSnapshot netconfOperationServiceSnapshot = factoriesListener.getSnapshot(idProvider
                .getCurrentSessionId());

        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(netconfOperationServiceSnapshot);

        NetconfOperationRouterImpl operationRouter = new NetconfOperationRouterImpl(
                netconfOperationServiceSnapshot, capabilityProvider,
                commitNotifier);

        return new NetconfServerSessionListener(operationRouter);
    }
}
