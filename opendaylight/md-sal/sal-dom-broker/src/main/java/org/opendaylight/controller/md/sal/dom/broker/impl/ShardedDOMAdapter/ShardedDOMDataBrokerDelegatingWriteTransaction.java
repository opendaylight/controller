/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.ShardedDOMAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.broker.impl.TransactionCommitFailedExceptionMapper;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class ShardedDOMDataBrokerDelegatingWriteTransaction implements DOMDataWriteTransaction {
    private static final ListenableFuture<RpcResult<TransactionStatus>> SUCCESS_FUTURE =
            Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED).build());

    private final DOMDataTreeWriteTransaction delegateTx;
    private final Object txIdentifier;

    public ShardedDOMDataBrokerDelegatingWriteTransaction(final Object txIdentifier, final DOMDataTreeWriteTransaction delegateTx) {
        this.delegateTx = checkNotNull(delegateTx);
        this.txIdentifier = checkNotNull(txIdentifier);
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegateTx.put(MdsalDelegatingDataBrokerUtils.translateDataStoreType(store), path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegateTx.merge(MdsalDelegatingDataBrokerUtils.translateDataStoreType(store), path, data);
    }

    @Override
    public boolean cancel() {
        return delegateTx.cancel();
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateTx.delete(MdsalDelegatingDataBrokerUtils.translateDataStoreType(store), path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return Futures.makeChecked(delegateTx.submit(), TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return Futures.transform(submit(), (AsyncFunction<Void, RpcResult<TransactionStatus>>) input -> SUCCESS_FUTURE);
    }

    @Override
    public Object getIdentifier() {
        return txIdentifier;
    }
}
