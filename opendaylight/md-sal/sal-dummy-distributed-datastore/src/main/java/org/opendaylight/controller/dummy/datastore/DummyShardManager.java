/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.dummy.datastore;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;

public class DummyShardManager extends UntypedAbstractActor {
    public DummyShardManager(final Configuration configuration, final String memberName, final String[] shardNames,
            final String type) {
        new DummyShardsCreator(configuration, getContext(), memberName, shardNames, type).create();
    }

    @Override
    public void onReceive(final Object message) {

    }

    public static Props props(final Configuration configuration, final String memberName, final String[] shardNames,
            final String type) {
        return Props.create(DummyShardManager.class, configuration, memberName, shardNames, type);
    }

    private static class DummyShardsCreator {
        private final Configuration configuration;
        private final ActorContext actorSystem;
        private final String memberName;
        private final String[] shardNames;
        private final String type;

        DummyShardsCreator(final Configuration configuration, final ActorContext actorSystem, final String memberName,
                final String[] shardNames, final String type) {
            this.configuration = configuration;
            this.actorSystem = actorSystem;
            this.memberName = memberName;
            this.shardNames = shardNames;
            this.type = type;
        }

        void create() {
            for (String shardName : shardNames) {
                String shardId = memberName + "-shard-" + shardName + "-" + type;
                actorSystem.actorOf(DummyShard.props(configuration, shardId), shardId);
            }
        }
    }
}
