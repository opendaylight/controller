/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractTracingWriteTransaction implements DOMDataWriteTransaction {

    private final DOMDataWriteTransaction delegate;
    private final TracingBroker tracingBroker;
    private final List<String> logs = new ArrayList<>();

    AbstractTracingWriteTransaction(DOMDataWriteTransaction delegate, TracingBroker tracingBroker) {
        this.delegate = Objects.requireNonNull(delegate);
        this.tracingBroker = Objects.requireNonNull(tracingBroker);
        recordOp(null, null, "instantiate", null);
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
            sb.append("Method \"").append(method).append('"');
            if (store != null) {
                sb.append(" to ").append(store);
            }
            if (yiid != null) {
                sb.append(" at ").append(tracingBroker.toPathString(yiid));
            }
            sb.append('.');
            if (yiid != null) {
                // If we don’t have an id, we don’t expect data either
                sb.append(" Data: ");
                if (node != null) {
                    sb.append(node.getValue());
                } else {
                    sb.append("null");
                }
            }
            sb.append(" Stack:").append(tracingBroker.getStackSummary());
            synchronized (this) {
                logs.add(sb.toString());
            }
        }
    }

    private synchronized void logOps() {
        synchronized (this) {
            if (TracingBroker.LOG.isWarnEnabled()) {
                TracingBroker.LOG.warn("Transaction {} contains the following operations:", getIdentifier());
                logs.forEach(TracingBroker.LOG::warn);
            }
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
        return delegate.cancel();
    }

    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        recordOp(store, yiid, "delete", null);
        delegate.delete(store, yiid);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        recordOp(null, null, "commit", null);
        logOps();
        return delegate.commit();
    }

    @Override
    @Nonnull
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    // https://jira.opendaylight.org/browse/CONTROLLER-1792

    @Override
    public final boolean equals(Object object) {
        return object == this || delegate.equals(object);
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public final String toString() {
        return getClass().getName() + "; delegate=" + delegate;
    }
}
