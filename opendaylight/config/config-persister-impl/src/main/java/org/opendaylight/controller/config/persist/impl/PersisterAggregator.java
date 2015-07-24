/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.opendaylight.controller.config.persist.impl.osgi.ConfigPersisterActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Persister} implementation that delegates persisting functionality to
 * underlying {@link Persister} storages. Each storage has unique id, class, readonly value.
 *
 * Storage adapters are low level persisters that do the heavy lifting for this
 * class. Instances of storage adapters can be injected directly via constructor
 * or instantiated from a full name of its class provided in a properties file.
 *
 * Example configuration:<pre>
 netconf.config.persister.active=2,3
 # read startup configuration
 netconf.config.persister.1.storageAdapterClass=org.opendaylight.controller.config.persist.storage.directory.xml.XmlDirectoryStorageAdapter
 netconf.config.persister.1.properties.fileStorage=configuration/initial/

 netconf.config.persister.2.storageAdapterClass=org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter
 netconf.config.persister.2.readonly=true
 netconf.config.persister.2.properties.fileStorage=configuration/current/controller.config.1.xml

 netconf.config.persister.3.storageAdapterClass=org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter
 netconf.config.persister.3.properties.fileStorage=configuration/current/controller.config.2.xml
 netconf.config.persister.3.properties.numberOfBackups=3

 </pre>
 * During server startup {@link ConfigPersisterNotificationHandler} requests last snapshot from underlying storages.
 * Each storage can respond by giving snapshot or absent response.
 * The {@link #loadLastConfigs()} will search for first non-absent response from storages ordered backwards as user
 * specified (first '3', then '2').
 *
 * When a commit notification is received, '2' will be omitted because readonly flag is set to true, so
 * only '3' will have a chance to persist new configuration. If readonly was false or not specified, both storage adapters
 * would be called in order specified by 'netconf.config.persister' property.
 *
 */
public final class PersisterAggregator implements Persister {
    private static final Logger LOG = LoggerFactory.getLogger(PersisterAggregator.class);

    public static class PersisterWithConfiguration {

        private final Persister storage;
        private final boolean readOnly;

        public PersisterWithConfiguration(Persister storage, boolean readOnly) {
            this.storage = storage;
            this.readOnly = readOnly;
        }

        @VisibleForTesting
        public Persister getStorage() {
            return storage;
        }

        @VisibleForTesting
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public String toString() {
            return "PersisterWithConfiguration{" +
                    "storage=" + storage +
                    ", readOnly=" + readOnly +
                    '}';
        }
    }

    private static PersisterWithConfiguration loadConfiguration(final String index, final PropertiesProvider propertiesProvider) {

        String classKey = index + "." + ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX;
        String storageAdapterClass = propertiesProvider.getProperty(classKey);
        StorageAdapter storageAdapter;
        if (storageAdapterClass == null || storageAdapterClass.equals("")) {
            throw new IllegalStateException("No persister is defined in " +
                    propertiesProvider.getFullKeyForReporting(classKey)
                    + " property. Persister is not operational");
        }

        try {
            Class<?> clazz = Class.forName(storageAdapterClass);
            boolean implementsCorrectIfc = StorageAdapter.class.isAssignableFrom(clazz);
            if (!implementsCorrectIfc) {
                throw new IllegalArgumentException("Storage adapter " + clazz + " does not implement " + StorageAdapter.class);
            }
            storageAdapter = StorageAdapter.class.cast(clazz.newInstance());

            boolean readOnly = false;
            String readOnlyProperty = propertiesProvider.getProperty(index + "." + "readonly");
            if (readOnlyProperty != null && readOnlyProperty.equals("true")) {
                readOnly = true;
            }

            PropertiesProviderAdapterImpl innerProvider = new PropertiesProviderAdapterImpl(propertiesProvider, index);
            Persister storage = storageAdapter.instantiate(innerProvider);
            return new PersisterWithConfiguration(storage, readOnly);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to instantiate storage adapter from " + storageAdapterClass, e);
        }
    }

    /**
     * Persisters ordered by 'netconf.config.persister' property.
     */
    private final List<PersisterWithConfiguration> persisterWithConfigurations;

    public PersisterAggregator(List<PersisterWithConfiguration> persisterWithConfigurations) {
        this.persisterWithConfigurations = persisterWithConfigurations;

    }

    public static PersisterAggregator createFromProperties(PropertiesProvider propertiesProvider) {
        List<PersisterWithConfiguration> persisterWithConfigurations = new ArrayList<>();
        String prefixes = propertiesProvider.getProperty("active");
        if (prefixes!=null && !prefixes.isEmpty()) {
            String [] keys = prefixes.split(",");
            for (String index: keys) {
                persisterWithConfigurations.add(PersisterAggregator.loadConfiguration(index, propertiesProvider));
            }
        }
        LOG.debug("Initialized persister with following adapters {}", persisterWithConfigurations);
        return new PersisterAggregator(persisterWithConfigurations);
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        for (PersisterWithConfiguration persisterWithConfiguration: persisterWithConfigurations){
            if (!persisterWithConfiguration.readOnly){
                LOG.debug("Calling {}.persistConfig", persisterWithConfiguration.getStorage());
                persisterWithConfiguration.getStorage().persistConfig(holder);
            }
        }
    }

    /**
     * @return last non-empty result from input persisters
     */
    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs()  {
        // iterate in reverse order
        ListIterator<PersisterWithConfiguration> li = persisterWithConfigurations.listIterator(persisterWithConfigurations.size());
        while(li.hasPrevious()) {
            PersisterWithConfiguration persisterWithConfiguration = li.previous();
            List<ConfigSnapshotHolder> configs = null;
            try {
                configs = persisterWithConfiguration.storage.loadLastConfigs();
            } catch (IOException e) {
                throw new RuntimeException("Error while calling loadLastConfig on " +  persisterWithConfiguration, e);
            }
            if (!configs.isEmpty()) {
                LOG.debug("Found non empty configs using {}:{}", persisterWithConfiguration, configs);
                return configs;
            }
        }
        // no storage had an answer
        LOG.debug("No non-empty list of configuration snapshots found");
        return Collections.emptyList();
    }

    @VisibleForTesting
    List<PersisterWithConfiguration> getPersisterWithConfigurations() {
        return persisterWithConfigurations;
    }

    @Override
    public void close() {
        RuntimeException lastException = null;
        for (PersisterWithConfiguration persisterWithConfiguration: persisterWithConfigurations){
            try{
                persisterWithConfiguration.storage.close();
            }catch(RuntimeException e) {
                LOG.error("Error while closing {}", persisterWithConfiguration.storage, e);
                if (lastException == null){
                    lastException = e;
                } else {
                    lastException.addSuppressed(e);
                }
            }
        }
        if (lastException != null){
            throw lastException;
        }
    }

    @Override
    public String toString() {
        return "PersisterAggregator{" +
                "persisterWithConfigurations=" + persisterWithConfigurations +
                '}';
    }
}
