package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

class BindingDataReadWriteTransactionImpl extends
        BindingDataWriteTransactionImpl<DOMDataReadWriteTransaction> implements ReadWriteTransaction {

    protected BindingDataReadWriteTransactionImpl(final DOMDataReadWriteTransaction delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    @Override
    public ListenableFuture<Optional<DataObject>> read(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        return doRead(getDelegate(), store, path);
    }
}