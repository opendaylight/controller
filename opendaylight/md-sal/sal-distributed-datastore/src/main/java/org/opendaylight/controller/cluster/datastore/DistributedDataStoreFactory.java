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
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationReader;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;

public class DistributedDataStoreFactory {

    public static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";

    public static final String CONFIGURATION_NAME = "odl-cluster-data";

    private static AtomicReference<ActorSystem> persistentActorSystem = new AtomicReference<>();

    public static DistributedDataStore createInstance(String name, SchemaService schemaService,
                                                      DatastoreContext datastoreContext, BundleContext bundleContext) {

        ActorSystem actorSystem = getOrCreateInstance(bundleContext, datastoreContext.getConfigurationReader());
        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore =
                new DistributedDataStore(actorSystem, name, new ClusterWrapperImpl(actorSystem),
                        config, datastoreContext);

        ShardStrategyFactory.setConfiguration(config);
        schemaService.registerSchemaContextListener(dataStore);
        dataStore.waitTillReady();
        return dataStore;
    }

    synchronized private static final ActorSystem getOrCreateInstance(final BundleContext bundleContext, ConfigurationReader configurationReader) {

        AtomicReference<ActorSystem> actorSystemReference = persistentActorSystem;
        String configurationName = CONFIGURATION_NAME;
        String actorSystemName = ACTOR_SYSTEM_NAME;

        if (actorSystemReference.get() != null){
            return actorSystemReference.get();
        }

        // Create an OSGi bundle classloader for actor system
        BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());

        ActorSystem system = ActorSystem.create(actorSystemName,
                ConfigFactory.load(configurationReader.read()).getConfig(configurationName), classLoader);
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");

        actorSystemReference.set(system);
        return system;
    }

}
