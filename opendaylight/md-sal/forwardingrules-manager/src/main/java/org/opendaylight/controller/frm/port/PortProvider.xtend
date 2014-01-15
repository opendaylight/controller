package org.opendaylight.controller.frm.port

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.Port
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.UpdateNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.SalPortService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.Port
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory

class PortProvider implements AutoCloseable {
    
    @Property
    DataProviderService dataService;
    
    @Property
    SalPortService salPortService;
    
    PortCommitHandler commitHandler

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;
    
    static val LOG = LoggerFactory.getLogger(PortProvider);
    
    def void start() {
        commitHandler = new PortCommitHandler(salPortService,dataService)
       val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Nodes)
            .child(Node)
            .child(NodeConnector)
            .augmentation(UpdateNodeConnector)
            .child(Port)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("Port Config Provider started.");
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }
    
    override close() throws Exception {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
