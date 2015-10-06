/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.SelectOwner;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

/**
 * The EntityOwnerSelectionStrategyWrapper is an EntityOwnerSelectionStrategy decorator that adds the ability to
 * schedule an owner selection job.
 */
public class EntityOwnerSelectionStrategyWrapper implements EntityOwnerSelectionStrategy {
    private final Scheduler scheduler;
    private final ActorRef shard;
    private final ExecutionContextExecutor dispatcher;
    private final EntityOwnerSelectionStrategy strategy;

    private Cancellable lastScheduledTask;

    public EntityOwnerSelectionStrategyWrapper(Scheduler scheduler,
                                                ActorRef shard,
                                                ExecutionContextExecutor dispatcher,
                                                EntityOwnerSelectionStrategy strategy) {
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.shard = Preconditions.checkNotNull(shard);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.strategy = Preconditions.checkNotNull(strategy);
    }

    /**
     * Schedule a new owner selection job. Cancelling any outstanding job if it has not been cancelled.
     *
     * @param entityPath
     * @param allCandidates
     */
    public void scheduleOwnerSelection(YangInstanceIdentifier entityPath, Collection<String> allCandidates){
        if(lastScheduledTask != null && !lastScheduledTask.isCancelled()){
            lastScheduledTask.cancel();
        }
        lastScheduledTask = scheduler.scheduleOnce(
                FiniteDuration.apply(strategy.selectionDelayInMillis(), TimeUnit.MILLISECONDS)
                , shard, new SelectOwner(entityPath, allCandidates, strategy)
                , dispatcher, shard);
    }

    @Override
    public long selectionDelayInMillis(){
        return strategy.selectionDelayInMillis();
    }

    @Override
    public String newOwner(Collection<String> viableCandidates){
        return strategy.newOwner(viableCandidates);
    }
}