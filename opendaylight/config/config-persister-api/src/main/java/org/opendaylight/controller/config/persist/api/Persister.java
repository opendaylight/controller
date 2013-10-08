/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.api;

import com.google.common.base.Optional;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 * Base interface for persister implementation.
 */
public interface Persister extends Closeable {

    void persistConfig(ConfigSnapshotHolder configSnapshotHolder) throws IOException;

    Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException;

    public static interface ConfigSnapshotHolder {

        String getConfigSnapshot();

        Set<String> getCapabilities();
    }
}
