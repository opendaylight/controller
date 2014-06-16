package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMStoreProxy implements DOMStore {

    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(InstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
        throw new UnsupportedOperationException("registerChangeListener");
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        throw new UnsupportedOperationException("createTransactionChain");
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        throw new UnsupportedOperationException("newReadOnlyTransaction");
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        throw new UnsupportedOperationException("newWriteOnlyTransaction");
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        throw new UnsupportedOperationException("newReadWriteTransaction");
    }
}
