/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.compat.DOMStoreAdapter;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * In-memory DOM Data Store providing Controller MD-SAL APIs on top of MD-SAL's
 * {@link org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore}.
 *
 * @deprecated Please use {@link org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore} instead.
 */
@Deprecated
public class InMemoryDOMDataStore
        extends DOMStoreAdapter<org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore>
        implements Identifiable<String>, SchemaContextListener, AutoCloseable {
    private final org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore delegate;

    public InMemoryDOMDataStore(final String name, final ExecutorService dataChangeListenerExecutor) {
        this(name, LogicalDatastoreType.OPERATIONAL, dataChangeListenerExecutor,
            InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
    }

    public InMemoryDOMDataStore(final String name, final LogicalDatastoreType type,
            final ExecutorService dataChangeListenerExecutor,
            final int maxDataChangeListenerQueueSize, final boolean debugTransactions) {
        delegate = new org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore(name, type.toMdsal(),
            dataChangeListenerExecutor, maxDataChangeListenerQueueSize, debugTransactions);
    }

    @Override
    protected org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore delegate() {
        return delegate;
    }

    public void setCloseable(final AutoCloseable closeable) {
        delegate.setCloseable(closeable);
    }

    public QueuedNotificationManager<?, ?> getDataChangeListenerNotificationManager() {
        return delegate.getDataChangeListenerNotificationManager();
    }

    @Override
    public final String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext ctx) {
        delegate.onGlobalContextUpdated(ctx);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public final boolean getDebugTransactions() {
        return delegate.getDebugTransactions();
    }
}
