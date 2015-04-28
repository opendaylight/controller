/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import java.io.IOException;
import org.opendaylight.controller.cluster.raft.election.DefaultElectionStrategy;
import org.opendaylight.controller.cluster.raft.election.ElectionStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ElectionStrategyFactory {
    public static final String CONFIG_ID = "org.opendaylight.controller.cluster.datastore.leaders";

    private static ElectionStrategy electionStrategy = new DefaultElectionStrategy();

    public static ElectionStrategy getElectionStrategy(){
        return electionStrategy;
    }

    public static void setBundleContext(BundleContext bundleContext){
        ServiceReference<ConfigurationAdmin> configAdminServiceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(configAdminServiceReference);
        try {
            Configuration configuration = configurationAdmin.getConfiguration(CONFIG_ID);
            electionStrategy = new FixedLeaderElectionStrategy(configuration.getProperties());
        } catch (IOException e) {
            // Election strategy is unchanged
        }
    }
}
