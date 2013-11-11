package org.opendaylight.controller.sal.compatibility.adsal;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerListener;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowServiceAdapter implements SalFlowService, IFlowProgrammerListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowServiceAdapter.class);

    private IFlowProgrammerService delegate;

    private NotificationProviderService publish;

    @Override
    public void flowRemoved(org.opendaylight.controller.sal.core.Node node, Flow flow) {
        FlowRemovedBuilder flowRemovedBuilder = new FlowRemovedBuilder();
        flowRemovedBuilder.setNode(InventoryMapping.toNodeRef(node));
        publish.publish(flowRemovedBuilder.build());
    }

    @Override
    public void flowErrorReported(org.opendaylight.controller.sal.core.Node node, long rid, Object err) {
        // TODO Auto-generated method stub

    }

    @Override
    public Future<RpcResult<AddFlowOutput>> addFlow(AddFlowInput input) {

        Flow flow = ToSalConversionsUtils.toFlow(input);
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Status status = delegate.addFlowAsync(node, flow);
        AddFlowOutputBuilder builder = new AddFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        AddFlowOutput rpcResultType = builder.build();
        return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
    }

    @Override
    public Future<RpcResult<RemoveFlowOutput>> removeFlow(RemoveFlowInput input) {

        Flow flow = ToSalConversionsUtils.toFlow(input);
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Status status = delegate.removeFlowAsync(node, flow);
        RemoveFlowOutputBuilder builder = new RemoveFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        RemoveFlowOutput rpcResultType = builder.build();
        return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));

    }

    @Override
    public Future<RpcResult<UpdateFlowOutput>> updateFlow(UpdateFlowInput input) {
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Flow originalFlow = ToSalConversionsUtils.toFlow(input.getOriginalFlow());
        Flow updatedFlow = ToSalConversionsUtils.toFlow(input.getUpdatedFlow());
        Status status = delegate.modifyFlowAsync(node, originalFlow, updatedFlow);
        UpdateFlowOutputBuilder builder = new UpdateFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        UpdateFlowOutput rpcResultType = builder.build();
        throw new UnsupportedOperationException("Need to translate AD-SAL status to MD-SAL UpdateFlowOuptut - eaw@cisco.com");
        // return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
    }
}
