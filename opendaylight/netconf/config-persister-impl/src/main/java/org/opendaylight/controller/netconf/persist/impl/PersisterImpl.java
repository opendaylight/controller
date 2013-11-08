/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.osgi.framework.BundleContext;

import java.io.IOException;

/**
 * {@link Persister} implementation that delegates persisting functionality to
 * underlying {@link Persister} called Storage Adapter.
 *
 * Storage adapters are low level persisters that do the heavy lifting for this
 * class. Instances of storage adapters can be injected directly via constructor
 * or instantiated from a full name of its class provided in a properties file.
 *
 * Name of storage adapter class should be located under
 * {@link #STORAGE_ADAPTER_CLASS_PROP} key.
 */
public final class PersisterImpl implements Persister {

    public static final String STORAGE_ADAPTER_CLASS_PROP = "netconf.config.persister.storageAdapterClass";
    private final StorageAdapter storage;

    public PersisterImpl(StorageAdapter storage) {
        this.storage = storage;
    }

    public static Optional<PersisterImpl> createFromProperties(BundleContext bundleContext) {
        String storageAdapterClass = bundleContext.getProperty(STORAGE_ADAPTER_CLASS_PROP);
        StorageAdapter storage;
        if (storageAdapterClass == null || storageAdapterClass.equals("")) {
            return Optional.absent();
        }

        try {
            storage = StorageAdapter.class.cast(resolveClass(storageAdapterClass, StorageAdapter.class).newInstance());
            storage.setProperties(bundleContext);

        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to instantiate storage adapter from " + storageAdapterClass, e);
        }
        return Optional.of(new PersisterImpl(storage));
    }

    private static Class<?> resolveClass(String storageAdapterClass, Class<?> baseType) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(storageAdapterClass);

        if (!isImplemented(baseType, clazz))
            throw new IllegalArgumentException("Storage adapter " + clazz + " has to implement " + baseType);
        return clazz;
    }

    private static boolean isImplemented(Class<?> expectedIface, Class<?> byClazz) {
        for (Class<?> iface : byClazz.getInterfaces()) {
            if (iface.equals(expectedIface))
                return true;
        }
        return false;
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        storage.persistConfig(holder);
    }

    @Override
    public Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException {
        return storage.loadLastConfig();
    }

    @VisibleForTesting
    StorageAdapter getStorage() {
        return storage;
    }

    @Override
    public void close() throws IOException {
        storage.close();
    }

    @Override
    public String toString() {
        return "PersisterImpl [storage=" + storage + "]";
    }
}
