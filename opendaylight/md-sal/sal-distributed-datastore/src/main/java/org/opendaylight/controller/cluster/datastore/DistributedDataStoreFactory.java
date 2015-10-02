/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.osgi.BundleDelegatingClassLoader;
import akka.remote.RemotingLifecycleEvent;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class DistributedDataStoreFactory {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    private static final String SEED_NODES_PATH = "akka.cluster.seed-nodes";
    private static final ExecutorService restartExecutor = Executors.newSingleThreadExecutor();
    private static final Set<DistributedDataStore> createdInstances = new HashSet<>(2);
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);
    private static BundleDelegatingClassLoader classLoader = null;
    private static Config akkaConfig = null;
    private static volatile ActorSystem actorSystem = null;
    private static volatile boolean isActorSystemRestarting = false;

    public static synchronized DistributedDataStore createInstance(SchemaService schemaService,
            DatastoreContext datastoreContext, BundleContext bundleContext) {

        LOG.info("Create data store instance of type : {}", datastoreContext.getDataStoreType());

        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext);
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                introspector, bundleContext);

        if (akkaConfig == null) {
            akkaConfig = ConfigFactory.load(datastoreContext.getConfigurationReader().read()).getConfig(
                    CONFIGURATION_NAME);
        }

        if (classLoader == null) {
            // Create an OSGi bundle classloader for actor system
            classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(), Thread.currentThread()
                    .getContextClassLoader());
        }

        if (actorSystem == null) {
            actorSystem = startActorSystem(akkaConfig, classLoader);
        }

        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore = new DistributedDataStore(actorSystem,
                new ClusterWrapperImpl(actorSystem), config, introspector.getContext());

        overlay.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(overlay);
        dataStore.waitTillReady();

        createdInstances.add(dataStore);
        return dataStore;
    }

    private static synchronized final ActorSystem startActorSystem(Config akkaConfig, ClassLoader classLoader) {

        LOG.trace("Starting new ActorSystem {}", ACTOR_SYSTEM_NAME);
        final ActorSystem actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME, akkaConfig, classLoader);
        actorSystem.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");

        ActorRef monitor = actorSystem.actorOf(Props.create(NodeMonitor.class), NodeMonitor.ADDRESS);
        actorSystem.eventStream().subscribe(monitor, RemotingLifecycleEvent.class);
        Cluster cluster = Cluster.get(actorSystem);
        cluster.subscribe(monitor, ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);

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

    public static synchronized void restartActorSystem() {

        // only allow one restart at a time
        if (actorSystem != null && !isActorSystemRestarting) {

            isActorSystemRestarting = true;

            // run in a separate thread since this method is called from within an actor, so
            // that we avoid blocking the actor system from shutting down
            restartExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.trace("restartActorSystem: {}", actorSystem);

                    final Cluster cluster = Cluster.get(actorSystem);

                    // remove self address from default seed nodes in akka.conf
                    List<String> defaultSeedNodes = akkaConfig.getStringList(SEED_NODES_PATH);
                    ArrayList<String> nonSelfSeedNodes = new ArrayList<String>();
                    for (String seedNode : defaultSeedNodes) {
                        if (!seedNode.equals(cluster.selfAddress().toString())) {
                            nonSelfSeedNodes.add(seedNode);
                        }
                    }

                    final Config akkaConfigForRestart = akkaConfig.withValue(SEED_NODES_PATH,
                            ConfigValueFactory.fromIterable(nonSelfSeedNodes));
                    LOG.debug("akkaConfigForRestart: {}", akkaConfigForRestart.toString());

                    LOG.trace("Shutting down ActorSystem {}", ACTOR_SYSTEM_NAME);
                    actorSystem.shutdown();
                    try {
                        actorSystem.awaitTermination(Duration.create(60, TimeUnit.SECONDS));
                        LOG.info("ActorSystem {} terminated", ACTOR_SYSTEM_NAME);
                    } catch (Exception e) {
                        LOG.warn("Error awaiting actor system termination", e);
                    }

                    // start new actor system
                    actorSystem = startActorSystem(akkaConfigForRestart, classLoader);
                    for (DistributedDataStore dataStore : createdInstances) {
                        dataStore.setActorSystem(actorSystem, new ClusterWrapperImpl(actorSystem));
                    }

                    isActorSystemRestarting = false;

                }
            });
        }
    }

}
