/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedSnapshotBackedWriteTransaction implements DOMStoreWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(CachedSnapshotBackedWriteTransaction.class);

    private NormalizedNodeCache normalizedNodeCache;
    private YangInstanceIdentifier cachingWildcardedPath;
    private DOMStoreWriteTransaction delegate;

    public CachedSnapshotBackedWriteTransaction(NormalizedNodeCache normalizedNodeCache,
                                                YangInstanceIdentifier cachingWildcardedPath,
                                                DOMStoreWriteTransaction delegate) {
        this.normalizedNodeCache = normalizedNodeCache;
        this.cachingWildcardedPath = cachingWildcardedPath;
        this.delegate = delegate;
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        // only cache if the path is contained under specified wildcard path
        if (!checkIfIsInPath(path)) {
            delegate.write(path, data);
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = normalizedNodeCache.getReference(data);
        if (potentialReference.isPresent()) {
            delegate.write(path, potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            normalizedNodeCache.putObjectWithChildNodes(data);
            delegate.write(path, data);
            LOG.debug("Node '{}' was not found in the cache", data);
        }
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        if (!checkIfIsInPath(path)) {
            delegate.merge(path, data);
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = normalizedNodeCache.getReference(data);

        if (potentialReference.isPresent()) {
            delegate.merge(path, potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            normalizedNodeCache.putObjectWithChildNodes(data);
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

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void close() {
        delegate.close();
    }

    public boolean checkIfIsInPath(YangInstanceIdentifier path) {
        return cachingWildcardedPath.contains(path);
    }
}
