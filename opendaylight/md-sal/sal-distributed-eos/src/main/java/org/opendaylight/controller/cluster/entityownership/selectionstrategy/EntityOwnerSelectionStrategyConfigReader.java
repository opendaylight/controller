/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the entity owner selection strategy config.
 *
 */
public final class EntityOwnerSelectionStrategyConfigReader {

    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfigReader.class);
    private static final String ENTITY_TYPE_PREFIX = "entity.type.";

    private EntityOwnerSelectionStrategyConfigReader() {
        // Hidden on purpose
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static EntityOwnerSelectionStrategyConfig loadStrategyWithConfig(final Map<Object, Object> props) {
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();

        if (props != null && !props.isEmpty()) {
            parseConfiguration(builder, props);
        } else {
            if (props == null) {
                LOG.debug("Could not read strategy configuration file, will use default configuration.");
            } else {
                LOG.debug("Configuration file is empty, will use default configuration.");
            }
        }
        return builder.build();
    }

    private static EntityOwnerSelectionStrategyConfig parseConfiguration(final Builder builder,
            final Map<Object, Object> properties) {

        for (final Entry<Object, Object> entry : properties.entrySet()) {
            final String key = (String) entry.getKey();
            if (!key.startsWith(ENTITY_TYPE_PREFIX)) {
                LOG.debug("Ignoring non-conforming property key : {}", key);
                continue;
            }

            final String[] strategyClassAndDelay = ((String) properties.get(key)).split(",");
            final Class<? extends EntityOwnerSelectionStrategy> aClass = loadClass(strategyClassAndDelay[0]);

            final long delay;
            if (strategyClassAndDelay.length > 1) {
                delay = Long.parseLong(strategyClassAndDelay[1]);
            } else {
                delay = 0;
            }

            final String entityType = key.substring(key.lastIndexOf(".") + 1);
            builder.addStrategy(entityType, aClass, delay);
            LOG.debug("Entity Type '{}' using strategy {} delay {}", entityType, aClass, delay);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends EntityOwnerSelectionStrategy> loadClass(final String strategyClassAndDelay) {
        final Class<?> clazz;
        try {
            clazz = EntityOwnerSelectionStrategyConfigReader.class.getClassLoader().loadClass(strategyClassAndDelay);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to load strategy " + strategyClassAndDelay, e);
        }

        Preconditions.checkArgument(EntityOwnerSelectionStrategy.class.isAssignableFrom(clazz),
            "Selected implementation %s must implement EntityOwnerSelectionStrategy, clazz");

        return (Class<? extends EntityOwnerSelectionStrategy>) clazz;
    }
}
