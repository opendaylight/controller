package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMStore {

    DOMStoreReadTransaction newReadOnlyTransaction();

    DOMStoreWriteTransaction newWriteOnlyTransaction();

    DOMStoreReadWriteTransaction newReadWriteTransaction();

    <L extends DataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            InstanceIdentifier path, DataChangeScope scope);

}
