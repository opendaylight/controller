/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.Instant;
import java.util.Objects;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedTrait;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class TracingReadWriteTransaction
    extends AbstractTracingWriteTransaction
        implements DOMDataReadWriteTransaction, CloseTracked<TracingReadWriteTransaction> {

    private final CloseTrackedTrait<TracingReadWriteTransaction> closeTracker;
    private final DOMDataReadWriteTransaction delegate;

    TracingReadWriteTransaction(DOMDataReadWriteTransaction delegate, TracingBroker tracingBroker,
            CloseTrackedRegistry<TracingReadWriteTransaction> readWriteTransactionsRegistry) {
        super(delegate, tracingBroker);
        this.closeTracker = new CloseTrackedTrait<>(readWriteTransactionsRegistry);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                                                            LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        return delegate.read(store, yiid);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        return delegate.exists(store, yiid);
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
    public Instant getObjectCreated() {
        return closeTracker.getObjectCreated();
    }

    @Override
    public Throwable getAllocationContext() {
        return closeTracker.getAllocationContext();
    }
}
