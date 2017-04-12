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
            final DatastoreContext initialDatastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final BundleContext bundleContext) {

        final String datastoreName = initialDatastoreContext.getDataStoreName();
        LOG.info("Create data store instance of type : {}", datastoreName);

        final ActorSystem actorSystem = actorSystemProvider.getActorSystem();
        final DatastoreSnapshot restoreFromSnapshot = datastoreSnapshotRestore.getAndRemove(datastoreName);
        final DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(initialDatastoreContext);
        final DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                introspector, bundleContext);

        final Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final ClusterWrapper clusterWrapper = new ClusterWrapperImpl(actorSystem);
        final DatastoreContextFactory contextFactory = introspector.newContextFactory();

        // This is the potentially-updated datastore context, distinct from the initial one
        final DatastoreContext datastoreContext = contextFactory.getBaseDatastoreContext();

        final AbstractDataStore dataStore;
        if (datastoreContext.isUseTellBasedProtocol()) {
            dataStore = new ClientBackedDataStore(actorSystem, clusterWrapper, config, contextFactory,
                restoreFromSnapshot);
            LOG.info("Data store {} is using tell-based protocol", datastoreName);
        } else {
            dataStore = new DistributedDataStore(actorSystem, clusterWrapper, config, contextFactory,
                restoreFromSnapshot);
            LOG.info("Data store {} is using ask-based protocol", datastoreName);
        }

        overlay.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(overlay);
        dataStore.waitTillReady();

        return dataStore;
    }
}
