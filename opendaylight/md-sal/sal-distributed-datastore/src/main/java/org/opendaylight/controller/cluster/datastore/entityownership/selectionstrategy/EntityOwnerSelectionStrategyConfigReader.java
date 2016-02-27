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
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig.Builder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated FIXME: Service injection class. This class needs to be eliminated in favor of proper service injection,
 *             which can be any of OSGi (which this class uses internally), java.util.ServiceLoader, or config
 *             subsystem.
 */
@Deprecated
public final class EntityOwnerSelectionStrategyConfigReader {
    public static final String CONFIG_ID = "org.opendaylight.controller.cluster.entity.owner.selection.strategies";

    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfigReader.class);
    private static final String ENTITY_TYPE_PREFIX = "entity.type.";

    private EntityOwnerSelectionStrategyConfigReader() {
        // Hidden on purpose
    }

    public static EntityOwnerSelectionStrategyConfig loadStrategyWithConfig(final BundleContext bundleContext) {
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();

        final ServiceReference<ConfigurationAdmin> configAdminServiceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);
        if (configAdminServiceReference == null) {
            LOG.warn("No ConfigurationAdmin service found");
            return builder.build();
        }

        final ConfigurationAdmin configAdmin = bundleContext.getService(configAdminServiceReference);
        if (configAdmin == null) {
            LOG.warn("Failed to get ConfigurationAdmin service");
            return builder.build();
        }

        final Configuration config;
        try {
            config = configAdmin.getConfiguration(CONFIG_ID);
            if (config != null) {
                return parseConfiguration(builder, config);
            }

            LOG.debug("Could not read strategy configuration file, will use default configuration");
        } catch (IOException e1) {
            LOG.warn("Failed to get configuration for {}, starting up empty", CONFIG_ID);
            return builder.build();
        } finally {
            try {
                bundleContext.ungetService(configAdminServiceReference);
            } catch (Exception e) {
                LOG.debug("Error from ungetService", e);
            }
        }

        return builder.build();
    }

    private static EntityOwnerSelectionStrategyConfig parseConfiguration(final Builder builder, final Configuration config) {
        // Historic note: java.util.Dictionary since introduction of java.util.Map in Java 1.2
        final Dictionary<String, Object> properties = config.getProperties();
        if (properties == null) {
            LOG.debug("Empty strategy configuration {}, using defaults", config);
            return builder.build();
        }

        // No java.util.Iterable: Wheeey, pre-Java 5 world!!!
        final Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            if (!key.startsWith(ENTITY_TYPE_PREFIX)) {
                LOG.debug("Ignoring non-conforming property key : {}");
                continue;
            }

            final String[] strategyClassAndDelay = ((String) properties.get(key)).split(",");
            final Class<? extends EntityOwnerSelectionStrategy> aClass;
            try {
                aClass = loadClass(strategyClassAndDelay[0]);
            } catch (ClassNotFoundException e) {
                LOG.error("Failed to load class {}, ignoring it", strategyClassAndDelay[0], e);
                continue;
            }

            final long delay;
            if (strategyClassAndDelay.length > 1) {
                delay = Long.parseLong(strategyClassAndDelay[1]);
            } else {
                delay = 0;
            }

            String entityType = key.substring(key.lastIndexOf(".") + 1);
            builder.addStrategy(entityType, aClass, delay);
            LOG.debug("Entity Type '{}' using strategy {} delay {}", entityType, aClass, delay);
        }

        return builder.build();
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private static Class<? extends EntityOwnerSelectionStrategy> loadClass(final String strategyClassAndDelay)
            throws ClassNotFoundException {
        final Class<?> clazz;
        try {
           clazz = EntityOwnerSelectionStrategyConfigReader.class.getClassLoader().loadClass(strategyClassAndDelay);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to load strategy " + strategyClassAndDelay);
        }

        Preconditions.checkArgument(EntityOwnerSelectionStrategy.class.isAssignableFrom(clazz),
            "Selected implementation %s must implement EntityOwnerSelectionStrategy, clazz");

        return (Class<? extends EntityOwnerSelectionStrategy>) clazz;
    }
}
