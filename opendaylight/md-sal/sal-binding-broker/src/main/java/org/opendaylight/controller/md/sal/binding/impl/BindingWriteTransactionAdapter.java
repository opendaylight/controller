/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class BindingWriteTransactionAdapter<T extends org.opendaylight.mdsal.binding.api.WriteTransaction>
        extends AbstractBindingTransactionAdapter<T> implements WriteTransaction {
    private static final ExceptionMapper<TransactionCommitFailedException> COMMIT_EX_MAPPER =
            new ExceptionMapper<TransactionCommitFailedException>("commit", TransactionCommitFailedException.class) {
        @Override
        protected TransactionCommitFailedException newWithCause(String message, Throwable cause) {
            if (cause instanceof org.opendaylight.mdsal.common.api.OptimisticLockFailedException) {
                return new OptimisticLockFailedException(cause.getMessage(), cause.getCause());
            } else if (cause instanceof org.opendaylight.mdsal.common.api.TransactionCommitFailedException) {
                Throwable rootCause = cause.getCause();
                if (rootCause instanceof org.opendaylight.mdsal.common.api.DataStoreUnavailableException) {
                    rootCause = new DataStoreUnavailableException(rootCause.getMessage(), rootCause.getCause());
                }

                return new TransactionCommitFailedException(cause.getMessage(), rootCause);
            }

            return new TransactionCommitFailedException(message, cause);
        }
    };

    BindingWriteTransactionAdapter(T delegate) {
        super(delegate);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        getDelegate().put(convert(store), path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        getDelegate().put(convert(store), path, data);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        getDelegate().merge(convert(store), path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        getDelegate().merge(convert(store), path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        getDelegate().delete(convert(store), path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        final SettableFuture<CommitInfo> resultFuture = SettableFuture.create();
        getDelegate().commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(CommitInfo result) {
                resultFuture.set(result);
            }

            @Override
            public void onFailure(Throwable ex) {
                if (ex instanceof Exception) {
                    resultFuture.setException(COMMIT_EX_MAPPER.apply((Exception)ex));
                } else {
                    resultFuture.setException(ex);
                }
            }
        }, MoreExecutors.directExecutor());

        return resultFuture;
    }

    @Override
    public boolean cancel() {
        return getDelegate().cancel();
    }
}
