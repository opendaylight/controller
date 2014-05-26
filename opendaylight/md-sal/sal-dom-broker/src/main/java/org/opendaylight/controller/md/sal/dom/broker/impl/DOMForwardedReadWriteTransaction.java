package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

class DOMForwardedReadWriteTransaction extends DOMForwardedWriteTransaction<DOMStoreReadWriteTransaction>
        implements DOMDataReadWriteTransaction {

    protected DOMForwardedReadWriteTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, DOMStoreReadWriteTransaction> backingTxs,
            final DOMDataCommitImplementation commitImpl) {
        super(identifier, backingTxs, commitImpl);
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final InstanceIdentifier path) {
        return getSubtransaction(store).read(path);
    }
}