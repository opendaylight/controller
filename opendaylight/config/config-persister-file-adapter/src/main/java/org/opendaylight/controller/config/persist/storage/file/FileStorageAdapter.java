/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * StorageAdapter that stores configuration in a plan file.
 */
public class FileStorageAdapter implements StorageAdapter {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageAdapter.class);

    // TODO prefix properties

    private static final Charset ENCODING = Charsets.UTF_8;

    public static final String FILE_STORAGE_PROP = "fileStorage";
    public static final String NUMBER_OF_BACKUPS = "numberOfBackups";

    private static final String SEPARATOR_E_PURE = "//END OF CONFIG";
    private static final String SEPARATOR_E = newLine(SEPARATOR_E_PURE);

    private static final String SEPARATOR_M_PURE = "//END OF SNAPSHOT";
    private static final String SEPARATOR_M = newLine(SEPARATOR_M_PURE);

    private static final String SEPARATOR_S = newLine("//START OF CONFIG");

    private static final String SEPARATOR_SL_PURE = "//START OF CONFIG-LAST";
    private static final String SEPARATOR_SL = newLine(SEPARATOR_SL_PURE);

    private static Integer numberOfStoredBackups;
    private File storage;

    @Override
    public void setProperties(BundleContext bundleContext) {
        File storage = extractStorageFileFromProperties(bundleContext);
        logger.debug("Using file {}", storage.getAbsolutePath());
        // Create file if it does not exist
        File parentFile = storage.getAbsoluteFile().getParentFile();
        if (parentFile.exists() == false) {
            logger.debug("Creating parent folders {}", parentFile);
            parentFile.mkdirs();
        }
        if (storage.exists() == false) {
            logger.debug("Storage file does not exist, creating empty file");
            try {
                boolean result = storage.createNewFile();
                if (result == false)
                    throw new RuntimeException("Unable to create storage file " + storage);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create storage file " + storage, e);
            }
        }
        if (numberOfStoredBackups == 0) {
            throw new RuntimeException(NUMBER_OF_BACKUPS
                    + " property should be either set to positive value, or ommited. Can not be set to 0.");
        }
        setFileStorage(storage);

    }

    @VisibleForTesting
    void setFileStorage(File storage) {
        this.storage = storage;
    }

    @VisibleForTesting
    void setNumberOfBackups(Integer numberOfBackups) {
        numberOfStoredBackups = numberOfBackups;
    }

    private static File extractStorageFileFromProperties(BundleContext bundleContext) {
        String fileStorageProperty = bundleContext.getProperty(FILE_STORAGE_PROP);
        Preconditions.checkNotNull(fileStorageProperty, "Unable to find " + FILE_STORAGE_PROP
                + " in received context :" + bundleContext);
        File result = new File(fileStorageProperty);
        String numberOfBAckupsAsString = bundleContext.getProperty(NUMBER_OF_BACKUPS);
        if (numberOfBAckupsAsString != null) {
            numberOfStoredBackups = Integer.valueOf(numberOfBAckupsAsString);
        } else {
            numberOfStoredBackups = Integer.MAX_VALUE;
        }

        return result;
    }

    private static String newLine(String string) {
        return string + "\n";
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        Preconditions.checkNotNull(storage, "Storage file is null");

        String content = Files.toString(storage, ENCODING);
        if (numberOfStoredBackups == Integer.MAX_VALUE) {
            resetLastConfig(content);
            persistLastConfig(holder);
        } else {
            if (numberOfStoredBackups == 1) {
                Files.write("", storage, ENCODING);
                persistLastConfig(holder);
            } else {
                int count = StringUtils.countMatches(content, SEPARATOR_S);
                if ((count + 1) < numberOfStoredBackups) {
                    resetLastConfig(content);
                    persistLastConfig(holder);
                } else {
                    String contentSubString = StringUtils.substringBefore(content, SEPARATOR_E);
                    contentSubString = contentSubString.concat(SEPARATOR_E_PURE);
                    content = StringUtils.substringAfter(content, contentSubString);
                    resetLastConfig(content);
                    persistLastConfig(holder);
                }
            }
        }
    }

    private void resetLastConfig(String content) throws IOException {
        content = content.replaceFirst(SEPARATOR_SL, SEPARATOR_S);
        Files.write(content, storage, ENCODING);
    }

    private void persistLastConfig(ConfigSnapshotHolder holder) throws IOException {
        Files.append(SEPARATOR_SL, storage, ENCODING);
        String snapshotAsString = holder.getConfigSnapshot();
        Files.append(newLine(snapshotAsString), storage, ENCODING);
        Files.append(SEPARATOR_M, storage, ENCODING);
        Files.append(toStringCaps(holder.getCapabilities()), storage, ENCODING);
        Files.append(SEPARATOR_E, storage, ENCODING);
    }

    private CharSequence toStringCaps(Set<String> capabilities) {
        StringBuilder b = new StringBuilder();
        for (String capability : capabilities) {
            b.append(newLine(capability));
        }
        return b.toString();
    }

    @Override
    public Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException {
        Preconditions.checkNotNull(storage, "Storage file is null");

        if (!storage.exists()) {
            return Optional.absent();
        }

        final LineProcessor lineProcessor = new LineProcessor();
        String result = Files.readLines(storage, ENCODING, lineProcessor);

        try {
            if (lineProcessor.getConfigSnapshot().isPresent() == false) {
                return Optional.absent();
            } else {
                return Optional.<ConfigSnapshotHolder> of(new PersistedConfigImpl(lineProcessor.getConfigSnapshot(),
                        lineProcessor.getCapabilities()));
            }

        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Unable to load last config ", e);
        }
    }

    private static final class LineProcessor implements com.google.common.io.LineProcessor<String> {

        private boolean inLastConfig, inLastSnapshot;
        private final StringBuffer snapshotBuffer = new StringBuffer();
        private final Set<String> caps = Sets.newHashSet();

        @Override
        public String getResult() {
            return null;
        }

        @Override
        public boolean processLine(String line) throws IOException {
            if (inLastConfig && line.equals(SEPARATOR_E_PURE)) {
                inLastConfig = false;
                return false;
            }

            if (inLastConfig && line.equals(SEPARATOR_M_PURE)) {
                inLastSnapshot = false;
                return true;
            }

            if (inLastConfig) {
                if (inLastSnapshot) {
                    snapshotBuffer.append(line);
                    snapshotBuffer.append(System.lineSeparator());
                } else {
                    caps.add(line);
                }
            }

            if (line.equals(SEPARATOR_SL_PURE)) {
                inLastConfig = true;
                inLastSnapshot = true;
            }

            return true;
        }

        Optional<String> getConfigSnapshot() throws IOException, SAXException, ParserConfigurationException {
            final String xmlContent = snapshotBuffer.toString();
            if (xmlContent == null || xmlContent.equals("")) {
                return Optional.absent();
            } else
                return Optional.of(xmlContent);
        }

        Set<String> getCapabilities() throws IOException, SAXException, ParserConfigurationException {
            return caps;
        }

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String toString() {
        return "FileStorageAdapter [storage=" + storage + "]";
    }

    private class PersistedConfigImpl implements ConfigSnapshotHolder {

        private final String snapshot;
        private final Set<String> caps;

        public PersistedConfigImpl(Optional<String> configSnapshot, Set<String> capabilities) {
            this.snapshot = configSnapshot.get();
            this.caps = capabilities;
        }

        @Override
        public String getConfigSnapshot() {
            return snapshot;
        }

        @Override
        public Set<String> getCapabilities() {
            return caps;
        }
    }

}
