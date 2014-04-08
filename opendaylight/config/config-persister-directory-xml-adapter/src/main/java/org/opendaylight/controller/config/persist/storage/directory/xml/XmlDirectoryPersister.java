/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.xml;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkArgument;

public class XmlDirectoryPersister implements Persister {
    private static final Logger logger = LoggerFactory.getLogger(XmlDirectoryPersister.class);

    private final File storage;
    private final Optional<FilenameFilter> extensionsFilter;

    /**
     * Creates XmlDirectoryPersister that picks up all files in specified folder
     */
    public XmlDirectoryPersister(File storage) {
        this(storage, Optional.<FilenameFilter>absent());
    }

    /**
     * Creates XmlDirectoryPersister that picks up files only with specified file extension
     */
    public XmlDirectoryPersister(File storage, Set<String> fileExtensions) {
        this(storage, Optional.of(getFilter(fileExtensions)));
    }

    private XmlDirectoryPersister(File storage, Optional<FilenameFilter> extensionsFilter) {
        checkArgument(storage.exists() && storage.isDirectory(), "Storage directory does not exist: " + storage);
        this.storage = storage;
        this.extensionsFilter = extensionsFilter;
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        throw new UnsupportedOperationException("This adapter is read only. Please set readonly=true on " + getClass());
    }

    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
        File[] filesArray = extensionsFilter.isPresent() ? storage.listFiles(extensionsFilter.get()) : storage.listFiles();
        if (filesArray == null || filesArray.length == 0) {
            return Collections.emptyList();
        }
        List<File> sortedFiles = new ArrayList<>(Arrays.asList(filesArray));
        Collections.sort(sortedFiles);
        // combine all found files
        logger.debug("Reading files in following order: {}", sortedFiles);

        List<ConfigSnapshotHolder> result = new ArrayList<>();
        for (File file : sortedFiles) {
            logger.trace("Adding file '{}' to combined result", file);
            ConfigSnapshotHolder h = fromXmlSnapshot(file);
            result.add(h);
        }
        return result;
    }

    private ConfigSnapshotHolder fromXmlSnapshot(File file) {
        try {
            return loadLastConfig(file);
        } catch (JAXBException e) {
            logger.warn("Unable to restore configuration snapshot from {}", file, e);
            throw new IllegalStateException("Unable to restore configuration snapshot from " + file, e);
        }
    }

    public static ConfigSnapshotHolder loadLastConfig(File file) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigSnapshot.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        return asHolder((ConfigSnapshot) um.unmarshal(file));
    }

    private static ConfigSnapshotHolder asHolder(final ConfigSnapshot unmarshalled) {
        return new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return unmarshalled.getConfigSnapshot();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return unmarshalled.getCapabilities();
            }

            @Override
            public String toString() {
                return unmarshalled.toString();
            }
        };
    }

    private static FilenameFilter getFilter(final Set<String>fileExtensions) {
        checkArgument(fileExtensions.isEmpty() == false, "No file extension provided", fileExtensions);

        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String ext = Files.getFileExtension(name);
                return fileExtensions.contains(ext);
            }
        };
    }

    @Override
    public void close() {

    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("XmlDirectoryPersister{");
        sb.append("storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}