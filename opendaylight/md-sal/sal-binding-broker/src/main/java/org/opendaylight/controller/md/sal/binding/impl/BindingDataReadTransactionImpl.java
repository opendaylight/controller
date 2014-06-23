package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

class BindingDataReadTransactionImpl extends AbstractForwardedTransaction<DOMDataReadOnlyTransaction> implements
        ReadOnlyTransaction {

    protected BindingDataReadTransactionImpl(final DOMDataReadOnlyTransaction delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    @Override
    public ListenableFuture<Optional<DataObject>> read(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        return doRead(getDelegate(),store, path);
    }

    @Override
    public void close() {
        getDelegate().close();
    }

}