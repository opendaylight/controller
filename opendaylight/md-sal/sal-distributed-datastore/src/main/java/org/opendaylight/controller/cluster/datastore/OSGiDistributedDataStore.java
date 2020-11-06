/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfigProvider;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global bootstrap component. It is responsible to start all distributed datastore instances and activate
 * {@link OSGiDOMStore} as appropriate. It also provides routing of datastore proprerties towards AbstractDataStore.
 */
@Beta
@Component(immediate = true, configurationPid = "org.opendaylight.controller.cluster.datastore")
public final class OSGiDistributedDataStore {
    /**
     * Internal state associated with a particular datastore. An instance is created for each datastore and once the
     * datastore settles, we create a new component configuration of {@link OSGiDOMStore}. This indirection is needed
     * to not block Service Component Runtime from activating other components while we are waiting for the datastore
     * to settle (which can take a long time).
     */
    private final class DatastoreState implements FutureCallback<Object> {
        private final LogicalDatastoreType datastoreType;
        private final AbstractDataStore datastore;
        private final String serviceType;

        @GuardedBy("this")
        private ComponentInstance component;
        @GuardedBy("this")
        private boolean stopped;

        DatastoreState(final LogicalDatastoreType datastoreType, final AbstractDataStore datastore,
                       final String serviceType) {
            this.datastoreType = requireNonNull(datastoreType);
            this.datastore = requireNonNull(datastore);
            this.serviceType = requireNonNull(serviceType);
        }

        void stop() {
            LOG.info("Distributed Datastore type {} stopping", datastoreType);

            synchronized (this) {
                stopped = true;
                if (component != null) {
                    component.dispose();
                    component = null;
                }
                datastore.close();
                LOG.info("Distributed Datastore type {} stopped", datastoreType);
            }
        }

        @Override
        public void onSuccess(final Object result) {
            LOG.debug("Distributed Datastore type {} reached initial settle", datastoreType);

            synchronized (this) {
                if (!stopped) {
                    final Dictionary<String, Object> dict = new Hashtable<>();
                    dict.put(OSGiDOMStore.DATASTORE_TYPE_PROP, datastoreType);
                    dict.put(OSGiDOMStore.DATASTORE_INST_PROP, datastore);
                    dict.put("type", serviceType);
                    component = datastoreFactory.newInstance(dict);
                    LOG.info("Distributed Datastore type {} started", datastoreType);
                }
            }
        }

        @Override
        public synchronized void onFailure(final Throwable cause) {
            LOG.error("Distributed Datastore type {} failed to settle", datastoreType, cause);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(OSGiDistributedDataStore.class);

    @Reference
    DOMSchemaService schemaService = null;
    @Reference
    ActorSystemProvider actorSystemProvider = null;
    @Reference
    DatastoreContextIntrospectorFactory introspectorFactory = null;
    @Reference
    DatastoreSnapshotRestore snapshotRestore = null;
    @Reference
    ModuleShardConfigProvider configProvider = null;
    @Reference(target = "(component.factory=" + OSGiDOMStore.FACTORY_NAME + ")")
    ComponentFactory datastoreFactory = null;

    private DatastoreState configDatastore;
    private DatastoreState operDatastore;

    @Activate
    void activate(final Map<String, Object> properties) {
        configDatastore = createDatastore(LogicalDatastoreType.CONFIGURATION, "distributed-config",
                null, properties);
        operDatastore = createDatastore(LogicalDatastoreType.OPERATIONAL, "distributed-operational",
                new ConfigurationImpl(configProvider), properties);
    }


    @Deactivate
    void deactivate() {
        operDatastore.stop();
        operDatastore = null;
        configDatastore.stop();
        configDatastore = null;
    }

    private DatastoreState createDatastore(final LogicalDatastoreType datastoreType, final String serviceType,
                                           final Configuration config, final Map<String, Object> properties) {
        LOG.info("Distributed Datastore type {} starting", datastoreType);
        final DatastoreContextIntrospector introspector = introspectorFactory.newInstance(datastoreType);
        introspector.update(properties);
        final AbstractDataStore datastore = DistributedDataStoreFactory.createInstance(actorSystemProvider,
            introspector.getContext(), introspector, snapshotRestore, config);
        datastore.setCloseable(schemaService.registerSchemaContextListener(datastore));
        final DatastoreState state = new DatastoreState(datastoreType, datastore, serviceType);

        Futures.addCallback(datastore.initialSettleFuture(), state,
            // Note we are invoked from shard manager and therefore could block it, hence the round-trip to executor
            datastore.getActorUtils().getClientDispatcher()::execute);
        return state;
    }
}
