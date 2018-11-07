/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMDataTreeWriteTransactionAdapter extends ForwardingObject
        implements DOMDataTreeWriteTransaction {
    private final DOMDataWriteTransaction delegate;

    public DOMDataTreeWriteTransactionAdapter(final DOMDataWriteTransaction delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public Object getIdentifier() {
        return delegate().getIdentifier();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        TransactionUtils.put(delegate(), store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        TransactionUtils.merge(delegate(), store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        TransactionUtils.delete(delegate(), store, path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return delegate().commit();
    }

    @Override
    public boolean cancel() {
        return delegate().cancel();
    }

    @Override
    protected DOMDataWriteTransaction delegate() {
        return delegate;
    }
}
