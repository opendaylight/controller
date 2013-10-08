/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

import static com.google.common.base.Preconditions.checkState;

public class Activator implements BundleActivator, YangStoreServiceTracker.YangStoreTrackerListener {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private BundleContext context;
    ServiceRegistration osgiRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        YangStoreServiceTracker tracker = new YangStoreServiceTracker(context, this);
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    @Override
    public synchronized void onYangStoreAdded(YangStoreService yangStoreService) {
        checkState(osgiRegistration == null, "More than one onYangStoreAdded received");
        NetconfOperationServiceFactoryImpl factory = new NetconfOperationServiceFactoryImpl(yangStoreService);
        logger.debug("Registering into OSGi");
        osgiRegistration = context.registerService(new String[]{NetconfOperationServiceFactory.class.getName()}, factory,
                new Hashtable<String, Object>());
    }

    @Override
    public synchronized void onYangStoreRemoved() {
        osgiRegistration.unregister();
        osgiRegistration = null;
    }
}
