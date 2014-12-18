/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.xml;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlDirectoryPersister implements Persister {
    private static final Logger LOG = LoggerFactory.getLogger(XmlDirectoryPersister.class);

    private final File storage;
    private final Optional<FilenameFilter> extensionsFilter;

    /**
     * Creates XmlDirectoryPersister that picks up all files in specified folder
     */
    public XmlDirectoryPersister(final File storage) {
        this(storage, Optional.<FilenameFilter>absent());
    }

    /**
     * Creates XmlDirectoryPersister that picks up files only with specified file extension
     */
    public XmlDirectoryPersister(final File storage, final Set<String> fileExtensions) {
        this(storage, Optional.of(getFilter(fileExtensions)));
    }

    private XmlDirectoryPersister(final File storage, final Optional<FilenameFilter> extensionsFilter) {
        checkArgument(storage.exists() && storage.isDirectory(), "Storage directory does not exist: " + storage);
        this.storage = storage;
        this.extensionsFilter = extensionsFilter;
    }

    @Override
    public void persistConfig(final ConfigSnapshotHolder holder) throws IOException {
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
        LOG.debug("Reading files in following order: {}", sortedFiles);

        List<ConfigSnapshotHolder> result = new ArrayList<>();
        for (File file : sortedFiles) {
            LOG.trace("Adding file '{}' to combined result", file);
            Optional<ConfigSnapshotHolder> h = fromXmlSnapshot(file);
            // Ignore non valid snapshot
            if(h.isPresent() == false) {
                continue;
            }

            result.add(h.get());
        }
        return result;
    }

    private Optional<ConfigSnapshotHolder> fromXmlSnapshot(final File file) {
        try {
            return Optional.of(loadLastConfig(file));
        } catch (JAXBException e) {
            // In case of parse error, issue a warning, ignore and continue
            LOG.warn(
                    "Unable to parse configuration snapshot from {}. Initial config from {} will be IGNORED in this run. ",
                    file, file);
            LOG.warn(
                    "Note that subsequent config files may fail due to this problem. ",
                    "Xml markup in this file needs to be fixed, for detailed information see enclosed exception.",
                    e);
        }

        return Optional.absent();
    }

    public static ConfigSnapshotHolder loadLastConfig(final File file) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigSnapshot.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        try {
            XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(file));
            return asHolder((ConfigSnapshot) um.unmarshal(xsr));
        } catch (final XMLStreamException e) {
            throw new JAXBException(e);
        }
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
            public boolean accept(final File dir, final String name) {
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