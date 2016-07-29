/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;

public class ShardedDataTreeActor extends AbstractUntypedPersistentActor {

    private final DOMDataTreeService dataTreeService;
    private final DOMDataTreeShardingService shardingService;

    ShardedDataTreeActor(final ShardedDataTreeActorCreator builder) {
        dataTreeService = builder.getDataTreeService();
        shardingService = builder.getShardingService();
    }

    @Override
    protected void handleRecover(final Object message) throws Exception {

    }

    @Override
    protected void handleCommand(final Object message) throws Exception {

    }

    @Override
    public String persistenceId() {
        return null;
    }

    public static class ShardedDataTreeActorCreator {

        private DOMDataTreeService dataTreeService;
        private DOMDataTreeShardingService shardingService;

        public DOMDataTreeService getDataTreeService() {
            return dataTreeService;
        }

        public ShardedDataTreeActorCreator setDataTreeService(final DOMDataTreeService dataTreeService) {
            this.dataTreeService = dataTreeService;
            return this;
        }

        public DOMDataTreeShardingService getShardingService() {
            return shardingService;
        }

        public ShardedDataTreeActorCreator setShardingService(final DOMDataTreeShardingService shardingService) {
            this.shardingService = shardingService;
            return this;
        }

        private void verify() {
            Preconditions.checkNotNull(dataTreeService);
            Preconditions.checkNotNull(shardingService);
        }

        public Props props() {
            verify();
            return Props.create(ShardedDataTreeActor.class, this);
        }
    }
}
