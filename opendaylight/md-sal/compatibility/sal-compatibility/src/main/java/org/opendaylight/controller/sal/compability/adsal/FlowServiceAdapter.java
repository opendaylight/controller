package org.opendaylight.controller.sal.compability.adsal;

import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.compability.NodeMapping;
import org.opendaylight.controller.sal.compability.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerListener;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowServiceAdapter implements SalFlowService, IFlowProgrammerListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowServiceAdapter.class);

    private IFlowProgrammerService delegate;

    private NotificationProviderService publish;

    @Override
    public void flowRemoved(Node node, Flow flow) {
        FlowRemovedBuilder flowRemovedBuilder = new FlowRemovedBuilder();
        flowRemovedBuilder.setNode(NodeMapping.toNodeRef(node));
        publish.publish(flowRemovedBuilder.build());
    }

    @Override
    public void flowErrorReported(Node node, long rid, Object err) {
        // TODO Auto-generated method stub

    }

    @Override
    public Future<RpcResult<Void>> addFlow(AddFlowInput input) {
        try {
            Flow flow = ToSalConversionsUtils.toFlow(input);
            Node node = NodeMapping.toADNode(input.getNode());
            Status status = delegate.addFlowAsync(node, flow);
            Void rpcResultType = null;
            return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Future<RpcResult<Void>> removeFlow(RemoveFlowInput input) {
        try {
            Flow flow = ToSalConversionsUtils.toFlow(input);
            Node node = NodeMapping.toADNode(input.getNode());
            Status status = delegate.removeFlowAsync(node, flow);
            Void rpcResultType = null;
            return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Future<RpcResult<Void>> updateFlow(UpdateFlowInput input) {
        try {
            Node node = NodeMapping.toADNode(input.getNode());
            Flow originalFlow = ToSalConversionsUtils.toFlow(input.getOriginalFlow());
            Flow updatedFlow = ToSalConversionsUtils.toFlow(input.getUpdatedFlow());
            Status status = delegate.modifyFlowAsync(node, originalFlow, updatedFlow);
            Void rpcResultType = null;
            return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }
}
