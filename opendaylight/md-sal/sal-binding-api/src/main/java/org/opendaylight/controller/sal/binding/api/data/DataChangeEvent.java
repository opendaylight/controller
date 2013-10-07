package org.opendaylight.controller.sal.binding.api.data;

import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface DataChangeEvent {

    Map<InstanceIdentifier, DataObject> getCreated();

    Map<InstanceIdentifier, DataObject> getUpdated();

    Set<InstanceIdentifier> getRemoved();
}
