package org.opendaylight.controller.frm.flow

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class FlowCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
       
    @Property
    val FlowProvider manager;
    
    new(FlowProvider manager) {
        _manager = manager;
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new FlowTransaction(modification,manager);
    }
    
}