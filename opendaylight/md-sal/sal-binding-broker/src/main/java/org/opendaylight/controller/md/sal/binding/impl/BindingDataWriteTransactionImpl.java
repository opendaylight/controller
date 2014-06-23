package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.ListenableFuture;

class BindingDataWriteTransactionImpl<T extends DOMDataWriteTransaction> extends
        AbstractWriteTransaction<T> implements WriteTransaction {

    protected BindingDataWriteTransactionImpl(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        super(delegateTx, codec);
    }



    @Override
    public void put(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        doPut(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
        doMerge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        doDelete( store, path);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return doCommit();
    }

    @Override
    public boolean cancel() {
        return doCancel();
    }
}