package org.opendaylight.controller.frm

import java.util.Collections
import java.util.HashSet
import java.util.Map.Entry
import java.util.Set
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.common.util.Rpcs
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.RpcError

class FlowNodeConfigTransaction implements DataCommitTransaction<InstanceIdentifier<?extends DataObject>, DataObject> {
    
    @Property
    val FlowNodeConfigProvider manager;
    
    @Property
    val DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,FlowNodeConfigProvider manager) {
        _modification = modification;
        _manager = manager;
    }
    
    override finish() throws IllegalStateException {
        callRpcs();
        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
    }
    
    override getModification() {
        return _modification;
    }
    
    override rollback() throws IllegalStateException {
        rollbackRpcs();
        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
    }
    
    def private callRpcs() {
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = _modification.getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and modified entries
         * The reason I created a new HashSet is because the collections we are returned are immutable.
         */
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
        updatedEntries.addAll(_modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        val Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = _modification.getRemovedConfigurationData();
        for (Entry<InstanceIdentifier<? extends DataObject >, DataObject> entry : createdEntries) {
            if(entry.value instanceof Flow) {
                addFlow(entry.key,(entry.value as Flow));
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if(entry.value instanceof Flow) {
                val originalFlow = (_modification.originalConfigurationData.get(entry.key) as Flow);
                val updatedFlow = (entry.value as Flow)
                updateFlow(entry.key, originalFlow ,updatedFlow);
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
            val removeValue = _modification.getOriginalConfigurationData.get(instanceId);
            if(removeValue instanceof Flow) {
                removeFlow(instanceId,(removeValue as Flow));

            }
        }
    }
    
    def private rollbackRpcs() {
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = _modification.getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and modified entries
         * The reason I created a new HashSet is because the collections we are returned are immutable.
         */
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
        updatedEntries.addAll(_modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        val Set<InstanceIdentifier<? >> removeEntriesInstanceIdentifiers = _modification.getRemovedConfigurationData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
            if(entry.value instanceof Flow) {
                removeFlow(entry.key,(entry.value as Flow)); // because we are rolling back, remove what we would have added.
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if(entry.value instanceof Flow) {
                val originalFlow = (_modification.originalConfigurationData.get(entry.key) as Flow);
                val updatedFlow = (entry.value as Flow)
                updateFlow(entry.key, updatedFlow ,originalFlow);// because we are rolling back, replace the updated with the original
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
            val removeValue = _modification.getOriginalConfigurationData.get(instanceId);
            if(removeValue instanceof Flow) {
                addFlow(instanceId,(removeValue as Flow));// because we are rolling back, add what we would have removed.

            }
        }
    }
    
    private def removeFlow(InstanceIdentifier<?> instanceId, Flow flow) {
        val tableInstanceId = instanceId.firstIdentifierOf(Table);
        val nodeInstanceId = instanceId.firstIdentifierOf(Node);
        val builder = new RemoveFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        _manager.salFlowService.removeFlow(builder.build());
    }
    
    private def updateFlow(InstanceIdentifier<?> instanceId, Flow originalFlow, Flow updatedFlow) {
        val nodeInstanceId = instanceId.firstIdentifierOf(Node);
        val builder = new UpdateFlowInputBuilder();
        builder.setNode(new NodeRef(nodeInstanceId));
        val ufb = new UpdatedFlowBuilder(updatedFlow);
        builder.setUpdatedFlow((ufb.build()));
        val ofb = new OriginalFlowBuilder(originalFlow);
        builder.setOriginalFlow(ofb.build());      
        _manager.salFlowService.updateFlow(builder.build());
    }
    
    private def addFlow(InstanceIdentifier<?> instanceId, Flow flow) {
        val tableInstanceId = instanceId.firstIdentifierOf(Table);
        val nodeInstanceId = instanceId.firstIdentifierOf(Node);
        val builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        _manager.salFlowService.addFlow(builder.build());
    } 
    
    
}