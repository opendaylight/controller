/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class NoOpStorageAdapter implements StorageAdapter, Persister {
    private static final Logger logger = LoggerFactory.getLogger(NoOpStorageAdapter.class);

    @Override
    public Persister instantiate(PropertiesProvider propertiesProvider) {
        logger.debug("instantiate called with {}", propertiesProvider);
        return this;
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        logger.debug("persistConfig called with {}", holder);
    }

    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
        logger.debug("loadLastConfig called");
        return Collections.emptyList();
    }

    @Override
    public void close() {
        logger.debug("close called");
    }
}
