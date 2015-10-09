/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityOwnerSelectionStrategyConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfig.class);
    private final Map<String, StrategyInfo> entityTypeToStrategyInfo = new HashMap<>();
    private final Map<String, EntityOwnerSelectionStrategy> entityTypeToOwnerSelectionStrategy = new HashMap<>();

    private EntityOwnerSelectionStrategyConfig(){

    }

    public EntityOwnerSelectionStrategy createStrategy(String entityType){
        final EntityOwnerSelectionStrategy strategy;
        final EntityOwnerSelectionStrategy existingStrategy = entityTypeToOwnerSelectionStrategy.get(entityType);
        if(existingStrategy != null){
            strategy = existingStrategy;
        } else {
            EntityOwnerSelectionStrategyConfig.StrategyInfo strategyInfo = entityTypeToStrategyInfo.get(entityType);
            if(strategyInfo == null){
                strategy = FirstCandidateSelectionStrategy.INSTANCE;
            } else {
                strategy = strategyInfo.createStrategy();
            }
            entityTypeToOwnerSelectionStrategy.put(entityType, strategy);
        }
        return strategy;

    }

    private static final class StrategyInfo {
        private final Class<? extends EntityOwnerSelectionStrategy> strategyClass;
        private final long delay;

        private StrategyInfo(Class<? extends EntityOwnerSelectionStrategy> strategyClass, long delay) {
            this.strategyClass = strategyClass;
            this.delay = delay;
        }

        public EntityOwnerSelectionStrategy createStrategy(){
            try {
                return strategyClass.getDeclaredConstructor(long.class).newInstance(delay);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOG.warn("could not create custom strategy", e);
            }
            return FirstCandidateSelectionStrategy.INSTANCE;
        }
    }

    public static Builder newBuilder(){
        return new Builder(new EntityOwnerSelectionStrategyConfig());
    }

    public static class Builder {
        private final EntityOwnerSelectionStrategyConfig config;

        private Builder(EntityOwnerSelectionStrategyConfig config){
            this.config = config;
        }

        public Builder addStrategy(String entityType, Class<? extends EntityOwnerSelectionStrategy> strategy, long delay){
            config.entityTypeToStrategyInfo.put(entityType, new StrategyInfo(strategy, delay));
            return this;
        }

        public EntityOwnerSelectionStrategyConfig build(){
            return this.config;
        }
    }

}
