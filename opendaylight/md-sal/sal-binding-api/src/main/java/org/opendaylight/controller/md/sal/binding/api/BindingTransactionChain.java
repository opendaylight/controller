package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface BindingTransactionChain extends TransactionChain<InstanceIdentifier<?>, DataObject> {

    @Override
    ReadOnlyTransaction newReadOnlyTransaction();

    @Override
    ReadWriteTransaction newReadWriteTransaction();

    @Override
    WriteTransaction newWriteOnlyTransaction();

}
