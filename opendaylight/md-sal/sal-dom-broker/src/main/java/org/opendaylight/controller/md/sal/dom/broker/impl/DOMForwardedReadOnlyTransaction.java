package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

class DOMForwardedReadOnlyTransaction extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, DOMStoreReadTransaction> implements
        DOMDataReadTransaction {

    protected DOMForwardedReadOnlyTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, DOMStoreReadTransaction> backingTxs) {
        super(identifier, backingTxs);
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final InstanceIdentifier path) {
        return getSubtransaction(store).read(path);
    }

}