/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedDataStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);

    public static AbstractDataStore createInstance(final SchemaService schemaService,
            final DatastoreContext datastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final BundleContext bundleContext) {

        LOG.info("Create data store instance of type : {}", datastoreContext.getDataStoreName());

        ActorSystem actorSystem = actorSystemProvider.getActorSystem();
        DatastoreSnapshot restoreFromSnapshot = datastoreSnapshotRestore.getAndRemove(
                datastoreContext.getDataStoreName());
        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext);
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                introspector, bundleContext);

        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        ClusterWrapper clusterWrapper = new ClusterWrapperImpl(actorSystem);
        DatastoreContextFactory contextFactory = introspector.newContextFactory();

        final AbstractDataStore dataStore = datastoreContext.isUseTellBasedProtocol()
                ? new ClientBackedDataStore(actorSystem, clusterWrapper, config, contextFactory, restoreFromSnapshot) :
                    new DistributedDataStore(actorSystem, clusterWrapper, config, contextFactory, restoreFromSnapshot);

        overlay.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(overlay);
        dataStore.waitTillReady();

        return dataStore;
    }
}
