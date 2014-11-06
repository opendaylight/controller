/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Transaction context. Tracks the relationship with the backend transaction.
 */
final class PingPongTransaction  implements FutureCallback<Object> {
    private final CheckedFuture<Void, TransactionCommitFailedException> submitFuture;
    private final ListenableFuture<RpcResult<TransactionStatus>> commitFuture;
    private final DOMDataReadWriteTransaction delegate;
    private final SettableFuture<Void> future;

    PingPongTransaction(final DOMDataReadWriteTransaction delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
        future = SettableFuture.create();
        submitFuture = new PingPongFuture(future);
        commitFuture = AbstractDataTransaction.convertToLegacyCommitFuture(submitFuture);
    }

    DOMDataReadWriteTransaction getTransaction() {
        return delegate;
    }

    CheckedFuture<Void, TransactionCommitFailedException> getSubmitFuture() {
        return submitFuture;
    }

    ListenableFuture<RpcResult<TransactionStatus>> getCommitFuture() {
        return commitFuture;
    }

    @Override
    public void onSuccess(final Object result) {
        future.set(null);
    }

    @Override
    public void onFailure(final Throwable t) {
        future.setException(t);
    }
}