package org.opendaylight.controller.md.frm.compatibility

import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.FlowsBuilder
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager
import static com.google.common.base.Preconditions.*;
import static extension org.opendaylight.controller.md.frm.compatibility.FlowConfigMapping.*;
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*;
import org.opendaylight.controller.sal.common.util.Arguments
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig
import java.util.HashSet
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.yangtools.yang.common.RpcError

class FRMRuntimeDataProvider implements RuntimeDataProvider, DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    static val FLOWS_PATH = InstanceIdentifier.builder().node(Flows).toInstance;

    @Property
    var DataProviderService dataService;

    @Property
    var DataChangeListener changeListener;
    
    @Property
    var IForwardingRulesManager manager;

    FlowManagementReader configuration = new ConfigurationReader();

    def init() {
        //dataService.registerDataChangeListener(FLOWS_PATH, changeListener);
        dataService.registerCommitHandler(FLOWS_PATH, this);
    }

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return readFrom(configuration, path);
    }

    override DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return readFrom(configuration, path);
    }

    def DataObject readFrom(FlowManagementReader store, InstanceIdentifier<? extends DataObject> path) {
        if (FLOWS_PATH == path) {
            return store.readAllFlows();
        }
        if (FLOWS_PATH.contains(path)) {
            return store.readFlow(path.toFlowKey());
        }
        return null;
    }

    override FlowCommitTransaction requestCommit(
        DataModification modification) {
        return new FlowCommitTransaction(this,modification);
    }

    def toFlowKey(InstanceIdentifier<? extends DataObject> identifier) {
        checkNotNull(identifier)
        val item = Arguments.checkInstanceOf(identifier.path.get(1),IdentifiableItem);
        val key = Arguments.checkInstanceOf(item.key,FlowKey)
        return key;
    }
    
    def RpcResult<Void> finish(FlowCommitTransaction transaction) {
        for(flw: transaction.toRemove) {
            manager.removeStaticFlow(flw.name,flw.node)
        }
        
        for(flw: transaction.toUpdate) {
            manager.removeStaticFlow(flw.name,flw.node);
            manager.addStaticFlow(flw);
        }
        
        return Rpcs.<Void>getRpcResult(true,null,Collections.<RpcError>emptySet())
    }
    
    def RpcResult<Void> rollback(FlowCommitTransaction transaction) {
        // NOOP: We did not changed any state.
    }

}

class ConfigurationReader implements FlowManagementReader {

    @Property
    var IForwardingRulesManager manager;

    override Flows readAllFlows() {
        val it = new FlowsBuilder();
        flow = manager.staticFlows.map[
            toConfigurationFlow();
        ]
        return it.build();
    }

    override readFlow(FlowKey key) {
        val flowCfg = manager.getStaticFlow(String.valueOf(key.id), key.node.toADNode());
        return flowCfg.toConfigurationFlow;
    }
}

public static class FlowCommitTransaction implements DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

    @Property
    val DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

    @Property
    val FRMRuntimeDataProvider flowManager;
    
    @Property
    val toAdd = new HashSet<FlowConfig>();
    
    @Property
    var Iterable<FlowConfig> toUpdate
    
    @Property
    var Iterable<FlowConfig> toRemove
    

    new(FRMRuntimeDataProvider flowManager,DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        super();
        _flowManager = flowManager;
        _modification = modification;
        processModification();
    }

    override finish() throws IllegalStateException {
        return flowManager.finish(this);
    }

    override rollback() throws IllegalStateException
{
        return flowManager.rollback(this);
    }

    def processModification() {
        val updated = modification.updatedConfigurationData.entrySet;
        
        val _toUpdate = updated.filter[key.isFlowPath].map[
             return (value as Flow).toFlowConfig
        ]
        toUpdate = _toUpdate as Iterable<FlowConfig>
        
        
        val _toRemove = modification.removedConfigurationData.filter[isFlowPath].map[
             toFlowConfig
        ]
        toRemove = _toRemove as Iterable<FlowConfig>
        
    }
}
