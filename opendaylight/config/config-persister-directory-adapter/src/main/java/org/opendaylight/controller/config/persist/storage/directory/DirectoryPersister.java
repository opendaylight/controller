/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolderImpl;
import org.opendaylight.controller.config.persist.api.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class DirectoryPersister implements Persister {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryPersister.class);
    private static final Charset ENCODING = Charsets.UTF_8;

    public static final String MODULES_START = "//MODULES START";
    static final String SERVICES_START = "//SERVICES START";
    static final String CAPABILITIES_START = "//CAPABILITIES START";


    private final File storage;
    private static final String header, middle, footer;

    static {
        header = readResource("header.txt");
        middle = readResource("middle.txt");
        footer = readResource("footer.txt");
    }

    public DirectoryPersister(File storage) {
        checkArgument(storage.exists() && storage.isDirectory(), "Storage directory does not exist: " + storage);
        this.storage = storage;
    }

    private static String readResource(String resource) {
        try {
            return IOUtils.toString(DirectoryPersister.class.getResourceAsStream("/" + resource));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + resource, e);
        }
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

            ConfigSnapshotHolder configSnapshotHolder = loadLastConfig(file);
            result.add(configSnapshotHolder);
        }
        return result;
    }

    public static ConfigSnapshotHolder loadLastConfig(File file) throws IOException {
        final MyLineProcessor lineProcessor = new MyLineProcessor(file.getAbsolutePath());
        Files.readLines(file, ENCODING, lineProcessor);
        return lineProcessor.getConfigSnapshotHolder(header, middle, footer);
    }


    @Override
    public void close() {

    }

    @Override
    public String toString() {
        return "FileStorageAdapter [storage=" + storage + "]";
    }
}

class MyLineProcessor implements com.google.common.io.LineProcessor<String> {
    private final String fileNameForReporting;

    private boolean inModules, inServices, inCapabilities;
    private final StringBuffer modulesBuffer = new StringBuffer(), servicesBuilder = new StringBuffer();
    private final SortedSet<String> caps = new TreeSet<>();

    MyLineProcessor(String fileNameForReporting) {
        this.fileNameForReporting = fileNameForReporting;
    }

    @Override
    public String getResult() {
        return null;
    }

    @Override
    public boolean processLine(String line) throws IOException {

        String lineWithNewLine = line + System.lineSeparator();
        if (line.equals(DirectoryPersister.MODULES_START)) {
            checkState(inModules == false && inServices == false && inCapabilities == false);
            inModules = true;
        } else if (line.equals(DirectoryPersister.SERVICES_START)) {
            checkState(inModules == true && inServices == false && inCapabilities == false);
            inModules = false;
            inServices = true;
        } else if (line.equals(DirectoryPersister.CAPABILITIES_START)) {
            checkState(inModules == false && inServices == true && inCapabilities == false);
            inServices = false;
            inCapabilities = true;
        } else if (inModules) {
            modulesBuffer.append(lineWithNewLine);
        } else if (inServices) {
            servicesBuilder.append(lineWithNewLine);
        } else {
            caps.add(line);
        }
        return true;
    }

    private void checkFileConsistency(){
        checkState(inCapabilities, "File %s is missing delimiters in this order: %s", fileNameForReporting,
                Arrays.asList(DirectoryPersister.MODULES_START,
                        DirectoryPersister.SERVICES_START,
                        DirectoryPersister.CAPABILITIES_START));
    }

    String getModules() {
        checkFileConsistency();
        return modulesBuffer.toString();
    }

    String getServices() {
        checkFileConsistency();
        return servicesBuilder.toString();
    }

    SortedSet<String> getCapabilities() {
        checkFileConsistency();
        return caps;
    }

    ConfigSnapshotHolder getConfigSnapshotHolder(String header, String middle, String footer) {
        String combinedSnapshot = header + getModules() + middle + getServices() + footer;
        ConfigSnapshotHolder result = new ConfigSnapshotHolderImpl(combinedSnapshot, getCapabilities(), fileNameForReporting);
        return result;
    }

}

