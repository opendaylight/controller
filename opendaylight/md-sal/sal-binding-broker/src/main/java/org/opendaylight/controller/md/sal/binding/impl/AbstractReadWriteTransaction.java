/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class AbstractReadWriteTransaction extends AbstractWriteTransaction<DOMDataReadWriteTransaction> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractReadWriteTransaction.class);

    public AbstractReadWriteTransaction(final DOMDataReadWriteTransaction delegate, final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    protected final void doPutWithEnsureParents(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);

        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath = normalized.getKey();
        ensureParentsByMerge(store, normalizedPath, path);
        LOG.debug("Tx: {} : Putting data {}", getDelegate().getIdentifier(), normalizedPath);
        doPut(store, path, data);
    }

    protected final void doMergeWithEnsureParents(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);

        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath = normalized.getKey();
        ensureParentsByMerge(store, normalizedPath, path);
        LOG.debug("Tx: {} : Merge data {}", getDelegate().getIdentifier(), normalizedPath);
        doMerge(store, path, data);
    }

    private final void ensureParentsByMerge(final LogicalDatastoreType store,
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath,
            final InstanceIdentifier<?> path) {
        List<PathArgument> currentArguments = new ArrayList<>();
        DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
        Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
            currentArguments.add(currentArg);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier currentPath = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.create(
                    currentArguments);

            final Optional<NormalizedNode<?, ?>> d;
            try {
                d = getDelegate().read(store, currentPath).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read pre-existing data from store {} path {}", store, currentPath, e);
                throw new IllegalStateException("Failed to read pre-existing data", e);
            }

            if (!d.isPresent() && iterator.hasNext()) {
                getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }
    }


}
