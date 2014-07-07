/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

class BindingDataReadWriteTransactionImpl extends
        AbstractReadWriteTransaction implements ReadWriteTransaction {

    protected BindingDataReadWriteTransactionImpl(final DOMDataReadWriteTransaction delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        doDelete(store, path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
            final DataObject data) {
        doMerge(store, path, data);
    }

    @Override
    public void put(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
            final DataObject data) {
        doPut(store, path, data);
    }

    @Override
    public ListenableFuture<Optional<DataObject>> read(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        return doRead(getDelegate(), store, path);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T extends DataObject> ListenableFuture<Optional<T>> readChecked(
            final LogicalDatastoreType store, final InstanceIdentifier<T> path) {
        return (ListenableFuture) read(store, path);
    }

    @Override
    public void putAndEnsureParents(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        doPutWithEnsureParents(store, path, data);
    }

    @Override
    public void mergeAndEnsureParents(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        doMergeWithEnsureParents(store, path, data);
    }

    @Override
    public boolean cancel() {
        return doCancel();
        }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return doCommit();
    }
}