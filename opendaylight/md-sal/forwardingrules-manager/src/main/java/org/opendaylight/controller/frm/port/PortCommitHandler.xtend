package org.opendaylight.controller.frm.port

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.SalPortService
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PortCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
       
    @Property
    SalPortService salPortService;
    
    @Property
    DataProviderService dataService;
    
    new(SalPortService manager, DataProviderService dataService) {
        _salPortService = manager;
        _dataService = dataService
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new PortTransaction(modification,salPortService,dataService);
    }
    
}