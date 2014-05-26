package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMTransactionChain extends TransactionChain<InstanceIdentifier, NormalizedNode<?, ?>> {

    @Override
    DOMDataReadTransaction newReadOnlyTransaction();

    @Override
    DOMDataReadWriteTransaction newReadWriteTransaction();

    @Override
    DOMDataWriteTransaction newWriteOnlyTransaction();

}
