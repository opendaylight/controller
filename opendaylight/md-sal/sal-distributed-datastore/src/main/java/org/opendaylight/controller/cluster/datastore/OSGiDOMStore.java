/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi manifestation of a the distributed datastore, as represented by {@link AbstractDataStore}. This component's
 * configuration is managed by {@link OSGiDistributedDataStore}.
 */
@Beta
@Component(factory = OSGiDOMStore.FACTORY_NAME, service = { DOMStore.class,  DistributedDataStoreInterface.class })
public final class OSGiDOMStore
        implements DistributedDataStoreInterface, DOMStoreTreeChangePublisher, DOMDataTreeCommitCohortRegistry {
    // OSGi DS Component Factory name
    static final String FACTORY_NAME = "org.opendaylight.controller.cluster.datastore.OSGiDOMStore";
    static final String DATASTORE_INST_PROP = ".datastore.instance";
    static final String DATASTORE_TYPE_PROP = ".datastore.type";

    private static final Logger LOG = LoggerFactory.getLogger(OSGiDOMStore.class);

    private LogicalDatastoreType datastoreType;
    private AbstractDataStore datastore;

    @Override
    public ActorUtils getActorUtils() {
        return datastore.getActorUtils();
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerProxyListener(
            final YangInstanceIdentifier shardLookup, final YangInstanceIdentifier insideShard,
            final DOMDataTreeChangeListener delegate) {
        return datastore.registerProxyListener(shardLookup, insideShard, delegate);
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        return datastore.registerTreeChangeListener(treeId, listener);
    }

    @Override
    public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
            final DOMDataTreeIdentifier path, final T cohort) {
        return datastore.registerCommitCohort(path, cohort);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return datastore.createTransactionChain();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return datastore.newReadOnlyTransaction();
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return datastore.newWriteOnlyTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return datastore.newReadWriteTransaction();
    }

    @Activate
    void activate(final Map<String, ?> properties) {
        datastoreType = (LogicalDatastoreType) verifyNotNull(properties.get(DATASTORE_TYPE_PROP));
        datastore = (AbstractDataStore) verifyNotNull(properties.get(DATASTORE_INST_PROP));
        LOG.info("Datastore service type {} activated", datastoreType);
    }

    @Deactivate
    void deactivate() {
        datastore = null;
        LOG.info("Datastore service type {} deactivated", datastoreType);
    }
}
