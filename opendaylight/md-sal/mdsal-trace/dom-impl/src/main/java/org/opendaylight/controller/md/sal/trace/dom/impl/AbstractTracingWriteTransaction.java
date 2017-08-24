/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractTracingWriteTransaction implements DOMDataWriteTransaction {

    private final DOMDataWriteTransaction delegate;
    private final TracingBroker tracingBroker;
    private final List<String> logs = new ArrayList<>();

    AbstractTracingWriteTransaction(DOMDataWriteTransaction delegate, TracingBroker tracingBroker) {
        this.delegate = Objects.requireNonNull(delegate);
        this.tracingBroker = Objects.requireNonNull(tracingBroker);
    }

    private void recordOp(LogicalDatastoreType store, YangInstanceIdentifier yiid, String method,
            NormalizedNode<?, ?> node) {
        if (!tracingBroker.isWriteWatched(yiid, store)) {
            return;
        }

        final Object value = node != null ? node.getValue() : null;

        if (value != null && value instanceof ImmutableSet && ((Set<?>)value).isEmpty()) {
            if (TracingBroker.LOG.isDebugEnabled()) {
                TracingBroker.LOG.debug("Empty data set write to {}", tracingBroker.toPathString(yiid));
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Method \"").append(method);
            sb.append("\" to ").append(store);
            sb.append(" at ").append(tracingBroker.toPathString(yiid)).append('.');
            sb.append(" Data: ");
            if (node != null) {
                sb.append(node.getValue());
            } else {
                sb.append("null");
            }
            sb.append(" Stack:").append(tracingBroker.getStackSummary());
            synchronized (this) {
                logs.add(sb.toString());
            }
        }
    }

    private synchronized void logOps() {
        synchronized (this) {
            logs.forEach(log -> TracingBroker.LOG.warn(log));
            logs.clear();
        }
    }

    @Override
    public void put(LogicalDatastoreType store, YangInstanceIdentifier yiid, NormalizedNode<?, ?> node) {
        recordOp(store, yiid, "put", node);
        delegate.put(store, yiid, node);
    }

    @Override
    public void merge(LogicalDatastoreType store, YangInstanceIdentifier yiid, NormalizedNode<?, ?> node) {
        recordOp(store, yiid, "merge", node);
        delegate.merge(store, yiid, node);
    }

    @Override
    public boolean cancel() {
        synchronized (this) {
            logs.clear();
        }
        boolean result = delegate.cancel();
        return result;
    }

    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        recordOp(store, yiid, "delete", null);
        delegate.delete(store, yiid);
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
