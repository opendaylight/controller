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
import java.nio.file.Path;
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfigProvider;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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
        private final DatastoreContextIntrospector introspector;
        private final LogicalDatastoreType datastoreType;
        private final AbstractDataStore datastore;
        private final String serviceType;

        @GuardedBy("this")
        private ComponentInstance<OSGiDOMStore> component;
        @GuardedBy("this")
        private boolean stopped;

        DatastoreState(final DatastoreContextIntrospector introspector, final LogicalDatastoreType datastoreType,
                final AbstractDataStore datastore, final String serviceType) {
            this.introspector = requireNonNull(introspector);
            this.datastoreType = requireNonNull(datastoreType);
            this.datastore = requireNonNull(datastore);
            this.serviceType = requireNonNull(serviceType);
        }

        synchronized void updateProperties(final Map<String, Object> properties) {
            if (introspector.update(properties)) {
                LOG.info("Distributed Datastore type {} updating context", datastoreType);
                datastore.onDatastoreContextUpdated(introspector.newContextFactory());
            }
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
                    component = datastoreFactory.newInstance(FrameworkUtil.asDictionary(Map.of(
                        OSGiDOMStore.DATASTORE_TYPE_PROP, datastoreType,
                        OSGiDOMStore.DATASTORE_INST_PROP, datastore,
                        "type", serviceType)));
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
    private static final Path STATE_DIR = Path.of("state");

    private final ComponentFactory<OSGiDOMStore> datastoreFactory;
    private DatastoreState configDatastore;
    private DatastoreState operDatastore;

    @Activate
    public OSGiDistributedDataStore(@Reference final DOMSchemaService schemaService,
            @Reference final ActorSystemProvider actorSystemProvider,
            @Reference final DatastoreContextIntrospectorFactory introspectorFactory,
            @Reference final DatastoreSnapshotRestore snapshotRestore,
            @Reference final ModuleShardConfigProvider configProvider,
            @Reference(target = "(component.factory=" + OSGiDOMStore.FACTORY_NAME + ")")
            final ComponentFactory<OSGiDOMStore> datastoreFactory, final Map<String, Object> properties) {
        this.datastoreFactory = requireNonNull(datastoreFactory);
        configDatastore = createDatastore(STATE_DIR, schemaService, actorSystemProvider, snapshotRestore,
            introspectorFactory, LogicalDatastoreType.CONFIGURATION, "distributed-config", properties, null);
        operDatastore = createDatastore(STATE_DIR, schemaService, actorSystemProvider, snapshotRestore,
            introspectorFactory, LogicalDatastoreType.OPERATIONAL, "distributed-operational", properties,
            new ConfigurationImpl(configProvider));
    }

    @Modified
    void modified(final Map<String, Object> properties) {
        LOG.debug("Overlaying settings: {}", properties);
        configDatastore.updateProperties(properties);
        operDatastore.updateProperties(properties);
    }

    @Deactivate
    void deactivate() {
        operDatastore.stop();
        operDatastore = null;
        configDatastore.stop();
        configDatastore = null;
    }

    private DatastoreState createDatastore(final Path stateDir, final DOMSchemaService schemaService,
            final ActorSystemProvider actorSystemProvider, final DatastoreSnapshotRestore snapshotRestore,
            final DatastoreContextIntrospectorFactory introspectorFactory, final LogicalDatastoreType datastoreType,
            final String serviceType, final Map<String, Object> properties,final Configuration config) {
        LOG.info("Distributed Datastore type {} starting", datastoreType);
        final var introspector = introspectorFactory.newInstance(datastoreType, properties);
        final var datastore = DistributedDataStoreFactory.createInstance(stateDir, actorSystemProvider,
            introspector.getContext(), introspector, snapshotRestore, config);
        datastore.setCloseable(schemaService.registerSchemaContextListener(datastore::onModelContextUpdated));
        final var state = new DatastoreState(introspector, datastoreType, datastore, serviceType);

        Futures.addCallback(datastore.initialSettleFuture(), state,
            // Note we are invoked from shard manager and therefore could block it, hence the round-trip to executor
            datastore.getActorUtils().getClientDispatcher()::execute);
        return state;
    }
}
