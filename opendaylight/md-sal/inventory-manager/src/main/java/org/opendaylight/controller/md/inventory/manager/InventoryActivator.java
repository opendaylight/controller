/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryActivator extends AbstractBindingAwareProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryActivator.class);
    private FlowCapableInventoryProvider provider;

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        DataBroker dataBroker = session.getSALService(DataBroker.class);
        NotificationProviderService salNotifiService =
                session.getSALService(NotificationProviderService.class);

        provider = new FlowCapableInventoryProvider(dataBroker, salNotifiService);
        provider.start();
    }

    @Override
    protected void stopImpl(final BundleContext context) {
        if (provider != null) {
            try {
                provider.close();
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for shutdown", e);
            }
            provider = null;
        }
    }
}
