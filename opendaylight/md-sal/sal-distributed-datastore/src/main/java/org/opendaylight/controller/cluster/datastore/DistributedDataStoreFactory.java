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
import com.typesafe.config.ConfigFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationReader;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class DistributedDataStoreFactory {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    private static ActorSystem actorSystem = null;
    private static final Set<DistributedDataStore> createdInstances = new HashSet<>(2);
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);

    public static synchronized DistributedDataStore createInstance(SchemaService schemaService,
            DatastoreContext datastoreContext, BundleContext bundleContext) {

        LOG.info("Create data store instance of type : {}", datastoreContext.getDataStoreType());

        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext);
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                introspector, bundleContext);

        ActorSystem actorSystem = getActorSystem(bundleContext, datastoreContext.getConfigurationReader());
        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore = new DistributedDataStore(actorSystem,
                new ClusterWrapperImpl(actorSystem), config, introspector.getContext());

        overlay.setListener(dataStore);

        ShardStrategyFactory.setConfiguration(config);
        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(overlay);
        dataStore.waitTillReady();

        createdInstances.add(dataStore);
        return dataStore;
    }

    private static synchronized final ActorSystem getActorSystem(final BundleContext bundleContext,
                                                                 ConfigurationReader configurationReader) {
        if (actorSystem == null) {
            // Create an OSGi bundle classloader for actor system
            BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());

            actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME,
                ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME), classLoader);
            actorSystem.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
        }

        return actorSystem;
    }

    public static synchronized void destroyInstance(DistributedDataStore dataStore){
        Preconditions.checkNotNull(dataStore, "dataStore should not be null");

        LOG.info("Destroy data store instance of type : {}", dataStore.getActorContext().getDataStoreType());

        if(createdInstances.remove(dataStore)){
            if(createdInstances.size() == 0){
                if(actorSystem != null) {
                    actorSystem.shutdown();
                    try {
                        actorSystem.awaitTermination(Duration.create(10, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        LOG.warn("Error awaiting actor termination", e);
                    }
                    actorSystem = null;
                }
            }
        }
    }

}
