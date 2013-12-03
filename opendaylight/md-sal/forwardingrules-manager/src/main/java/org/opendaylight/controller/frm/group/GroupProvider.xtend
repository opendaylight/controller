package org.opendaylight.controller.frm.group

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group

class GroupProvider implements AutoCloseable {
    
    @Property
    DataProviderService dataService;
    
    @Property
    SalGroupService groupService;
    
    GroupCommitHandler commitHandler

    Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> commitHandlerRegistration;
    
    static val LOG = LoggerFactory.getLogger(GroupProvider);
    
    def void start() {
        commitHandler = new GroupCommitHandler(groupService)
        val InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Nodes)
            .child(Node)
            .augmentation(FlowCapableNode)
            .child(Group)
            .toInstance();
        commitHandlerRegistration = dataService.registerCommitHandler(path,commitHandler);
        LOG.info("Group Config Provider started.");
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }
    
    override close() throws Exception {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
