/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.file.xml;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.opendaylight.controller.config.persist.storage.file.xml.model.Config;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StorageAdapter that stores configuration in an xml file.
 */
public class XmlFileStorageAdapter implements StorageAdapter, Persister {
    private static final Logger LOG = LoggerFactory.getLogger(XmlFileStorageAdapter.class);

    public static final String FILE_STORAGE_PROP = "fileStorage";
    public static final String NUMBER_OF_BACKUPS = "numberOfBackups";

    private static Integer numberOfStoredBackups;
    private File storage;

    private static volatile XmlFileStorageAdapter instance;
    private volatile ConfigSnapshot lastCfgSnapshotCache;
    private volatile Optional<FeatureListProvider> featuresService = Optional.absent();

    @VisibleForTesting
    public void reset() {
        instance = null;
        lastCfgSnapshotCache = null;
        featuresService = null;
    }

    @Override
    public Persister instantiate(PropertiesProvider propertiesProvider) {
        if(instance != null) {
            return instance;
        }

        File storage = extractStorageFileFromProperties(propertiesProvider);
        LOG.debug("Using file {}", storage.getAbsolutePath());
        // Create file if it does not exist
        File parentFile = storage.getAbsoluteFile().getParentFile();
        if (parentFile.exists() == false) {
            LOG.debug("Creating parent folders {}", parentFile);
            parentFile.mkdirs();
        }
        if (storage.exists() == false) {
            LOG.debug("Storage file does not exist, creating empty file");
            try {
                boolean result = storage.createNewFile();
                if (result == false) {
                    throw new RuntimeException("Unable to create storage file " + storage);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to create storage file " + storage, e);
            }
        }
        if (numberOfStoredBackups == 0) {
            throw new RuntimeException(NUMBER_OF_BACKUPS
                    + " property should be either set to positive value, or ommited. Can not be set to 0.");
        }
        setFileStorage(storage);

        instance = this;
        return this;
    }

    public static Optional<XmlFileStorageAdapter> getInstance() {
        return Optional.fromNullable(instance);
    }

    public Set<String> getPersistedFeatures() {
        return lastCfgSnapshotCache == null ? Collections.<String>emptySet() : lastCfgSnapshotCache.getFeatures();
    }

    public void setFeaturesService(final FeatureListProvider featuresService) {
        this.featuresService = Optional.of(featuresService);
    }

    @VisibleForTesting
    public void setFileStorage(File storage) {
        this.storage = storage;
    }

    @VisibleForTesting
    public void setNumberOfBackups(Integer numberOfBackups) {
        numberOfStoredBackups = numberOfBackups;
    }

    private static File extractStorageFileFromProperties(PropertiesProvider propertiesProvider) {
        String fileStorageProperty = propertiesProvider.getProperty(FILE_STORAGE_PROP);
        Preconditions.checkNotNull(fileStorageProperty, "Unable to find " + propertiesProvider.getFullKeyForReporting(FILE_STORAGE_PROP));
        File result = new File(fileStorageProperty);
        String numberOfBackupsAsString = propertiesProvider.getProperty(NUMBER_OF_BACKUPS);
        if (numberOfBackupsAsString != null) {
            numberOfStoredBackups = Integer.valueOf(numberOfBackupsAsString);
        } else {
            numberOfStoredBackups = Integer.MAX_VALUE;
        }
        LOG.trace("Property {} set to {}", NUMBER_OF_BACKUPS, numberOfStoredBackups);
        return result;
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        Preconditions.checkNotNull(storage, "Storage file is null");

        Set<String> installedFeatureIds = Collections.emptySet();
        if(featuresService.isPresent()) {
            installedFeatureIds = featuresService.get().listFeatures();
        }

        Config cfg = Config.fromXml(storage);
        cfg.addConfigSnapshot(ConfigSnapshot.fromConfigSnapshot(holder, installedFeatureIds), numberOfStoredBackups);
        cfg.toXml(storage);
    }

    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
        Preconditions.checkNotNull(storage, "Storage file is null");

        if (!storage.exists()) {
            return Collections.emptyList();
        }

        Optional<ConfigSnapshot> lastSnapshot = Config.fromXml(storage).getLastSnapshot();

        if (lastSnapshot.isPresent()) {
            lastCfgSnapshotCache = lastSnapshot.get();
            return Lists.newArrayList(toConfigSnapshot(lastCfgSnapshotCache));
        } else {
            return Collections.emptyList();
        }
    }


    public ConfigSnapshotHolder toConfigSnapshot(final ConfigSnapshot configSnapshot) {
        return new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return configSnapshot.getConfigSnapshot();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return configSnapshot.getCapabilities();
            }

            @Override
            public String toString() {
                return configSnapshot.toString();
            }
        };
    }

    @Override
    public void close() {

    }

    @Override
    public String toString() {
        return "XmlFileStorageAdapter [storage=" + storage + "]";
    }

}
