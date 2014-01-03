package org.opendaylight.controller.sal.compatibility

import java.util.concurrent.ExecutionException
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


import static org.opendaylight.controller.sal.compatibility.MDFlowMapping.*

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

    override addFlow(Node node, Flow flow) {
        val input = addFlowInput(node, flow);
        writeFlow(input, new NodeKey(new NodeId(node.getNodeIDString())), new FlowKey(new FlowId(flow.getId())));
        return toStatus(true);
    }

    override modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        val input = updateFlowInput(node, oldFlow, newFlow);
        val future = delegate.updateFlow(input);
        try {
            val result = future.get();
            return toStatus(result);
        } catch (Exception e) {
            return processException(e);
        }
    }

    override removeFlow(Node node, Flow flow) {
        val input = removeFlowInput(node, flow);
        val future = delegate.removeFlow(input);

        try {
            val result = future.get();
            return toStatus(result);
        } catch (Exception e) {
            return processException(e);
        }
    }

    override addFlowAsync(Node node, Flow flow, long rid) {
        val input = addFlowInput(node, flow);
        delegate.addFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    override modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid) {
        val input = updateFlowInput(node, oldFlow, newFlow);
        delegate.updateFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    override removeFlowAsync(Node node, Flow flow, long rid) {
        val input = removeFlowInput(node, flow);
        delegate.removeFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    override removeAllFlows(Node node) {
        throw new UnsupportedOperationException("Not present in MD-SAL");
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


    private def writeFlow(AddFlowInput flow, NodeKey nodeKey, FlowKey flowKey) {
        val modification = this._dataBrokerService.beginTransaction();
        val flowPath = InstanceIdentifier.builder(Nodes)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, nodeKey).augmentation(FlowCapableNode)
                .child(Table, new TableKey(flow.getTableId())).child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, flowKey).build();
        modification.putOperationalData(flowPath, flow);
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
