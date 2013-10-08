/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class YangStoreServiceTracker extends ServiceTracker<YangStoreService, YangStoreService> {
    private final YangStoreTrackerListener listener;

    YangStoreServiceTracker(BundleContext context, final YangStoreTrackerListener listener) {
        super(context, YangStoreService.class, null);
        this.listener = listener;
    }

    @Override
    public synchronized YangStoreService addingService(final ServiceReference<YangStoreService> reference) {
        final YangStoreService yangStoreService = super.addingService(reference);
        listener.onYangStoreAdded(yangStoreService);
        return yangStoreService;
    }

    @Override
    public synchronized void removedService(final ServiceReference<YangStoreService> reference,
            final YangStoreService service) {
        listener.onYangStoreRemoved();
    }

    static interface YangStoreTrackerListener {
        void onYangStoreAdded(YangStoreService yangStoreService);
        void onYangStoreRemoved();
    }
}
