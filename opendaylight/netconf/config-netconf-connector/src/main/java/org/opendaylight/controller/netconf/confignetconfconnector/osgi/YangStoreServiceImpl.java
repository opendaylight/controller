/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

import javax.annotation.concurrent.GuardedBy;

public class YangStoreServiceImpl implements YangStoreService {
    private final SchemaContextProvider service;
    @GuardedBy("this")
    private YangStoreSnapshotImpl cache = null;

    public YangStoreServiceImpl(SchemaContextProvider service) {
        this.service = service;
    }

    @Override
    public synchronized YangStoreSnapshotImpl getYangStoreSnapshot() throws YangStoreException {
        if (cache == null) {
            cache = new YangStoreSnapshotImpl(service.getSchemaContext());
        }
        return cache;
    }

    /**
     * Called when schema context changes, invalidates cache.
     */
    public synchronized void refresh() {
        cache = null;
    }
}
