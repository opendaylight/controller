package org.opendaylight.controller.frm.port

import org.opendaylight.controller.frm.AbstractTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.common.port.Configuration
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.SalPortService
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.UpdatePortInputBuilder
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
        if(obj instanceof NodeConnector) {
            val nodeConnector = (obj as NodeConnector)
            val flowNodeConnector = nodeConnector.getAugmentation(FlowCapableNodeConnector)
            if(flowNodeConnector != null) {
                val configuration = flowNodeConnector.configuration
                if(configuration != null) {
                    val nodeInstanceId = instanceId.firstIdentifierOf(Node);
                    val nodeConnectorInstanceId = instanceId.firstIdentifierOf(NodeConnector);
                    val builder = new UpdatePortInputBuilder(configuration);
                    builder.setNode(new NodeRef(nodeInstanceId));
                    builder.setNodeConnectorRef(new NodeConnectorRef(nodeConnectorInstanceId));
                    builder.setHardwareAddress(readHardwareAddress(nodeConnectorInstanceId))
                    _salPortService.updatePort(builder.build());
                }
            }
        }
    }
    
    override validate() throws IllegalStateException {
        PortTransactionValidator.validate(this)
    }  
    
    def MacAddress readHardwareAddress(InstanceIdentifier<NodeConnector> instanceId) {
        val nodeConnector = (dataService.readOperationalData(instanceId) as NodeConnector);
        if( nodeConnector != null ) {
            val flowNodeConnector =  nodeConnector.getAugmentation(FlowCapableNodeConnector);
            if(flowNodeConnector != null) {
                return flowNodeConnector.hardwareAddress;
            }
        }
        throw new IllegalStateException("Attempted to write config to a non-existent port");
    }
}