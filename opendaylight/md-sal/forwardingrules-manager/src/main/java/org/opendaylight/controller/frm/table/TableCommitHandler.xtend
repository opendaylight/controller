package org.opendaylight.controller.frm.table

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.SalTableService;
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TableCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {
       
    @Property
    val SalTableService tableService;
    
    new(SalTableService tableService) {
        _tableService = tableService;
    }
    
    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new TableTransaction(modification,tableService);
    }
    
}