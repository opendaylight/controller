/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: this is simple registry service, except it also loads classes.
 */
public final class EntityOwnerSelectionStrategyConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfig.class);
    private final Map<String, StrategyInfo> entityTypeToStrategyInfo = new HashMap<>();
    private final Map<String, EntityOwnerSelectionStrategy> entityTypeToOwnerSelectionStrategy = new HashMap<>();

    private EntityOwnerSelectionStrategyConfig() {

    }

    public boolean isStrategyConfigured(final String entityType) {
        return entityTypeToStrategyInfo.get(entityType) != null;
    }

    public EntityOwnerSelectionStrategy createStrategy(final String entityType,
            final Map<String, Long> initialStatistics) {
        final EntityOwnerSelectionStrategy strategy;
        final EntityOwnerSelectionStrategy existingStrategy = entityTypeToOwnerSelectionStrategy.get(entityType);
        if (existingStrategy != null) {
            strategy = existingStrategy;
        } else {
            EntityOwnerSelectionStrategyConfig.StrategyInfo strategyInfo = entityTypeToStrategyInfo.get(entityType);
            if (strategyInfo == null) {
                strategy = FirstCandidateSelectionStrategy.INSTANCE;
            } else {
                strategy = strategyInfo.createStrategy(initialStatistics);
            }
            entityTypeToOwnerSelectionStrategy.put(entityType, strategy);
        }
        return strategy;
    }

    /**
     * This class should not exist. It contains a single long, which is passed to the constructor (via reflection).
     * We are getting that information from a BundleContext. We are running in OSGi environment, hence this class
     * needs to be deployed in its own bundle, with its own configuration.
     * If this is used internally, it needs to be relocated into a separate package along with the implementation
     * using it.
     *
     * @deprecated FIXME: THIS IS CONFIGURATION FOR A CUSTOM-LOADED CLASS CONSTRUCTOR
     */
    @Deprecated
    public void clearStrategies() {
        entityTypeToOwnerSelectionStrategy.clear();
    }

    private static final class StrategyInfo {
        private final Class<? extends EntityOwnerSelectionStrategy> strategyClass;
        private final long delay;

        private StrategyInfo(final Class<? extends EntityOwnerSelectionStrategy> strategyClass, final long delay) {
            this.strategyClass = strategyClass;
            this.delay = delay;
        }

        public EntityOwnerSelectionStrategy createStrategy(final Map<String, Long> initialStatistics) {
            try {
                return strategyClass.getDeclaredConstructor(long.class, Map.class)
                        .newInstance(delay, initialStatistics);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                LOG.warn("could not create custom strategy", e);
            }
            return FirstCandidateSelectionStrategy.INSTANCE;
        }
    }

    public static Builder newBuilder() {
        return new Builder(new EntityOwnerSelectionStrategyConfig());
    }

    public static final class Builder {
        private final EntityOwnerSelectionStrategyConfig config;

        Builder(final EntityOwnerSelectionStrategyConfig config) {
            this.config = config;
        }

        public Builder addStrategy(final String entityType,
                final Class<? extends EntityOwnerSelectionStrategy> strategy, final long delay) {
            config.entityTypeToStrategyInfo.put(entityType, new StrategyInfo(strategy, delay));
            return this;
        }

        public EntityOwnerSelectionStrategyConfig build() {
            return this.config;
        }
    }
}
