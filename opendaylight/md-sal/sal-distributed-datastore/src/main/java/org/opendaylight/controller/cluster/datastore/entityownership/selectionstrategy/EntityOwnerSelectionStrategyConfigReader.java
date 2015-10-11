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
import javax.annotation.Nullable;
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
        EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();
        ConfigurationAdmin configAdmin = null;
        try {
            configAdmin = bundleContext.getService(configAdminServiceReference);
            Dictionary<String, Object> properties = getProperties(configAdmin);
            if(properties != null) {
                Enumeration<String> keys = properties.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    String strategyProps = (String) properties.get(key);
                    String[] strategyClassAndDelay = strategyProps.split(",");
                    if(strategyClassAndDelay.length >= 1) {
                        @SuppressWarnings("unchecked")
                        Class<? extends EntityOwnerSelectionStrategy> aClass
                        = (Class<? extends EntityOwnerSelectionStrategy>) getClass().getClassLoader().loadClass(strategyClassAndDelay[0]);
                        long delay = 0;
                        if(strategyClassAndDelay.length > 1){
                            delay = Long.parseLong(strategyClassAndDelay[1]);
                        }
                        builder.addStrategy(key, aClass, delay);
                    }
                }
            }
        } catch(Exception e){
            LOG.warn("Failed to read selection strategy configuration file. All configuration will be ignored.", e);
        } finally {
            if(configAdmin != null) {
                try {
                    bundleContext.ungetService(configAdminServiceReference);
                } catch (Exception e) {
                    LOG.debug("Error from ungetService", e);
                }
            }
        }

        return builder.build();
    }

    @Nullable
    private static Dictionary<String, Object> getProperties(ConfigurationAdmin configAdmin) throws IOException {
        Configuration config = configAdmin.getConfiguration(CONFIG_ID);
        return config != null ? config.getProperties() : null;
    }

    public EntityOwnerSelectionStrategyConfig getConfig() {
        return config;
    }
}
