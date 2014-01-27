/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.api;

import java.util.SortedSet;

public class ConfigSnapshotHolderImpl implements ConfigSnapshotHolder {

    private final String snapshot;
    private final SortedSet<String> caps;
    private final String fileName;

    public ConfigSnapshotHolderImpl(String configSnapshot, SortedSet<String> capabilities, String fileName) {
        this.snapshot = configSnapshot;
        this.caps = capabilities;
        this.fileName = fileName;
    }

    @Override
    public String getConfigSnapshot() {
        return snapshot;
    }

    @Override
    public SortedSet<String> getCapabilities() {
        return caps;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "ConfigSnapshotHolderImpl{" +
                "snapshot='" + snapshot + '\'' +
                ", caps=" + caps +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
