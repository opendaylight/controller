/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;

/**
 * Transaction context. Tracks the relationship with the backend transaction.
 * We never leak this class to the user and have it implement the {@link FutureCallback}
 * interface so we have a simple way of propagating the result.
 */
final class PingPongTransaction implements FutureCallback<CommitInfo> {
    private final DOMDataReadWriteTransaction delegate;
    private final SettableFuture<CommitInfo> future;
    private DOMDataReadWriteTransaction frontendTransaction;

    PingPongTransaction(final DOMDataReadWriteTransaction delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
        future = SettableFuture.create();
    }

    DOMDataReadWriteTransaction getTransaction() {
        return delegate;
    }

    DOMDataReadWriteTransaction getFrontendTransaction() {
        return frontendTransaction;
    }

    ListenableFuture<CommitInfo> getCommitFuture() {
        return future;
    }

    @Override
    public void onSuccess(final CommitInfo result) {
        future.set(result);
    }

    @Override
    public void onFailure(final Throwable throwable) {
        future.setException(throwable);
    }

    void recordFrontendTransaction(final DOMDataReadWriteTransaction tx) {
        if (frontendTransaction != null) {
            frontendTransaction = tx;
        }
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("delegate", delegate);
    }
}
