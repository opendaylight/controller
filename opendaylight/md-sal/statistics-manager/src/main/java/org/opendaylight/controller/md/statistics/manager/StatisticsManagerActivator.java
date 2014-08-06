/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics Manager Activator
 *
 * OSGi bundle activator
 *
 */
public class StatisticsManagerActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerActivator.class);
    private StatisticsProvider statsProvider;

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("StatisticsManagerActivator initialization.");
        try {
            final DataBroker statistService = session.getSALService(DataBroker.class);
            final NotificationProviderService notifService =
                    session.getSALService(NotificationProviderService.class);
            this.statsProvider = new StatisticsProvider(statistService);
            this.statsProvider.start(notifService, session);

            LOG.info("StatisticsManagerActivator started successfully.");
        }
        catch (Exception e) {
            LOG.error("Unexpected error by initialization of StatisticsManagerActivator", e);
        }
    }

    @Override
    protected void stopImpl(BundleContext context) {
        if (statsProvider != null) {
            try {
                statsProvider.close();
            }
            catch (Exception e) {
                LOG.error("Unexpected error by stopping StatisticsManagerActivator", e);
            }
            finally {
                statsProvider = null;
            }
        }
        LOG.info("StatisticsManagerActivator stoped successfully.");
    }
}
