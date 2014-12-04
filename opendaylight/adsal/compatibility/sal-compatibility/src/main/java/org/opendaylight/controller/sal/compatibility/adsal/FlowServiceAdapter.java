/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.adsal;

import java.math.BigInteger;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
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
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

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
    public ListenableFuture<RpcResult<AddFlowOutput>> addFlow(AddFlowInput input) {

        Flow flow = ToSalConversionsUtils.toFlow(input, null);
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Status status = delegate.addFlowAsync(node, flow);
        AddFlowOutputBuilder builder = new AddFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        AddFlowOutput rpcResultType = builder.build();
        return Futures.immediateFuture(RpcResultBuilder.<AddFlowOutput>status(status.isSuccess())
                .withResult(rpcResultType).build());
    }

    @Override
    public ListenableFuture<RpcResult<RemoveFlowOutput>> removeFlow(RemoveFlowInput input) {

        Flow flow = ToSalConversionsUtils.toFlow(input, null);
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Status status = delegate.removeFlowAsync(node, flow);
        RemoveFlowOutputBuilder builder = new RemoveFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        RemoveFlowOutput rpcResultType = builder.build();
        return Futures.immediateFuture(RpcResultBuilder.<RemoveFlowOutput>status(status.isSuccess())
                                                         .withResult(rpcResultType).build());

    }

    @Override
    public ListenableFuture<RpcResult<UpdateFlowOutput>> updateFlow(UpdateFlowInput input) {
        @SuppressWarnings("unchecked")
        org.opendaylight.controller.sal.core.Node node = InventoryMapping.toAdNode((InstanceIdentifier<Node>) input
                .getNode().getValue());
        Flow originalFlow = ToSalConversionsUtils.toFlow(input.getOriginalFlow(), null);
        Flow updatedFlow = ToSalConversionsUtils.toFlow(input.getUpdatedFlow(), null);
        Status status = delegate.modifyFlowAsync(node, originalFlow, updatedFlow);
        UpdateFlowOutputBuilder builder = new UpdateFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(status.getRequestId())));
        UpdateFlowOutput rpcResultType = builder.build();
        throw new UnsupportedOperationException("Need to translate AD-SAL status to MD-SAL UpdateFlowOuptut - eaw@cisco.com");
        // return Futures.immediateFuture(Rpcs.getRpcResult(status.isSuccess(), rpcResultType, null));
    }
}
