/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.concurrent.ExecutionException;

public class ReadWriteTx implements DOMDataReadWriteTransaction {

    private final DOMDataReadTransaction delegateReadTx;
    private final DOMDataWriteTransaction delegateWriteTx;

    public ReadWriteTx(final DOMDataReadTransaction delegateReadTx, final DOMDataWriteTransaction delegateWriteTx) {
        this.delegateReadTx = delegateReadTx;
        this.delegateWriteTx = delegateWriteTx;
    }

    @Override
    public boolean cancel() {
        return delegateWriteTx.cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegateWriteTx.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegateWriteTx.merge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateWriteTx.delete(store, path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return delegateWriteTx.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return delegateWriteTx.commit();
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return delegateReadTx.read(store, path);
    }

    @Override public CheckedFuture<Boolean, ReadFailedException> exists(
        final LogicalDatastoreType store,
        final YangInstanceIdentifier path) {
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
            data = read(store, path);

        try {
            return Futures.immediateCheckedFuture(data.get().isPresent());
        } catch (InterruptedException | ExecutionException e) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Exists failed",e));
        }
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
