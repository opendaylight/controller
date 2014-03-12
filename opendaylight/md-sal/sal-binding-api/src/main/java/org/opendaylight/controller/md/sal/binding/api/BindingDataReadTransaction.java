package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface BindingDataReadTransaction extends AsyncReadTransaction<InstanceIdentifier<?>, DataObject> {


    @Override
    public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path);


}
