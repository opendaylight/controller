package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Map.Entry;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface BindingIndependentMappingService {

    CompositeNode toDataDom(DataObject data);

    Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> toDataDom(
            Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry);

    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(InstanceIdentifier<? extends DataObject> path);

    DataObject dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> path, CompositeNode result);

    InstanceIdentifier<?> fromDataDom(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry);

}
