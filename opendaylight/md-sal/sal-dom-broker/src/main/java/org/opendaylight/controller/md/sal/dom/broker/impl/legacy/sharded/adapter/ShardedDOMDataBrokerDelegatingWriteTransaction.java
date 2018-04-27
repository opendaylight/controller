/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.broker.impl.TransactionCommitFailedExceptionMapper;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class ShardedDOMDataBrokerDelegatingWriteTransaction implements DOMDataWriteTransaction {
    private final DOMDataTreeWriteTransaction delegateTx;
    private final Object txIdentifier;

    ShardedDOMDataBrokerDelegatingWriteTransaction(final Object txIdentifier,
                                                          final DOMDataTreeWriteTransaction delegateTx) {
        this.delegateTx = checkNotNull(delegateTx);
        this.txIdentifier = checkNotNull(txIdentifier);
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        delegateTx.put(LegacyShardedDOMDataBrokerAdapterUtils.translateDataStoreType(store), path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        delegateTx.merge(LegacyShardedDOMDataBrokerAdapterUtils.translateDataStoreType(store), path, data);
    }

    @Override
    public boolean cancel() {
        return delegateTx.cancel();
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateTx.delete(LegacyShardedDOMDataBrokerAdapterUtils.translateDataStoreType(store), path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return Futures.makeChecked(delegateTx.submit(), TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return delegateTx.commit();
    }

    @Override
    public Object getIdentifier() {
        return txIdentifier;
    }
}
