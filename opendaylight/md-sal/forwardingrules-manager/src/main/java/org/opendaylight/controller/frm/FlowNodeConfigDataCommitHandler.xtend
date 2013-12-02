package org.opendaylight.controller.frm

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class FlowNodeConfigDataCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
       
    @Property
    val FlowNodeConfigProvider manager;
    
    new(FlowNodeConfigProvider manager) {
        _manager = manager;
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new FlowNodeConfigTransaction(modification,manager);
    }
    
}