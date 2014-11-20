/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangStoreServiceImpl implements YangStoreService {
    private static final Logger LOG = LoggerFactory.getLogger(YangStoreServiceImpl.class);

    /**
     * This is a rather interesting locking model. We need to guard against both the
     * cache expiring from GC and being invalidated by schema context change. The
     * context can change while we are doing processing, so we do not want to block
     * it, so no synchronization can happen on the methods.
     *
     * So what we are doing is the following:
     *
     * We synchronize with GC as usual, using a SoftReference.
     *
     * The atomic reference is used to synchronize with {@link #refresh()}, e.g. when
     * refresh happens, it will push a SoftReference(null), e.g. simulate the GC. Now
     * that may happen while the getter is already busy acting on the old schema context,
     * so it needs to understand that a refresh has happened and retry. To do that, it
     * attempts a CAS operation -- if it fails, in knows that the SoftReference has
     * been replaced and thus it needs to retry.
     *
     * Note that {@link #getYangStoreSnapshot()} will still use synchronize() internally
     * to stop multiple threads doing the same work.
     */
    private final AtomicReference<SoftReference<YangStoreSnapshotImpl>> ref = new AtomicReference<>(new SoftReference<YangStoreSnapshotImpl>(null));
    private final SchemaContextProvider service;

    public YangStoreServiceImpl(final SchemaContextProvider service) {
        this.service = service;
    }

    @Override
    public synchronized YangStoreSnapshotImpl getYangStoreSnapshot() throws YangStoreException {
        SoftReference<YangStoreSnapshotImpl> r = ref.get();
        YangStoreSnapshotImpl ret = r.get();

        while (ret == null) {
            // We need to be compute a new value
            ret = new YangStoreSnapshotImpl(service.getSchemaContext());

            if (!ref.compareAndSet(r, new SoftReference<>(ret))) {
                LOG.debug("Concurrent refresh detected, recomputing snapshot");
                r = ref.get();
                ret = null;
            }
        }

        return ret;
    }

    /**
     * Called when schema context changes, invalidates cache.
     */
    public void refresh() {
        ref.set(new SoftReference<YangStoreSnapshotImpl>(null));
    }
}
