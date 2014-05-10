/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AbstractForwardedTransaction<T extends AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>>>
        implements Delegator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractForwardedTransaction.class);
    private final T delegate;
    private final static CacheBuilder<Object, Object> CACHE_BUILDER = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MILLISECONDS).maximumSize(100);
    private final BindingToNormalizedNodeCodec codec;
    private final EnumMap<LogicalDatastoreType, Cache<InstanceIdentifier<?>, DataObject>> cacheMap;

    protected AbstractForwardedTransaction(final T delegate, final BindingToNormalizedNodeCodec codec) {
        super();
        this.delegate = delegate;
        this.codec = codec;

        this.cacheMap = new EnumMap<>(LogicalDatastoreType.class);
        cacheMap.put(LogicalDatastoreType.OPERATIONAL, CACHE_BUILDER.<InstanceIdentifier<?>, DataObject> build());
        cacheMap.put(LogicalDatastoreType.CONFIGURATION, CACHE_BUILDER.<InstanceIdentifier<?>, DataObject> build());

    }

    @Override
    public T getDelegate() {
        return delegate;
    }

    protected final BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected ListenableFuture<Optional<DataObject>> transformFuture(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final ListenableFuture<Optional<NormalizedNode<?, ?>>> future) {
        return Futures.transform(future, new Function<Optional<NormalizedNode<?, ?>>, Optional<DataObject>>() {
            @Nullable
            @Override
            public Optional<DataObject> apply(@Nullable final Optional<NormalizedNode<?, ?>> normalizedNode) {
                if (normalizedNode.isPresent()) {
                    final DataObject dataObject;
                    try {
                        dataObject = codec.toBinding(path, normalizedNode.get());
                    } catch (DeserializationException e) {
                        LOG.warn("Failed to create dataobject from node {}", normalizedNode.get(), e);
                        throw new IllegalStateException("Failed to create dataobject", e);
                    }

                    if (dataObject != null) {
                        updateCache(store, path, dataObject);
                        return Optional.of(dataObject);
                    }
                }
                return Optional.absent();
            }
        });
    }

    protected void doPut(final DOMDataWriteTransaction writeTransaction, final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        invalidateCache(store, path);
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = codec
                .toNormalizedNode(path, data);
        writeTransaction.put(store, normalized.getKey(), normalized.getValue());
    }

    protected void doPutWithEnsureParents(final DOMDataReadWriteTransaction writeTransaction,
            final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        invalidateCache(store, path);
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = codec
                .toNormalizedNode(path, data);

        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath = normalized.getKey();
        ensureParentsByMerge(writeTransaction, store, normalizedPath, path);
        LOG.debug("Tx: {} : Putting data {}", getDelegate().getIdentifier(), normalizedPath);
        writeTransaction.put(store, normalizedPath, normalized.getValue());
    }

    protected void doMergeWithEnsureParents(final DOMDataReadWriteTransaction writeTransaction,
            final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        invalidateCache(store, path);
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = codec
                .toNormalizedNode(path, data);

        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath = normalized.getKey();
        ensureParentsByMerge(writeTransaction, store, normalizedPath, path);
        LOG.debug("Tx: {} : Merge data {}",getDelegate().getIdentifier(),normalizedPath);
        writeTransaction.merge(store, normalizedPath, normalized.getValue());
    }

    private void ensureParentsByMerge(final DOMDataReadWriteTransaction writeTransaction,
            final LogicalDatastoreType store,
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath,
            final InstanceIdentifier<?> path) {
        List<PathArgument> currentArguments = new ArrayList<>();
        DataNormalizationOperation<?> currentOp = codec.getDataNormalizer().getRootOperation();
        Iterator<PathArgument> iterator = normalizedPath.getPath().iterator();
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
            currentArguments.add(currentArg);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier currentPath = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(
                    currentArguments);

            final Optional<NormalizedNode<?, ?>> d;
            try {
                d = writeTransaction.read(store, currentPath).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read pre-existing data from store {} path {}", store, currentPath, e);
                throw new IllegalStateException("Failed to read pre-existing data", e);
            }

            if (!d.isPresent() && iterator.hasNext()) {
                writeTransaction.merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }
    }

    protected void doMerge(final DOMDataWriteTransaction writeTransaction, final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        invalidateCache(store, path);
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = codec
                .toNormalizedNode(path, data);
        writeTransaction.merge(store, normalized.getKey(), normalized.getValue());
    }

    protected void doDelete(final DOMDataWriteTransaction writeTransaction, final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        invalidateCache(store, path);
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = codec.toNormalized(path);
        writeTransaction.delete(store, normalized);
    }

    protected ListenableFuture<RpcResult<TransactionStatus>> doCommit(final DOMDataWriteTransaction writeTransaction) {
        return writeTransaction.commit();
    }

    protected void doCancel(final DOMDataWriteTransaction writeTransaction) {
        writeTransaction.cancel();
    }

    protected ListenableFuture<Optional<DataObject>> doRead(final DOMDataReadTransaction readTransaction,
            final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        final DataObject dataObject = getFromCache(store, path);
        if (dataObject == null) {
            final ListenableFuture<Optional<NormalizedNode<?, ?>>> future = readTransaction.read(store,
                    codec.toNormalized(path));
            return transformFuture(store, path, future);
        } else {
            return Futures.immediateFuture(Optional.of(dataObject));
        }
    }

    private DataObject getFromCache(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        Cache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
        if (cache != null) {
            return cache.getIfPresent(path);
        }
        return null;
    }

    private void updateCache(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
            final DataObject dataObject) {
        // Check if cache exists. If not create one.
        Cache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
        if (cache == null) {
            cache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

        }

        cache.put(path, dataObject);
    }

    private void invalidateCache(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        // FIXME: Optimization: invalidate only parents and children of path
        Cache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
        cache.invalidateAll();
        LOG.trace("Cache invalidated");
    }

}
