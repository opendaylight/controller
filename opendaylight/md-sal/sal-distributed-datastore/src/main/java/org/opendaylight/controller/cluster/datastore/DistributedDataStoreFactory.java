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
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DistributedDataStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);
    private static final String DEFAULT_MODULE_SHARDS_PATH = "./configuration/initial/module-shards.conf";
    private static final String DEFAULT_MODULES_PATH = "./configuration/initial/modules.conf";

    private DistributedDataStoreFactory() {
    }

    public static AbstractDataStore createInstance(final DOMSchemaService schemaService,
            final DatastoreContext initialDatastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final DatastoreContextIntrospector introspector,
            final DatastoreContextPropertiesUpdater updater) {
        return createInstance(schemaService, initialDatastoreContext, datastoreSnapshotRestore, actorSystemProvider,
                introspector, updater, null);
    }

    // TODO: separate out settle wait so it is better controlled
    public static AbstractDataStore createInstance(final DOMSchemaService schemaService,
            final DatastoreContext initialDatastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final DatastoreContextIntrospector introspector,
            final DatastoreContextPropertiesUpdater updater, final Configuration orgConfig) {

        final AbstractDataStore dataStore = createInstance(actorSystemProvider, initialDatastoreContext,
            introspector, datastoreSnapshotRestore, orgConfig);

        updater.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(updater);
        dataStore.waitTillReady();

        return dataStore;
    }

    public static AbstractDataStore createInstance(final ActorSystemProvider actorSystemProvider,
            final DatastoreContext initialDatastoreContext, final DatastoreContextIntrospector introspector,
            final DatastoreSnapshotRestore datastoreSnapshotRestore, final Configuration orgConfig) {

        final String datastoreName = initialDatastoreContext.getDataStoreName();
        LOG.info("Create data store instance of type : {}", datastoreName);

        final ActorSystem actorSystem = actorSystemProvider.getActorSystem();
        final DatastoreSnapshot restoreFromSnapshot = datastoreSnapshotRestore.getAndRemove(datastoreName).orElse(null);

        final Configuration config;
        if (orgConfig == null) {
            config = new ConfigurationImpl(DEFAULT_MODULE_SHARDS_PATH, DEFAULT_MODULES_PATH);
        } else {
            config = orgConfig;
        }

        return new ClientBackedDataStore(actorSystem, new ClusterWrapperImpl(actorSystem), config,
            introspector.newContextFactory(), restoreFromSnapshot);
    }
}
