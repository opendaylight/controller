package org.opendaylight.controller.frm.table

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.SalTableService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory

class TableProvider implements AutoCloseable {
    
    @Property
    DataProviderService dataService;
    
    @Property
    SalTableService salTableService;
    
    TableCommitHandler commitHandler

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;
    
    static val LOG = LoggerFactory.getLogger(TableProvider);
    
    def void start() {
        commitHandler = new TableCommitHandler(salTableService)
        val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Nodes)
            .child(Node)
            .augmentation(FlowCapableNode)
            .child(Table)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("Table Config Provider started.");
        LOG.info( "Inside Table provider NSF") ;
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }
    
    override close() throws Exception {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
