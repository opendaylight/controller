/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationReader;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;

public class DistributedDataStoreFactory {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";

    private static volatile ActorSystem persistentActorSystem = null;

    public static DistributedDataStore createInstance(SchemaService schemaService,
                                                      DatastoreContext datastoreContext, BundleContext bundleContext) {

        ActorSystem actorSystem = getOrCreateInstance(bundleContext, datastoreContext.getConfigurationReader());
        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore =
                new DistributedDataStore(actorSystem, new ClusterWrapperImpl(actorSystem),
                        config, datastoreContext);

        ShardStrategyFactory.setConfiguration(config);
        schemaService.registerSchemaContextListener(dataStore);
        return dataStore;
    }

    private static final ActorSystem getOrCreateInstance(final BundleContext bundleContext, ConfigurationReader configurationReader) {
        ActorSystem ret = persistentActorSystem;
        if (ret == null) {
            synchronized (DistributedDataStoreFactory.class) {
                ret = persistentActorSystem;
                if (ret == null) {
                    // Create an OSGi bundle classloader for actor system
                    BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                        Thread.currentThread().getContextClassLoader());

                    ret = ActorSystem.create(ACTOR_SYSTEM_NAME,
                        ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME), classLoader);
                    ret.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");

                    persistentActorSystem = ret;
                }
            }
        }

        return ret;
    }
}
