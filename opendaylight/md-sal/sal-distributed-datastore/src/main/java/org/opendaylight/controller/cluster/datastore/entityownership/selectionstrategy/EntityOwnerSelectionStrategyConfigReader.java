/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityOwnerSelectionStrategyConfigReader {
    public static final String CONFIG_ID = "org.opendaylight.controller.cluster.entity-owner-selection-strategies";

    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfigReader.class);
    private final BundleContext bundleContext;
    private final EntityOwnerSelectionStrategyConfig config;

    public EntityOwnerSelectionStrategyConfigReader(BundleContext bundleContext) {
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        ServiceReference<ConfigurationAdmin> configAdminServiceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);
        if(configAdminServiceReference == null) {
            LOG.warn("No ConfigurationAdmin service found");
            this.config = EntityOwnerSelectionStrategyConfig.newBuilder().build();
        } else {
            this.config = readConfiguration(configAdminServiceReference);
        }
    }

    private EntityOwnerSelectionStrategyConfig readConfiguration(ServiceReference<ConfigurationAdmin> configAdminServiceReference) {
        EntityOwnerSelectionStrategyConfig strategyConfig;
        EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();
        try {
            ConfigurationAdmin configAdmin = bundleContext.getService(configAdminServiceReference);
            Configuration config = configAdmin.getConfiguration(CONFIG_ID);
            Dictionary<String, Object> properties = config.getProperties();
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String strategyProps = (String) properties.get(key);
                String[] strategyClassAndDelay = strategyProps.split(",");
                if(strategyClassAndDelay.length >= 1) {
                    Class<? extends EntityOwnerSelectionStrategy> aClass
                            = (Class<? extends EntityOwnerSelectionStrategy>) getClass().getClassLoader().loadClass(strategyClassAndDelay[0]);
                    long delay = 0;
                    if(strategyClassAndDelay.length > 1){
                        delay = Long.parseLong(strategyClassAndDelay[1]);
                    }
                    builder.addStrategy(key, aClass, delay);
                }
            }
            strategyConfig = builder.build();
        } catch(IOException | ClassNotFoundException | NumberFormatException e){
            LOG.warn("Failed to read selection strategy configuration file. All configuration will be ignored.", e);
            strategyConfig = EntityOwnerSelectionStrategyConfig.newBuilder().build();
        } finally {
            bundleContext.ungetService(configAdminServiceReference);
        }
        return strategyConfig;
    }

    public EntityOwnerSelectionStrategyConfig getConfig() {
        return config;
    }
}
