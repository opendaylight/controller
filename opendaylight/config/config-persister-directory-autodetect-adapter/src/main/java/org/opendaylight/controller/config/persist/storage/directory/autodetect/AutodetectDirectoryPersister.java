/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.autodetect;

import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.storage.directory.DirectoryPersister;
import org.opendaylight.controller.config.persist.storage.directory.xml.XmlDirectoryPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class AutodetectDirectoryPersister implements Persister {
    private static final Logger logger = LoggerFactory.getLogger(AutodetectDirectoryPersister.class);

    private final File storage;

    public AutodetectDirectoryPersister(File storage) {
        checkArgument(storage.exists() && storage.isDirectory(), "Storage directory does not exist: " + storage);
        this.storage = storage;
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        throw new UnsupportedOperationException("This adapter is read only. Please set readonly=true on " + getClass());
    }

    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
        File[] filesArray = storage.listFiles();
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

            FileType fileType = FileType.getFileType(file);
            logger.trace("File '{}' detected as {} storage", file, fileType);

            ConfigSnapshotHolder snapshot = loadLastConfig(file, fileType);
            result.add(snapshot);
        }
        return result;
    }

    private ConfigSnapshotHolder loadLastConfig(File file, FileType fileType) throws IOException {
        switch (fileType) {
        case plaintext:
            logger.warn("Plaintext configuration files are deprecated, and will not be supported in future versions. " +
                    "Use xml files instead");
            return DirectoryPersister.loadLastConfig(file);
        case xml:
            try {
                return XmlDirectoryPersister.loadLastConfig(file);
            } catch (JAXBException e) {
                logger.warn("Unable to restore configuration snapshot from {}", file, e);
                throw new IllegalStateException("Unable to restore configuration snapshot from " + file, e);
            }
        default:
            throw new IllegalStateException("Unknown storage type " + fileType);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AutodetectDirectoryPersister{");
        sb.append("storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}
