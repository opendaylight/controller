/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.nio.file.Path;
import org.apache.pekko.actor.ActorSystem;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.LocalShardStore;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.AbstractShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.shardmanager.TestShardManager;
import org.opendaylight.controller.cluster.datastore.shardmanager.TestShardManager.GetLocalShards;
import org.opendaylight.controller.cluster.datastore.shardmanager.TestShardManager.GetLocalShardsReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

public class TestClientBackedDataStore extends ClientBackedDataStore implements LocalShardStore {
    public TestClientBackedDataStore(final Path stateDir, final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        super(stateDir, actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
    }

    TestClientBackedDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier,
                              final DataStoreClient clientActor) {
        super(actorUtils, identifier, clientActor);
    }

    @Override
    protected AbstractShardManagerCreator<?> getShardManagerCreator() {
        return new TestShardManager.TestShardManagerCreator();
    }

    @Override
    public GetLocalShardsReply getLocalShards() {
        final ActorUtils utils = getActorUtils();
        return (GetLocalShardsReply) utils.executeOperation(utils.getShardManager(), GetLocalShards.INSTANCE);
    }
}
