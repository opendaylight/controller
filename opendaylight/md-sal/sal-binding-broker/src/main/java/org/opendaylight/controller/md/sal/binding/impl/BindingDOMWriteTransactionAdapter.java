/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

class BindingDOMWriteTransactionAdapter<T extends DOMDataWriteTransaction> extends
        AbstractWriteTransaction<T> implements WriteTransaction {

    protected BindingDOMWriteTransactionAdapter(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        super(delegateTx, codec);
    }

    @Override
    public <U extends DataObject> void put(final LogicalDatastoreType store, final InstanceIdentifier<U> path,
                                           final U data) {
        put(store, path, data,false);
    }

    @Override
    public <D extends DataObject> void merge(final LogicalDatastoreType store, final InstanceIdentifier<D> path,
                                             final D data) {
        merge(store, path, data,false);
    }


    @Override
    protected void ensureParentsByMerge(final LogicalDatastoreType store,
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier normalizedPath,
            final InstanceIdentifier<?> path) {
        List<PathArgument> currentArguments = new ArrayList<>();
        DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
        for (PathArgument currentArg : normalizedPath.getPathArguments()) {
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
            currentArguments.add(currentArg);
            YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(
                    currentArguments);

            getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
        }
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        doDelete(store, path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return doCommit();
    }

    @Override
    public boolean cancel() {
        return doCancel();
    }
}
