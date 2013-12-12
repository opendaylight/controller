/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.directory.xml;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * StorageAdapter that retrieves initial configuration from a directory. If multiple files are present, snapshot and
 * required capabilities will be merged together. Writing to this persister is not supported.
 */
public class XmlDirectoryStorageAdapter implements StorageAdapter {
    private static final Logger logger = LoggerFactory.getLogger(XmlDirectoryStorageAdapter.class);

    public static final String DIRECTORY_STORAGE_PROP = "directoryStorage";


    @Override
    public Persister instantiate(PropertiesProvider propertiesProvider) {
        String fileStorageProperty = propertiesProvider.getProperty(DIRECTORY_STORAGE_PROP);
        Preconditions.checkNotNull(fileStorageProperty, "Unable to find " + propertiesProvider.getFullKeyForReporting(DIRECTORY_STORAGE_PROP));
        File storage  = new File(fileStorageProperty);
        logger.debug("Using {}", storage);
        return new XmlDirectoryPersister(storage);
    }

}
