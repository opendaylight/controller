/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CachedBindingDataWriteTransaction<T extends DOMDataWriteTransaction> extends
        BindingDataWriteTransactionImpl<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CachedBindingDataWriteTransaction.class);

    private InstanceIdentifier<?> cachedPath;
    private NormalizedNodeCache cache;

    protected CachedBindingDataWriteTransaction(T delegate, BindingToNormalizedNodeCodec codec,
                                                InstanceIdentifier<?> cachedPath, NormalizedNodeCache cache) {
        super(delegate, codec);
        this.cachedPath = cachedPath;
        this.cache = cache;
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        ensureListParentIfNeeded(store,path,normalized);
        if (!cachedPath.containsWildcarded(path)) {
            getDelegate().put(store, normalized.getKey(), normalized.getValue());
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = cache.getReference(normalized.getValue());

        if (potentialReference.isPresent()) {
            getDelegate().put(store, normalized.getKey(), potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            cache.putObjectWithChildNodes(normalized.getValue());
            getDelegate().put(store, normalized.getKey(), normalized.getValue());
            LOG.debug("Node '{}' was not found in the cache", data);
        }
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        ensureListParentIfNeeded(store,path,normalized);
        getDelegate().put(store, normalized.getKey(), normalized.getValue());
        if (!cachedPath.containsWildcarded(path)) {
            getDelegate().merge(store, normalized.getKey(), normalized.getValue());
        }
        Optional<? extends NormalizedNode<?, ?>> potentialReference = cache.getReference(normalized.getValue());

        if (potentialReference.isPresent()) {
            getDelegate().merge(store, normalized.getKey(), potentialReference.get());
            LOG.debug("Node '{}' was found in the cache", data);
        } else {
            cache.putObjectWithChildNodes(normalized.getValue());
            getDelegate().merge(store, normalized.getKey(), normalized.getValue());
            LOG.debug("Node '{}' was not found in the cache", data);
        }
    }
}
