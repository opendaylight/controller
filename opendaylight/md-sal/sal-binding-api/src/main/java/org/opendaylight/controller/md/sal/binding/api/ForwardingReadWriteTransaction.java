/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Utility {@link ReadWriteTransaction} implementation which forwards all interface method
 * invocation to a delegate instance.
 */
@SuppressWarnings("deprecation") // due to CheckedFuture & TransactionStatus
public class ForwardingReadWriteTransaction extends ForwardingObject implements ReadWriteTransaction {

    private final ReadWriteTransaction delegate;

    protected ForwardingReadWriteTransaction(ReadWriteTransaction delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    protected ReadWriteTransaction delegate() {
        return delegate;
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        delegate.put(store, path, data);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        delegate.put(store, path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path) {
        return delegate.read(store, path);
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public boolean cancel() {
        return delegate.cancel();
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        delegate.merge(store, path, data);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        delegate.merge(store, path, data, createMissingParents);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return delegate.submit();
    }

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        delegate.delete(store, path);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return delegate.commit();
    }
}
