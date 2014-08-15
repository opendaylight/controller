/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.cached.store.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedSnapshotBackedReadWriteTransaction implements DOMStoreReadWriteTransaction{
    private static final Logger LOG = LoggerFactory.getLogger(CachedSnapshotBackedReadWriteTransaction.class);

    private NormalizedNodeCache normalizedNodeCache;
    private YangInstanceIdentifier cachingWildcardedPath;
    private DOMStoreReadWriteTransaction delegate;

    public CachedSnapshotBackedReadWriteTransaction(NormalizedNodeCache normalizedNodeCache,
                                                    YangInstanceIdentifier cachingWildcardedPath,
                                                    DOMStoreReadWriteTransaction delegate) {
        this.normalizedNodeCache = normalizedNodeCache;
        this.cachingWildcardedPath = cachingWildcardedPath;
        this.delegate = delegate;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        return delegate.read(path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        return delegate.exists(path);
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        if (!checkIfIsInPath(path)) {
            delegate.write(path, data);
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = normalizedNodeCache.getReference(data);
        if (potentialReference.isPresent()) {
            delegate.write(path, potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            normalizedNodeCache.putObjectWithChildNodes(data);
//            normalizedNodeCache.putObject(data);
            delegate.write(path, data);
            LOG.debug("Node '{}' was not found in the cache", data);
        }
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        if (!checkIfIsInPath(path)) {
            delegate.write(path, data);
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = normalizedNodeCache.getReference(data);
        if (potentialReference.isPresent()) {
            delegate.merge(path, potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            normalizedNodeCache.putObjectWithChildNodes(data);
//            normalizedNodeCache.putObject(data);
            delegate.merge(path, data);
            LOG.debug("Node '{}' was not found in the cache", data);
        }
    }

    @Override
    public void delete(YangInstanceIdentifier path) {
        delegate.delete(path);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        return delegate.ready();
    }

    public boolean checkIfIsInPath(YangInstanceIdentifier path) {
        return cachingWildcardedPath.contains(path);
    }
}
