package org.opendaylight.controller.frm.meter

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory

class MeterProvider implements AutoCloseable {
    
    @Property
    DataProviderService dataService;
    
    @Property
    SalMeterService salMeterService;
    
    FlowCommitHandler commitHandler

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;
    
    static val LOG = LoggerFactory.getLogger(MeterProvider);
    
    def void start() {
        commitHandler = new FlowCommitHandler(salMeterService)
        val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Nodes)
            .child(Node)
            .augmentation(FlowCapableNode)
            .child(Meter)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("Meter Config Provider started.");
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }
    
    override close() throws Exception {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
