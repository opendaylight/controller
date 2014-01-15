package org.opendaylight.controller.frm.port

import org.opendaylight.controller.frm.AbstractTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.SalPortService
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.UpdatePortInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.Port
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PortTransaction extends AbstractTransaction {
    
    @Property
    SalPortService salPortService;
    
    @Property
    DataProviderService dataService;
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,SalPortService salPortService, DataProviderService dataService) {
        super(modification)
        _salPortService = salPortService;
        _dataService = dataService;
    }
    
    override remove(InstanceIdentifier<?> instanceId, DataObject obj) {
        // NOOP
    }
    
    override update(InstanceIdentifier<?> instanceId, DataObject originalObj, DataObject updatedObj) {
        add(instanceId,updatedObj);
    }
    
    override add(InstanceIdentifier<?> instanceId, DataObject obj) {
        if(obj instanceof Port) {
           	val port = (obj as Port)
           	val nodeInstanceId = instanceId.firstIdentifierOf(Node);
           	val builder = new UpdatePortInputBuilder(port);
           	builder.setNode(new NodeRef(nodeInstanceId));
           	_salPortService.updatePort(builder.build());
        }
    }
    
    override validate() throws IllegalStateException {
        PortTransactionValidator.validate(this)
    }  
}