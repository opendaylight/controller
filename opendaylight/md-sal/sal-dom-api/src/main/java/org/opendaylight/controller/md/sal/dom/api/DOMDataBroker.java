package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMDataBroker extends AsyncDataBroker<InstanceIdentifier, NormalizedNode<?, ?>, DOMDataChangeListener>{


    @Override
    public DOMDataReadTransaction newReadOnlyTransaction();

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction();

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction();

}
