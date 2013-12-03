package org.opendaylight.controller.frm.flow

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory

class FlowProvider implements AutoCloseable {
    
    @Property
    DataProviderService dataService;
    
    @Property
    SalFlowService salFlowService;

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;
    val FlowCommitHandler commitHandler = new FlowCommitHandler(this);
    
    static val LOG = LoggerFactory.getLogger(FlowProvider);
    
    def void start() {
        val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Nodes)
            .child(Node)
            .augmentation(FlowCapableNode)
            .child(Table)
            .child(Flow)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("Flow Config Provider started.");
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }
    
    override close() throws Exception {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
