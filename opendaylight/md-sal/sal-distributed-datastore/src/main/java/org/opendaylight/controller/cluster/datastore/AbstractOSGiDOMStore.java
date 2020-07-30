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
import java.util.Map;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractOSGiDOMStore
        implements DistributedDataStoreInterface, DOMStoreTreeChangePublisher, DOMDataTreeCommitCohortRegistry {
    @Component(immediate = true, service = { DOMStore.class,  DistributedDataStoreInterface.class },
            configurationPid = "org.opendaylight.controller.cluster.datastore",
            property = "type=distributed-config")
    public static final class Configuration extends AbstractOSGiDOMStore {
        @Reference
        DOMSchemaService schemaService = null;
        @Reference
        ActorSystemProvider actorSystemProvider = null;
        @Reference
        DatastoreContextIntrospectorFactory introspectorFactory = null;
        @Reference
        DatastoreSnapshotRestore snapshotRestore = null;

        public Configuration() {
            super(LogicalDatastoreType.CONFIGURATION);
        }

        @Activate
        void activate(final Map<String, Object> properties) throws InterruptedException {
            start(schemaService, actorSystemProvider, introspectorFactory, snapshotRestore, null);
        }

        @Modified
        void modified(final Map<String, Object> properties) {
            update(properties);
        }

        @Deactivate
        void deactivate() {
            stop();
        }
    }

    @Component(immediate = true, service = { DOMStore.class, DistributedDataStoreInterface.class },
            configurationPid = "org.opendaylight.controller.cluster.datastore",
            property = "type=distributed-operational")
    public static final class Operational extends AbstractOSGiDOMStore {
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

        public Operational() {
            super(LogicalDatastoreType.OPERATIONAL);
        }

        @Activate
        void activate(final Map<String, Object> properties) throws InterruptedException {
            start(schemaService, actorSystemProvider, introspectorFactory, snapshotRestore,
                new ConfigurationImpl(configProvider));
        }

        @Modified
        void modified(final Map<String, Object> properties) {
            update(properties);
        }

        @Deactivate
        void deactivate() {
            stop();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOSGiDOMStore.class);

    private final LogicalDatastoreType datastoreType;

    private ListenerRegistration<?> schemaRegistration;
    private DatastoreContextIntrospector introspector;
    private AbstractDataStore datastore;

    AbstractOSGiDOMStore(final LogicalDatastoreType datastoreType) {
        this.datastoreType = requireNonNull(datastoreType);
    }

    @Override
    public final ActorUtils getActorUtils() {
        return datastore.getActorUtils();
    }

    @Override
    public final <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerShardConfigListener(
            final YangInstanceIdentifier internalPath, final DOMDataTreeChangeListener delegate) {
        return datastore.registerShardConfigListener(internalPath, delegate);
    }

    @Override
    public final <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerProxyListener(
            final YangInstanceIdentifier shardLookup, final YangInstanceIdentifier insideShard,
            final DOMDataTreeChangeListener delegate) {
        return datastore.registerProxyListener(shardLookup, insideShard, delegate);
    }

    @Override
    public final <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        return datastore.registerTreeChangeListener(treeId, listener);
    }

    @Override
    public final <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
            final DOMDataTreeIdentifier path, final T cohort) {
        return datastore.registerCommitCohort(path, cohort);
    }

    @Override
    public final DOMStoreTransactionChain createTransactionChain() {
        return datastore.createTransactionChain();
    }

    @Override
    public final DOMStoreReadTransaction newReadOnlyTransaction() {
        return datastore.newReadOnlyTransaction();
    }

    @Override
    public final DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return datastore.newWriteOnlyTransaction();
    }

    @Override
    public final DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return datastore.newReadWriteTransaction();
    }

    final void start(final DOMSchemaService schemaService, final ActorSystemProvider actorSystemProvider,
            final DatastoreContextIntrospectorFactory introspectorFactory,
            final DatastoreSnapshotRestore snapshotRestore,
            final org.opendaylight.controller.cluster.datastore.config.Configuration config)
                    throws InterruptedException {
        LOG.info("Distributed Datastore type {} starting", datastoreType);
        introspector = introspectorFactory.newInstance(datastoreType);

        datastore = DistributedDataStoreFactory.createInstance(actorSystemProvider, introspector.getContext(),
            introspector, snapshotRestore, config);
        schemaRegistration = schemaService.registerSchemaContextListener(datastore);

        datastore.awaitReadiness();
        LOG.info("Distributed Datastore type {} started", datastoreType);
    }

    final void update(final Map<String, Object> properties) {
        LOG.debug("Overlaying settings: {}", properties);
        if (introspector.update(properties)) {
            datastore.onDatastoreContextUpdated(introspector.newContextFactory());
        }
    }

    final void stop() {
        LOG.info("Distributed Datastore type {} stopping", datastoreType);
        schemaRegistration.close();
        datastore.close();
        LOG.info("Distributed Datastore type {} stopped", datastoreType);
    }
}
