package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface BindingDataWriteTransaction extends AsyncWriteTransaction<InstanceIdentifier<?>, DataObject> {

    @Override
    public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data);

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path);

}
