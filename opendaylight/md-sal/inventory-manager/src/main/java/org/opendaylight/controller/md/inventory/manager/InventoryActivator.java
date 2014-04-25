/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;

public class InventoryActivator extends AbstractBindingAwareProvider {

    private static FlowCapableInventoryProvider provider = new FlowCapableInventoryProvider();

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        DataProviderService salDataService = session.<DataProviderService> getSALService(DataProviderService.class);
        NotificationProviderService salNotifiService =
                session.<NotificationProviderService> getSALService(NotificationProviderService.class);
        InventoryActivator.provider.setDataService(salDataService);
        InventoryActivator.provider.setNotificationService(salNotifiService);
        InventoryActivator.provider.start();
    }

    @Override
    protected void stopImpl(final BundleContext context) {
        InventoryActivator.provider.close();
    }
}
