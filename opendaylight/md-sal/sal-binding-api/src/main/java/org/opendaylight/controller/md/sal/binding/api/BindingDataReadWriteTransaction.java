package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface BindingDataReadWriteTransaction extends BindingDataReadTransaction, BindingDataWriteTransaction, AsyncReadWriteTransaction<InstanceIdentifier<?>, DataObject> {

}
