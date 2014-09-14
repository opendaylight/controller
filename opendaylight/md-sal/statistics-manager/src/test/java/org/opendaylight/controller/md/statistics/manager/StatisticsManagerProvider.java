/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Sep 6, 2014
 */
public class StatisticsManagerProvider {

    private final StatisticsManagerActivator activator;

    public StatisticsManagerProvider(final StatisticsManagerActivator activator) {
        this.activator = activator;
    }

    /**
     * Method provides Initialized {@link StatisticsManager}
     * from {@link StatisticsManagerActivator} for all tests
     * suites;
     *
     * @return
     */
    public StatisticsManager getStatisticsManager() {
        return activator.getStatisticManager();
    }
}
