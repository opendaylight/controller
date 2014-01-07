package org.opendaylight.controller.sal.compatibility

import java.util.Map
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService
import org.opendaylight.controller.sal.utils.Status
import org.opendaylight.controller.sal.utils.StatusCode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification
import org.opendaylight.yangtools.yang.common.RpcResult
import org.slf4j.LoggerFactory

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId


import static extension org.opendaylight.controller.sal.compatibility.MDFlowMapping.*

import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import static extension org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils.*

class FlowProgrammerAdapter implements IPluginInFlowProgrammerService, SalFlowListener {

    private static val LOG = LoggerFactory.getLogger(FlowProgrammerAdapter);

    @Property
    private SalFlowService delegate;

    @Property
    private DataBrokerService dataBrokerService;
    
    @Property
    private IPluginOutFlowProgrammerService flowProgrammerPublisher;

    @Property
    private Map<Flow, UUID> flowToFlowId = new ConcurrentHashMap<Flow, UUID>();


    override addFlow(Node node, Flow flow) {
        return addFlowAsync(node,flow,0)
    }

    override modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        return modifyFlowAsync(node, oldFlow,newFlow,0)
    }

    override removeFlow(Node node, Flow flow) {
        return removeFlowAsync(node, flow,0);
    }

    override addFlowAsync(Node node, Flow flow, long rid) {
        var flowId = flowToFlowId.get(flow);
        if(flowId == null){
            flowId = UUID.randomUUID();
        }
        flowToFlowId.put(flow, flowId);

        writeFlow(flow.toMDFlow(flowId.toString()), new NodeKey(new NodeId(node.getNodeIDString())));
        return toStatus(true);
    }

    override modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid) {
        val flowId = flowToFlowId.remove(oldFlow);
        if(flowId == null){
            throw new IllegalArgumentException("oldFlow is unknown");
        }

        flowToFlowId.put(newFlow, flowId);
        writeFlow(newFlow.toMDFlow(flowId.toString()), new NodeKey(new NodeId(node.getNodeIDString())));
        return toStatus(true);
    }

    override removeFlowAsync(Node node, Flow adflow, long rid) {
        val flowId = flowToFlowId.remove(adflow);
        if(flowId == null){
            throw new IllegalArgumentException("adflow is unknown");
        }
        val flow = adflow.toMDFlow(flowId.toString());
        val modification = this._dataBrokerService.beginTransaction();
        val flowPath = InstanceIdentifier.builder(Nodes)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, new NodeKey(new NodeId(node.getNodeIDString())))
                .augmentation(FlowCapableNode)
                .child(Table, new TableKey(flow.getTableId()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, new FlowKey(flow.id))
                .build;
        modification.removeConfigurationData(flowPath);
        val commitFuture = modification.commit();
        return toStatus(true);
    }

    override removeAllFlows(Node node) {
        // I know this looks like a copout... but its exactly what the legacy OFplugin did
        return new Status(StatusCode.SUCCESS);
    }

    override syncSendBarrierMessage(Node node) {

        // FIXME: Update YANG model
        return null;
    }

    override asyncSendBarrierMessage(Node node) {

        // FIXME: Update YANG model
        return null;
    }

    private static def toStatus(boolean successful) {
        if (successful) {
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.INTERNALERROR);
        }
    }


    private def writeFlow(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow, NodeKey nodeKey) {
        val modification = this._dataBrokerService.beginTransaction();
        val flowPath = InstanceIdentifier.builder(Nodes)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, nodeKey)
                .augmentation(FlowCapableNode)
                .child(Table, new TableKey(flow.getTableId()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, new FlowKey(flow.id))
                .build;
        modification.putConfigurationData(flowPath, flow);
        val commitFuture = modification.commit();
        try {
            val result = commitFuture.get();
            val status = result.getResult();
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static def toStatus(RpcResult<?> result) {
        return toStatus(result.isSuccessful());
    }
    
    private static dispatch def Status processException(InterruptedException e) {
        LOG.error("Interruption occured during processing flow",e);
        return new Status(StatusCode.INTERNALERROR);
    }
    
    private static dispatch def Status processException(ExecutionException e) {
        LOG.error("Execution exception occured during processing flow",e.cause);
        return new Status(StatusCode.INTERNALERROR);
    }
    
    private static dispatch def Status processException(Exception e) {
        throw new RuntimeException(e);
    }
    
    override onFlowAdded(FlowAdded notification) {
        // NOOP : Not supported by AD SAL
    }
    
    override onFlowRemoved(FlowRemoved notification) {
        flowProgrammerPublisher.flowRemoved(notification.node.toADNode,notification.toFlow());
    }
    
    override onFlowUpdated(FlowUpdated notification) {
        // NOOP : Not supported by AD SAL
    }
    
    override onSwitchFlowRemoved(SwitchFlowRemoved notification) {
        // NOOP : Not supported by AD SAL
    }
    
     override onNodeErrorNotification(NodeErrorNotification notification) {
        // NOOP : Not supported by AD SAL
    }
    
     override onNodeExperimenterErrorNotification(
                NodeExperimenterErrorNotification notification) {
        // NOOP : Not supported by AD SAL
    }
}
