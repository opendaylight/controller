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
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.ExecutionContextExecutor;

public class EntityOwnerSelectionStrategyWrapperFactory {
    private final Scheduler scheduler;
    private final ExecutionContextExecutor dispatcher;
    private final ActorRef shard;
    private final EntityOwnerSelectionStrategyConfig config;
    private final Map<String, EntityOwnerSelectionStrategyWrapper> ownerSelectionStrategies = new HashMap<>();
    private final EntityOwnerSelectionStrategyWrapper defaultEntityOwnerSelectionStrategy;

    public EntityOwnerSelectionStrategyWrapperFactory(Scheduler scheduler, ExecutionContextExecutor dispatcher, ActorRef shard, EntityOwnerSelectionStrategyConfig config){
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.shard = Preconditions.checkNotNull(shard);
        this.config = Preconditions.checkNotNull(config);
        this.defaultEntityOwnerSelectionStrategy =
                createEntityOwnerSelectionStrategyWrapper(FirstCandidateSelectionStrategy.INSTANCE);
    }

    public EntityOwnerSelectionStrategyWrapper getEntityOwnerSelectionStrategyWrapper(YangInstanceIdentifier entityPath){
        String entityType = EntityOwnersModel.entityTypeFromEntityPath(entityPath);

        if(ownerSelectionStrategies.containsKey(entityType)){
            return ownerSelectionStrategies.get(entityType);
        }

        EntityOwnerSelectionStrategyWrapper wrapper = null;
        EntityOwnerSelectionStrategyConfig.StrategyInfo info = config.getStrategyInfo(entityType);
        if(info != null){
            wrapper = createEntityOwnerSelectionStrategyWrapper(info.createStrategy());
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

}
