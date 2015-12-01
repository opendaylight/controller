/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedDataStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreFactory.class);

    public static DistributedDataStore createInstance(SchemaService schemaService,
            DatastoreContext datastoreContext, DatastoreSnapshot restoreFromSnapshot, ActorSystem actorSystem,
            BundleContext bundleContext) {

        LOG.info("Create data store instance of type : {}", datastoreContext.getDataStoreName());

        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext);
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                introspector, bundleContext);

        Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
        final DistributedDataStore dataStore = new DistributedDataStore(actorSystem,
                new ClusterWrapperImpl(actorSystem), config, introspector.newContextFactory(), restoreFromSnapshot);

        overlay.setListener(dataStore);

        schemaService.registerSchemaContextListener(dataStore);

        dataStore.setCloseable(overlay);
        dataStore.waitTillReady();

        return dataStore;
    }
}
