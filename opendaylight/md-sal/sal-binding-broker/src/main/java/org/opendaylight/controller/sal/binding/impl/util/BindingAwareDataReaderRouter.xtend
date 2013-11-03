package org.opendaylight.controller.sal.binding.impl.util

import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject

class BindingAwareDataReaderRouter extends AbstractDataReadRouter<InstanceIdentifier<? extends DataObject>, DataObject> {
    
    override protected merge(InstanceIdentifier<? extends DataObject> path, Iterable<DataObject> data) {
        return data.iterator.next;
    }
    
}