/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Abstract Base Transaction for transactions which are backed by
 * {@link DOMDataWriteTransaction}
 */
public class AbstractWriteTransaction<T extends DOMDataWriteTransaction> extends
        AbstractForwardedTransaction<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWriteTransaction.class);

    protected AbstractWriteTransaction(final T delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    protected final void doPut(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        getDelegate().put(store, normalized.getKey(), normalized.getValue());
    }

    protected final void doMerge(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        getDelegate().merge(store, normalized.getKey(), normalized.getValue());
    }

    protected final void doDelete(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = getCodec().toNormalized(path);
        getDelegate().delete(store, normalized);
    }

    protected final ListenableFuture<RpcResult<TransactionStatus>> doCommit() {
        return getDelegate().commit();
    }

    protected final boolean doCancel() {
        return getDelegate().cancel();
    }

}
