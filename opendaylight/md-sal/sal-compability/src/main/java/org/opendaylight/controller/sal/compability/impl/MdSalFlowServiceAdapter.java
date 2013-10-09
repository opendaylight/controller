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

public class MdSalFlowServiceAdapter implements IPluginInFlowProgrammerService {

    private static final Logger LOG = LoggerFactory.getLogger(MdSalFlowServiceAdapter.class);
    private SalFlowService delegate;

    public SalFlowService getDelegate() {
        return delegate;        
    }

    public void setDelegate(SalFlowService delegate) {
        this.delegate = delegate;
    }

    @Override
    //node isn't used in the method
    public Status addFlow(Node node, Flow flow) {
        AddFlowInput input = null; //data from flow??? flowFrom method doesn't exists for AddFlowInput
        
        Future<RpcResult<Void>> future = delegate.addFlow(input );
        RpcResult<Void> result;
        try {
            result = future.get();
            return toStatus(result); //how get status from result? conversion?
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Flow Add not processed", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    //old Flow - what it the purpose?
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        UpdateFlowInput input = null;
        Future<RpcResult<Void>> future = delegate.updateFlow(input );
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
        RemoveFlowInput input = null;
        Future<RpcResult<Void>> future = delegate.removeFlow(input );
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
        return null;
    }

    @Override
    public Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removeFlowAsync(Node node, Flow flow, long rid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removeAllFlows(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status syncSendBarrierMessage(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status asyncSendBarrierMessage(Node node) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private Status toStatus(RpcResult<Void> result) {
        // TODO Auto-generated method stub
        return null;
    }
}
