/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import akka.actor.ActorRef;
import akka.actor.Scheduler;
import com.google.common.base.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContextExecutor;

public class EntityOwnerSelectionStrategyConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerSelectionStrategyConfig.class);
    private final Map<String, StrategyInfo> entityTypeToStrategyInfo = new HashMap<>();
    private final Map<String, EntityOwnerSelectionStrategyWrapper> ownerSelectionStrategies = new HashMap<>();

    private EntityOwnerSelectionStrategyWrapper defaultEntityOwnerSelectionStrategy;
    private Scheduler scheduler;
    private ExecutionContextExecutor dispatcher;
    private ActorRef shard;
    private boolean initialized = false;

    public void addStrategy(String entityType, Class<? extends EntityOwnerSelectionStrategy> strategy, long delay){
        entityTypeToStrategyInfo.put(entityType, new StrategyInfo(strategy, delay));
    }

    public void initialize(Scheduler scheduler, ExecutionContextExecutor dispatcher, ActorRef shard){
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.shard = Preconditions.checkNotNull(shard);
        this.initialized = true;
        this.defaultEntityOwnerSelectionStrategy =
                createEntityOwnerSelectionStrategyWrapper(FirstCandidateSelectionStrategy.INSTANCE);
    }

    public EntityOwnerSelectionStrategyWrapper getEntityOwnerSelectionStrategyWrapper(YangInstanceIdentifier entityPath){
        Preconditions.checkState(initialized, "config is not initialized");
        String entityType = EntityOwnersModel.entityTypeFromEntityPath(entityPath);

        if(ownerSelectionStrategies.containsKey(entityType)){
            return ownerSelectionStrategies.get(entityType);
        }

        EntityOwnerSelectionStrategyWrapper wrapper = null;
        if(entityTypeToStrategyInfo.containsKey(entityType)){
            wrapper = createEntityOwnerSelectionStrategyWrapper(entityTypeToStrategyInfo.get(entityType).createStrategy());
        }

        if(wrapper == null){
            wrapper = defaultEntityOwnerSelectionStrategy;
        }
        ownerSelectionStrategies.put(entityType, wrapper);

        return wrapper;
    }

    private EntityOwnerSelectionStrategyWrapper createEntityOwnerSelectionStrategyWrapper(EntityOwnerSelectionStrategy entityOwnerSelectionStrategy){
        return new EntityOwnerSelectionStrategyWrapper(scheduler, shard, dispatcher, entityOwnerSelectionStrategy);
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

}
