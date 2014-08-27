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
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;

import java.io.File;

public class DistributedDataStoreFactory {

    public static final String AKKA_CONF_PATH = "./configuration/initial/akka.conf";
    public static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    public static final String CONFIGURATION_NAME = "odl-cluster-data";

    public static DistributedDataStore createInstance(String name, SchemaService schemaService,
                                                      DatastoreContext datastoreContext, BundleContext bundleContext) {

        ActorSystem actorSystem = createInstance(bundleContext);
        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore =
                new DistributedDataStore(actorSystem, name, new ClusterWrapperImpl(actorSystem),
                        config, datastoreContext);

        ShardStrategyFactory.setConfiguration(config);
        schemaService.registerSchemaContextListener(dataStore);
        return dataStore;
    }

    private static final ActorSystem createInstance(final BundleContext bundleContext) {

        // Create an OSGi bundle classloader for actor system
        BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());

        ActorSystem system = ActorSystem.create(ACTOR_SYSTEM_NAME,
                ConfigFactory.load(readAkkaConfiguration()).getConfig(CONFIGURATION_NAME), classLoader);
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");


        return system;
    }


    private static final Config readAkkaConfiguration() {
        File defaultConfigFile = new File(AKKA_CONF_PATH);
        Preconditions.checkState(defaultConfigFile.exists(), "akka.conf is missing");
        return ConfigFactory.parseFile(defaultConfigFile);
    }
}
