/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.directory.xml;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StorageAdapter that retrieves initial configuration from a directory. If multiple files are present, snapshot and
 * required capabilities will be merged together. Writing to this persister is not supported.
 */
public class XmlDirectoryStorageAdapter implements StorageAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(XmlDirectoryStorageAdapter.class);

    public static final String DIRECTORY_STORAGE_PROP = "directoryStorage";
    public static final String INCLUDE_EXT_PROP = "includeExtensions";
    private static final char EXTENSIONS_SEPARATOR = ',';


    @Override
    public Persister instantiate(final PropertiesProvider propertiesProvider) {
        String fileStorageProperty = propertiesProvider.getProperty(DIRECTORY_STORAGE_PROP);
        Preconditions.checkNotNull(fileStorageProperty, "Unable to find " + propertiesProvider.getFullKeyForReporting(DIRECTORY_STORAGE_PROP));
        File storage  = new File(fileStorageProperty);
        String fileExtensions = propertiesProvider.getProperty(INCLUDE_EXT_PROP);

        LOG.debug("Using storage: {}", storage);

        if(fileExtensions != null) {
            LOG.debug("Using extensions: {}", fileExtensions);
            return new XmlDirectoryPersister(storage, splitExtensions(fileExtensions));
        } else {
            return new XmlDirectoryPersister(storage);
        }
    }

    private Set<String> splitExtensions(final String fileExtensions) {
        return Sets.newHashSet(Splitter.on(EXTENSIONS_SEPARATOR).trimResults().omitEmptyStrings()
                .split(fileExtensions));
    }

}
