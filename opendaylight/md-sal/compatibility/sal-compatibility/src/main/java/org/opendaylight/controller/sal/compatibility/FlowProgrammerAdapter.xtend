/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility

import java.util.Map
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.EnumSet
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService
import org.opendaylight.controller.sal.utils.Status
import org.opendaylight.controller.sal.utils.StatusCode
import org.opendaylight.controller.clustering.services.CacheExistException
import org.opendaylight.controller.clustering.services.IClusterGlobalServices
import org.opendaylight.controller.clustering.services.IClusterServices

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId


import static extension org.opendaylight.controller.sal.compatibility.MDFlowMapping.*

import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import static extension org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils.*

class FlowProgrammerAdapter implements IPluginInFlowProgrammerService, SalFlowListener {

    private static val LOG = LoggerFactory.getLogger(FlowProgrammerAdapter);
    private static val CACHE_NAME = "flowprogrammeradapter.flowtoid";

    @Property
    private SalFlowService delegate;

    @Property
    private DataBrokerService dataBrokerService;
    
    @Property
    private IPluginOutFlowProgrammerService flowProgrammerPublisher;

    @Property
    private IClusterGlobalServices clusterGlobalServices;


    @Property
    private Map<Flow, UUID> flowToFlowId = new ConcurrentHashMap<Flow, UUID>();


    override addFlow(Node node, Flow flow) {
        return toFutureStatus(internalAddFlowAsync(node,flow,0));
    }

    override modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        return toFutureStatus(internalModifyFlowAsync(node, oldFlow,newFlow,0));
    }

    override removeFlow(Node node, Flow flow) {
        return toFutureStatus(internalRemoveFlowAsync(node, flow,0));
    }

    override addFlowAsync(Node node, Flow flow, long rid) {
        internalAddFlowAsync(node, flow, rid);
        return toStatus(true);
    }

    override modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid) {
        internalModifyFlowAsync(node, oldFlow, newFlow, rid);
        return toStatus(true);
    }

    override removeFlowAsync(Node node, Flow flow, long rid) {
        internalRemoveFlowAsync(node, flow, rid);
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
        if(notification != null && notification.node != null) {
            val adNode = notification.node.toADNode
            if(adNode != null) {
                flowProgrammerPublisher.flowRemoved(adNode,notification.toFlow(adNode));
            }
        } 
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

    private def Future<RpcResult<TransactionStatus>> writeFlowAsync(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow, NodeKey nodeKey){
        val modification = this._dataBrokerService.beginTransaction();
        val flowPath = InstanceIdentifier.builder(Nodes)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, nodeKey)
                .augmentation(FlowCapableNode)
                .child(Table, new TableKey(flow.getTableId()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow, new FlowKey(flow.id))
                .build;
        modification.putConfigurationData(flowPath, flow);
        return modification.commit();
    }

    private def Future<RpcResult<TransactionStatus>> internalAddFlowAsync(Node node, Flow flow, long rid){
        var flowId = getCache().get(flow);
        if(flowId != null) {
            removeFlow(node, flow);
            return internalAddFlowAsync(node, flow, rid);
        }

        flowId = UUID.randomUUID();
        getCache().put(flow, flowId);

        return writeFlowAsync(flow.toMDFlow(flowId.toString()), new NodeKey(new NodeId(node.getNodeIDString())));
    }

    private def Future<RpcResult<TransactionStatus>> internalModifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid) {
        var flowId = getCache().remove(oldFlow);
        if(flowId == null){
            LOG.error("oldFlow not found in cache : " + oldFlow.hashCode);
            flowId = UUID.randomUUID();
            getCache().put(oldFlow, flowId);
        }

        getCache().put(newFlow, flowId);
        return writeFlowAsync(newFlow.toMDFlow(flowId.toString()), new NodeKey(new NodeId(node.getNodeIDString())));
    }


    private def Future<RpcResult<TransactionStatus>> internalRemoveFlowAsync(Node node, Flow adflow, long rid){
        val flowId = getCache().remove(adflow);
        if(flowId == null){
            //throw new IllegalArgumentException("adflow not found in cache : " + adflow.hashCode);
            LOG.error("adflow not found in cache : " + adflow.hashCode);
            return null;
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
        return modification.commit();
    }

    private def toFutureStatus(Future<RpcResult<TransactionStatus>> future){
        if(future == null){
            return toStatus(true);
        }

        try {
            val result = future.get();
            return toStatus(result);
        } catch (InterruptedException e) {
            return processException(e);
        } catch (ExecutionException e) {
            return processException(e);
        } catch (Exception e){
            processException(e);
        }
        return toStatus(false);
    }

    private def Map<Flow, UUID> getCache(){
        if(clusterGlobalServices == null){
            return new ConcurrentHashMap<Flow, UUID>();
        }

        var cache = clusterGlobalServices.getCache(CACHE_NAME);

        if(cache == null) {
            try {
                cache = clusterGlobalServices.createCache(CACHE_NAME, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            } catch (CacheExistException e) {
                cache = clusterGlobalServices.getCache(CACHE_NAME);
            }
        }
        return cache as Map<Flow, UUID>;

    }

}
