/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.mdsaltrace.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.ArrayList;
import java.util.List;

class TracingWriteTransaction implements WriteTransaction{

    private final WriteTransaction delegate;
    private final TracingBroker tracingBroker;
    private List<String> logs = new ArrayList<>();

    public TracingWriteTransaction(WriteTransaction delegate, TracingBroker tracingBroker) {
        this.delegate = delegate;
        this.tracingBroker = tracingBroker;
    }

    private <T extends DataObject> void recordOp(LogicalDatastoreType store, InstanceIdentifier iid,
                                                                                            String method, T t) {
        if (!tracingBroker.isWriteWatched(iid, store)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Method ").append(method);
        sb.append(" to ").append(store);
        sb.append(" at ").append(tracingBroker.toPathString(iid)).append('.');
        if (t != null) {
            sb.append(" Data: ").append(t);
        }
        sb.append(" Stack:").append(tracingBroker.getStackSummary(2));

        logs.add(sb.toString());
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> instanceIdentifier, T t) {
        recordOp(logicalDatastoreType, instanceIdentifier, "put", t);
        delegate.put(logicalDatastoreType, instanceIdentifier, t);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> instanceIdentifier, T t, boolean b) {
        recordOp(logicalDatastoreType, instanceIdentifier, "put", t);
        delegate.put(logicalDatastoreType, instanceIdentifier, t, b);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> instanceIdentifier, T t) {
        recordOp(logicalDatastoreType, instanceIdentifier, "merge", t);
        delegate.merge(logicalDatastoreType, instanceIdentifier, t);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> instanceIdentifier, T t, boolean b) {
        recordOp(logicalDatastoreType, instanceIdentifier, "merge", t);
        delegate.merge(logicalDatastoreType, instanceIdentifier, t, b);
    }

    @Override
    public boolean cancel() {
        logs.clear();
        return delegate.cancel();
    }

    @Override
    public void delete(LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<?> instanceIdentifier) {
        recordOp(logicalDatastoreType, instanceIdentifier, "delete", null);
        delegate.delete(logicalDatastoreType, instanceIdentifier);
    }

    private void logOps() {
        for (String log : logs) {
            tracingBroker.LOG.warn(log);
        }
    }
    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        logOps();
        return delegate.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        logOps();
        return delegate.commit();
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }
}
