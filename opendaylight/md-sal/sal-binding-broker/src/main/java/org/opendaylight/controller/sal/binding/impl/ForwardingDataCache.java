package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ForwardingDataCache {
    DataObject read(InstanceIdentifier<DataObject> id);
    void write(InstanceIdentifier<DataObject> id, DataObject data);
}
