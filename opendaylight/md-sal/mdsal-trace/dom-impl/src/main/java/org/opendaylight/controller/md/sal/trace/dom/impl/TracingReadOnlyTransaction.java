/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.AbstractCloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class TracingReadOnlyTransaction
        extends AbstractCloseTracked<TracingReadOnlyTransaction>
        implements DOMDataTreeReadTransaction {

    private final DOMDataTreeReadTransaction delegate;

    TracingReadOnlyTransaction(DOMDataTreeReadTransaction delegate,
            CloseTrackedRegistry<TracingReadOnlyTransaction> readOnlyTransactionsRegistry) {
        super(readOnlyTransactionsRegistry);
        this.delegate = delegate;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store,
            YangInstanceIdentifier path) {
        return delegate.read(store, path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        return delegate.exists(store, path);
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void close() {
        delegate.close();
        super.removeFromTrackedRegistry();
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
