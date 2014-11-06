/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

class BindingDataWriteTransactionImpl<T extends DOMDataWriteTransaction> extends
        AbstractWriteTransaction<T> implements WriteTransaction {

    protected BindingDataWriteTransactionImpl(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        super(delegateTx, codec);
    }

    @Override
    public <U extends DataObject> void put(final LogicalDatastoreType store, final InstanceIdentifier<U> path,
                                           final U data) {
        put(store, path, data,false);
    }

    @Override
    public <T extends DataObject> void merge(final LogicalDatastoreType store, final InstanceIdentifier<T> path,
                                             final T data) {
        merge(store, path, data,false);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        doDelete( store, path);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
    }

    @Override
    public CheckedFuture<Void,TransactionCommitFailedException> submit() {
        return doSubmit();
    }

    @Override
    public boolean cancel() {
        return doCancel();
    }
}