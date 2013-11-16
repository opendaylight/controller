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

import static org.opendaylight.controller.sal.compatibility.MDFlowMapping.*

import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import static extension org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils.*

class FlowProgrammerAdapter implements IPluginInFlowProgrammerService, SalFlowListener {

    private static val LOG = LoggerFactory.getLogger(FlowProgrammerAdapter);

    @Property
    private SalFlowService delegate;
    
    @Property
    private IPluginOutFlowProgrammerService flowProgrammerPublisher;

    override addFlow(Node node, Flow flow) {
        val input = addFlowInput(node, flow);
        val future = delegate.addFlow(input);
        try {
            val result = future.get();
            return toStatus(result); // how get status from result? conversion?
        } catch (Exception e) {
            return processException(e);
        }
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

    public static def toStatus(RpcResult<?> result) {
        if (result.isSuccessful()) {
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.INTERNALERROR);
        }
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
