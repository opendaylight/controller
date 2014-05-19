/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.lang.ref.SoftReference;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

import javax.annotation.concurrent.GuardedBy;

public class YangStoreServiceImpl implements YangStoreService {
    private final SchemaContextProvider service;
    @GuardedBy("this")
    private SoftReference<YangStoreSnapshotImpl> cache = new SoftReference<>(null);

    public YangStoreServiceImpl(SchemaContextProvider service) {
        this.service = service;
    }

    @Override
    public synchronized YangStoreSnapshotImpl getYangStoreSnapshot() throws YangStoreException {
        YangStoreSnapshotImpl yangStoreSnapshot = cache.get();
        if (yangStoreSnapshot == null) {
            yangStoreSnapshot = new YangStoreSnapshotImpl(service.getSchemaContext());
            cache = new SoftReference<>(yangStoreSnapshot);
        }
        return yangStoreSnapshot;
    }

    /**
     * Called when schema context changes, invalidates cache.
     */
    public synchronized void refresh() {
        cache.clear();
    }
}
