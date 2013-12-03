package org.opendaylight.controller.frm.flow

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService

class FlowCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
       
    @Property
    val SalFlowService salFlowService;
    
    new(SalFlowService manager) {
        _salFlowService = manager;
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new FlowTransaction(modification,salFlowService);
    }
    
}