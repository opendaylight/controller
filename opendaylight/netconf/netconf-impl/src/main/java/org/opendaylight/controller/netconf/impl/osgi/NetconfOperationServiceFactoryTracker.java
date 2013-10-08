/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class NetconfOperationServiceFactoryTracker extends
        ServiceTracker<NetconfOperationServiceFactory, NetconfOperationServiceFactory> {
    private final NetconfOperationServiceFactoryListener operationRouter;

    NetconfOperationServiceFactoryTracker(BundleContext context,
            final NetconfOperationServiceFactoryListener operationRouter) {
        super(context, NetconfOperationServiceFactory.class, null);
        this.operationRouter = operationRouter;
    }

    @Override
    public NetconfOperationServiceFactory addingService(ServiceReference<NetconfOperationServiceFactory> reference) {
        NetconfOperationServiceFactory netconfOperationServiceFactory = super.addingService(reference);
        operationRouter.onAddNetconfOperationServiceFactory(netconfOperationServiceFactory);
        return netconfOperationServiceFactory;
    }

    @Override
    public void removedService(ServiceReference<NetconfOperationServiceFactory> reference,
            NetconfOperationServiceFactory netconfOperationServiceFactory) {
        operationRouter.onRemoveNetconfOperationServiceFactory(netconfOperationServiceFactory);
    }

}
