/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerImpl;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Statistics Manager Activator
 *
 * OSGi bundle activator
 *
 */
public class StatisticsManagerActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(StatisticsManagerActivator.class);

    /* TODO VD move it to ConfigSubsystem */
    private static final long DEFAULT_MIN_REQUEST_NET_MONITOR_INTERVAL = 15000L;

    private StatisticsManager statsProvider;

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("StatisticsManagerActivator initialization.");
        try {
            final DataBroker dataBroker = session.getSALService(DataBroker.class);
            final NotificationProviderService notifService =
                    session.getSALService(NotificationProviderService.class);
            statsProvider = new StatisticsManagerImpl(dataBroker);
            statsProvider.start(notifService, session, DEFAULT_MIN_REQUEST_NET_MONITOR_INTERVAL);
            LOG.info("StatisticsManagerActivator started successfully.");
        }
        catch (final Exception e) {
            LOG.error("Unexpected error by initialization of StatisticsManagerActivator", e);
            stopImpl(null);
        }
    }

    @VisibleForTesting
    StatisticsManager getStatisticManager() {
        return statsProvider;
    }

    @Override
    protected void stopImpl(final BundleContext context) {
        if (statsProvider != null) {
            try {
                statsProvider.close();
            }
            catch (final Exception e) {
                LOG.error("Unexpected error by stopping StatisticsManagerActivator", e);
            }
            statsProvider = null;
        }
        LOG.info("StatisticsManagerActivator stoped.");
    }
}
