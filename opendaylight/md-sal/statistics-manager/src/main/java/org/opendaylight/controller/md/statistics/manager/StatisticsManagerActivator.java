/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;

public class StatisticsManagerActivator extends AbstractBindingAwareProvider {
    private StatisticsProvider statsProvider;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        final DataProviderService dps = session.getSALService(DataProviderService.class);
        final NotificationProviderService nps = session.getSALService(NotificationProviderService.class);

        statsProvider = new StatisticsProvider(dps);
        statsProvider.start(nps, session);
    }

    @Override
    protected void stopImpl(BundleContext context) {
        if (statsProvider != null) {
            statsProvider.close();
            statsProvider = null;
        }
    }
}
