package org.opendaylight.controller.sal.compability.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.controller.sal.compability.FromSalConversionsUtils.*;

public class MdSalFlowServiceAdapter implements IPluginInFlowProgrammerService {

    private static final Logger LOG = LoggerFactory
            .getLogger(MdSalFlowServiceAdapter.class);
    private SalFlowService delegate;

    public SalFlowService getDelegate() {
        return delegate;
    }

    public void setDelegate(SalFlowService delegate) {
        this.delegate = delegate;
    }

    @Override
    // node isn't used in the method
    public Status addFlow(Node node, Flow flow) {
        AddFlowInput input = addFlowInput(node, flow);
        Future<RpcResult<Void>> future = delegate.addFlow(input);
        RpcResult<Void> result;
        try {
            result = future.get();
            return toStatus(result); // how get status from result? conversion?
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Flow Add not processed", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    // old Flow - what it the purpose?
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        UpdateFlowInput input = updateFlowInput(node,oldFlow,newFlow);
        Future<RpcResult<Void>> future = delegate.updateFlow(input);
        RpcResult<Void> result;
        try {
            result = future.get();
            return toStatus(result);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Flow Modify not processed", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    public Status removeFlow(Node node, Flow flow) {
        RemoveFlowInput input = removeFlowInput(node,flow);
        Future<RpcResult<Void>> future = delegate.removeFlow(input);
        RpcResult<Void> result;
        try {
            result = future.get();
            return toStatus(result);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Flow Modify not processed", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    public Status addFlowAsync(Node node, Flow flow, long rid) {
        AddFlowInput input = addFlowInput(node, flow);
        delegate.addFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow,
            long rid) {
        UpdateFlowInput input = updateFlowInput(node,oldFlow,newFlow);
        delegate.updateFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removeFlowAsync(Node node, Flow flow, long rid) {
        RemoveFlowInput input = removeFlowInput(node,flow);
        delegate.removeFlow(input);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removeAllFlows(Node node) {
        throw new UnsupportedOperationException("Not present in MD-SAL");
    }

    @Override
    public Status syncSendBarrierMessage(Node node) {
        
        return null;
    }

    @Override
    public Status asyncSendBarrierMessage(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    private Status toStatus(RpcResult<Void> result) {
        if(result.isSuccessful()) {
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.INTERNALERROR);
        }
    }
}
