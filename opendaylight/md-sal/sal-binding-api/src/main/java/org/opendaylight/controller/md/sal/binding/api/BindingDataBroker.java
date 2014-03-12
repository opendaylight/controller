package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface BindingDataBroker extends AsyncDataBroker<InstanceIdentifier<?>, DataObject, BindingDataChangeListener>{

    @Override
    public BindingDataReadTransaction newReadOnlyTransaction();

    @Override
    public BindingDataReadWriteTransaction newReadWriteTransaction();

    @Override
    public BindingDataWriteTransaction newWriteOnlyTransaction();

    @Override
    public ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
            InstanceIdentifier<?> path, BindingDataChangeListener listener, DataChangeScope triggeringScope);
}
