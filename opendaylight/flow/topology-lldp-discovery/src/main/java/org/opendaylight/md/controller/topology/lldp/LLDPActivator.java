/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;

public class LLDPActivator extends AbstractBindingAwareProvider {
    private static LLDPDiscoveryProvider provider = new LLDPDiscoveryProvider();

    public void onSessionInitiated(final ProviderContext session) {
        DataProviderService dataService = session.<DataProviderService>getSALService(DataProviderService.class);
        provider.setDataService(dataService);
        NotificationProviderService notificationService = session.<NotificationProviderService>getSALService(NotificationProviderService.class);
        provider.setNotificationService(notificationService);
        provider.start();
    }

    protected void stopImpl(final BundleContext context) {
        provider.close();
    }
}
