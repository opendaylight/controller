/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
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
 * We never leak this class to the user and have it implement the {@link FutureCallback}
 * interface so we have a simple way of propagating the result.
 */
final class PingPongTransaction implements FutureCallback<Void> {
    private final CheckedFuture<Void, TransactionCommitFailedException> submitFuture;
    private final ListenableFuture<RpcResult<TransactionStatus>> commitFuture;
    private final DOMDataReadWriteTransaction delegate;
    private final SettableFuture<Void> future;
    private DOMDataReadWriteTransaction frontendTransaction;

    PingPongTransaction(final DOMDataReadWriteTransaction delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
        future = SettableFuture.create();
        submitFuture = new PingPongFuture(future);
        commitFuture = AbstractDataTransaction.convertToLegacyCommitFuture(submitFuture);
    }

    DOMDataReadWriteTransaction getTransaction() {
        return delegate;
    }

    DOMDataReadWriteTransaction getFrontendTransaction() {
        return frontendTransaction;
    }

    CheckedFuture<Void, TransactionCommitFailedException> getSubmitFuture() {
        return submitFuture;
    }

    ListenableFuture<RpcResult<TransactionStatus>> getCommitFuture() {
        return commitFuture;
    }

    @Override
    public void onSuccess(final Void result) {
        future.set(result);
    }

    @Override
    public void onFailure(final Throwable t) {
        future.setException(t);
    }

    void recordFrontendTransaction(final DOMDataReadWriteTransaction tx) {
        if (frontendTransaction != null) {
            frontendTransaction = tx;
        }
    }

    @Override
    public String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("delegate", delegate);
    }
}