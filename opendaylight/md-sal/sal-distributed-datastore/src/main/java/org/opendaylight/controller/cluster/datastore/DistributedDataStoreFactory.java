/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;

import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;

public class DistributedDataStoreFactory {
    public static DistributedDataStore createInstance(String name, SchemaService schemaService,
            DatastoreContext datastoreContext, BundleContext bundleContext) {

        ActorSystem actorSystem = ActorSystemFactory.createInstance(bundleContext);
        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore =
            new DistributedDataStore(actorSystem, name, new ClusterWrapperImpl(actorSystem),
                    config, datastoreContext );
        ShardStrategyFactory.setConfiguration(config);
        schemaService.registerSchemaContextListener(dataStore);
        return dataStore;
    }
}
