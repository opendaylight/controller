/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedTrait;
import org.opendaylight.yangtools.yang.common.RpcResult;

class TracingWriteTransaction extends AbstractTracingWriteTransaction
        implements CloseTracked<TracingWriteTransaction> {

    private final CloseTrackedTrait<TracingWriteTransaction> closeTracker;

    TracingWriteTransaction(DOMDataWriteTransaction delegate, TracingBroker tracingBroker,
            CloseTrackedRegistry<TracingWriteTransaction> writeTransactionsRegistry) {
        super(delegate, tracingBroker);
        this.closeTracker = new CloseTrackedTrait<>(writeTransactionsRegistry);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        closeTracker.removeFromTrackedRegistry();
        return super.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        closeTracker.removeFromTrackedRegistry();
        return super.commit();
    }

    @Override
    public boolean cancel() {
        closeTracker.removeFromTrackedRegistry();
        return super.cancel();
    }

    @Override
    public StackTraceElement[] getAllocationContextStackTrace() {
        return closeTracker.getAllocationContextStackTrace();
    }

}
