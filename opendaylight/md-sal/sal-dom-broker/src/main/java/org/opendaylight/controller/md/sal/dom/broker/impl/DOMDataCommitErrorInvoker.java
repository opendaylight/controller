/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;

/**
 *
 * Utility implemetation of {@link FutureCallback} which is responsible
 * for invoking {@link DOMDataCommitErrorListener} on TransactionCommit failed.
 *
 * When {@link #onFailure(Throwable)} is invoked, supplied {@link DOMDataCommitErrorListener}
 * callback is invoked with associated transaction and throwable is invoked on listener.
 *
 */
class DOMDataCommitErrorInvoker implements FutureCallback<Void> {

    private final DOMDataWriteTransaction tx;
    private final DOMDataCommitErrorListener listener;


    /**
     *
     * Construct new DOMDataCommitErrorInvoker.
     *
     * @param transaction Transaction which should be passed as argument to {@link DOMDataCommitErrorListener#onCommitFailed(DOMDataWriteTransaction, Throwable)}
     * @param listener Listener which should be invoked on error.
     */
    public DOMDataCommitErrorInvoker(DOMDataWriteTransaction transaction, DOMDataCommitErrorListener listener) {
        this.tx = Preconditions.checkNotNull(transaction, "Transaction must not be null");
        this.listener = Preconditions.checkNotNull(listener, "Listener must not be null");
    }

    @Override
    public void onFailure(Throwable t) {
        listener.onCommitFailed(tx, t);
    }

    @Override
    public void onSuccess(Void result) {
        // NOOP
    }
}