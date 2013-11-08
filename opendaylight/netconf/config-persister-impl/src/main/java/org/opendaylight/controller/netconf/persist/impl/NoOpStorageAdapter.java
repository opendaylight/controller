/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NoOpStorageAdapter implements StorageAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NoOpStorageAdapter.class);

    @Override
    public void setProperties(BundleContext bundleContext) {
        logger.debug("setProperties called with {}", bundleContext);
    }

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        logger.debug("persistConfig called with {}", holder);
    }

    @Override
    public Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException {
        logger.debug("loadLastConfig called");
        return Optional.absent();
    }

    @Override
    public void close() throws IOException {
        logger.debug("close called");
    }
}
