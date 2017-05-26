/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;

public class DummyAdapter implements StorageAdapter, Persister {

    static int persist = 0;

    @Override
    public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
        persist++;
    }

    static int load = 0;

    @Override
    public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
        load++;
        return Collections.emptyList();
    }

    static int props = 0;

    @Override
    public Persister instantiate(PropertiesProvider propertiesProvider) {
        props++;
        return this;
    }

    @Override
    public void close() {
    }

}
