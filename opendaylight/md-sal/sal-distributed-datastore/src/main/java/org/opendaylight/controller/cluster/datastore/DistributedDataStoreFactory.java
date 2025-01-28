/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.nio.file.Path;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DistributedDataStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);
    private static final String DEFAULT_MODULE_SHARDS_PATH = "./configuration/initial/module-shards.conf";
    private static final String DEFAULT_MODULES_PATH = "./configuration/initial/modules.conf";

    private DistributedDataStoreFactory() {
    }

    public static AbstractDataStore createInstance(final Path stateDir, final DOMSchemaService schemaService,
            final DatastoreContext initialDatastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final DatastoreContextIntrospector introspector,
            final DatastoreContextPropertiesUpdater updater) {
        return createInstance(stateDir, schemaService, initialDatastoreContext, datastoreSnapshotRestore,
            actorSystemProvider, introspector, updater, null);
    }

    // TODO: separate out settle wait so it is better controlled
    public static AbstractDataStore createInstance(final Path stateDir, final DOMSchemaService schemaService,
            final DatastoreContext initialDatastoreContext, final DatastoreSnapshotRestore datastoreSnapshotRestore,
            final ActorSystemProvider actorSystemProvider, final DatastoreContextIntrospector introspector,
            final DatastoreContextPropertiesUpdater updater, final Configuration orgConfig) {

        final AbstractDataStore dataStore = createInstance(stateDir, actorSystemProvider, initialDatastoreContext,
            introspector, datastoreSnapshotRestore, orgConfig);

        updater.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore::onModelContextUpdated);

        dataStore.setCloseable(updater);
        dataStore.waitTillReady();

        return dataStore;
    }

    public static AbstractDataStore createInstance(final Path stateDir, final ActorSystemProvider actorSystemProvider,
            final DatastoreContext initialDatastoreContext, final DatastoreContextIntrospector introspector,
            final DatastoreSnapshotRestore datastoreSnapshotRestore, final Configuration orgConfig) {

        final String datastoreName = initialDatastoreContext.getDataStoreName();
        LOG.info("Create data store instance of type : {}", datastoreName);

        final var actorSystem = actorSystemProvider.getActorSystem();
        final var restoreFromSnapshot = datastoreSnapshotRestore.getAndRemove(datastoreName).orElse(null);

        final Configuration config;
        if (orgConfig == null) {
            config = new ConfigurationImpl(DEFAULT_MODULE_SHARDS_PATH, DEFAULT_MODULES_PATH);
        } else {
            config = orgConfig;
        }
        final var clusterWrapper = new ClusterWrapperImpl(actorSystem);
        final var contextFactory = introspector.newContextFactory();

        final var ret = new ClientBackedDataStore(stateDir, actorSystem, clusterWrapper, config, contextFactory,
            restoreFromSnapshot);
        LOG.info("Data store {} is using tell-based protocol", datastoreName);
        return ret;
    }
}
