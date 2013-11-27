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
import java.util.SortedSet;

/**
 * Base interface for persister implementation.
 */
public interface Persister extends Closeable {

    void persistConfig(ConfigSnapshotHolder configSnapshotHolder) throws IOException;

    Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException;

    public static interface ConfigSnapshotHolder {

        /**
         * Get part of get-config document that contains just
         */
        String getConfigSnapshot();


        /**
         * Get only required capabilities referenced by the snapshot.
         */
        SortedSet<String> getCapabilities();
    }
}
