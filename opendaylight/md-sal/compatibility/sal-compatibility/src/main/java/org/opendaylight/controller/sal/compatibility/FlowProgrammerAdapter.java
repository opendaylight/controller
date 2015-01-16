/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices.cacheMode;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowProgrammerAdapter implements IPluginInFlowProgrammerService, SalFlowListener {
    private final static Logger LOG = LoggerFactory.getLogger(FlowProgrammerAdapter.class);

    // Note: clustering services manipulate this
    private final Map<Flow, UUID> flowToFlowId = new ConcurrentHashMap<Flow, UUID>();
    private final static String CACHE_NAME = "flowprogrammeradapter.flowtoid";

    // These are injected via Apache DM (see ComponentActivator)
    private IPluginOutFlowProgrammerService flowProgrammerPublisher;
    private IClusterGlobalServices clusterGlobalServices;
    private DataBrokerService dataBrokerService;
    private SalFlowService delegate;

    public SalFlowService getDelegate() {
        return this.delegate;
    }

    public void setDelegate(final SalFlowService delegate) {
        this.delegate = delegate;
    }

    public DataBrokerService getDataBrokerService() {
        return this.dataBrokerService;
    }

    public void setDataBrokerService(final DataBrokerService dataBrokerService) {
        this.dataBrokerService = dataBrokerService;
    }

    public IPluginOutFlowProgrammerService getFlowProgrammerPublisher() {
        return this.flowProgrammerPublisher;
    }

    public void setFlowProgrammerPublisher(final IPluginOutFlowProgrammerService flowProgrammerPublisher) {
        this.flowProgrammerPublisher = flowProgrammerPublisher;
    }

    public IClusterGlobalServices getClusterGlobalServices() {
        return this.clusterGlobalServices;
    }

    public void setClusterGlobalServices(final IClusterGlobalServices clusterGlobalServices) {
        this.clusterGlobalServices = clusterGlobalServices;
    }

    @Override
    public Status addFlow(final Node node, final Flow flow) {
        return toFutureStatus(internalAddFlowAsync(node, flow, 0));
    }

    @Override
    public Status modifyFlow(final Node node, final Flow oldFlow, final Flow newFlow) {
        return toFutureStatus(internalModifyFlowAsync(node, oldFlow, newFlow, 0));
    }

    @Override
    public Status removeFlow(final Node node, final Flow flow) {
        return toFutureStatus(internalRemoveFlowAsync(node, flow, 0));
    }

    @Override
    public Status addFlowAsync(final Node node, final Flow flow, final long rid) {
        // FIXME is this correct? What if the future fails?
        this.internalAddFlowAsync(node, flow, rid);
        return FlowProgrammerAdapter.toStatus(true);
    }

    @Override
    public Status modifyFlowAsync(final Node node, final Flow oldFlow, final Flow newFlow, final long rid) {
        // FIXME is this correct? What if the future fails?
        this.internalModifyFlowAsync(node, oldFlow, newFlow, rid);
        return FlowProgrammerAdapter.toStatus(true);
    }

    @Override
    public Status removeFlowAsync(final Node node, final Flow flow, final long rid) {
        // FIXME is this correct? What if the future fails?
        this.internalRemoveFlowAsync(node, flow, rid);
        return FlowProgrammerAdapter.toStatus(true);
    }

    @Override
    public Status removeAllFlows(final Node node) {
        // FIXME: unfinished?
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status syncSendBarrierMessage(final Node node) {
        // FIXME: unfinished?
        return null;
    }

    @Override
    public Status asyncSendBarrierMessage(final Node node) {
        // FIXME: unfinished?
        return null;
    }

    private static Status toStatus(final boolean successful) {
        return new Status(successful ? StatusCode.SUCCESS : StatusCode.INTERNALERROR);
    }

    public static Status toStatus(final RpcResult<? extends Object> result) {
        return toStatus(result.isSuccessful());
    }

    @Override
    public void onFlowAdded(final FlowAdded notification) {
        // FIXME: unfinished?
    }

    @Override
    public void onFlowRemoved(final FlowRemoved notification) {
        // notified upon remove flow rpc successfully invoked
        if (notification == null) {
            return;
        }

        final NodeRef node = notification.getNode();
        if (node == null) {
            LOG.debug("Notification {} has not node, ignoring it", notification);
            return;
        }

        Node adNode;
        try {
            adNode = NodeMapping.toADNode(notification.getNode());
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct AD node for {}, ignoring notification", node, e);
            return;
        }
        flowProgrammerPublisher.flowRemoved(adNode, ToSalConversionsUtils.toFlow(notification, adNode));
    }

    @Override
    public void onFlowUpdated(final FlowUpdated notification) {
        // FIXME: unfinished?
    }

    @Override
    public void onSwitchFlowRemoved(final SwitchFlowRemoved notification) {
        // notified upon remove flow message from device arrives
        if (notification == null) {
            return;
        }

        final NodeRef node = notification.getNode();
        if (node == null) {
            LOG.debug("Notification {} has not node, ignoring it", notification);
            return;
        }

        Node adNode;
        try {
            adNode = NodeMapping.toADNode(notification.getNode());
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct AD node for {}, ignoring notification", node, e);
            return;
        }
        flowProgrammerPublisher.flowRemoved(adNode, ToSalConversionsUtils.toFlow(notification, adNode));
    }

    @Override
    public void onNodeErrorNotification(final NodeErrorNotification notification) {
        // FIXME: unfinished?
    }

    @Override
    public void onNodeExperimenterErrorNotification(final NodeExperimenterErrorNotification notification) {
        // FIXME: unfinished?
    }

    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flowPath(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow, final NodeKey nodeKey) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey)
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class, new FlowKey(flow.getId()))
                .build();
    }

    private Future<RpcResult<TransactionStatus>> writeFlowAsync(final org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow, final NodeKey nodeKey) {
        final DataModificationTransaction modification = this.dataBrokerService.beginTransaction();
        modification.putConfigurationData(flowPath(flow, nodeKey), flow);
        return modification.commit();
    }

    private Future<RpcResult<TransactionStatus>> internalAddFlowAsync(final Node node, final Flow flow, final long rid) {
        final Map<Flow,UUID> cache = this.getCache();
        UUID flowId = cache.get(flow);
        if (flowId != null) {
            this.removeFlow(node, flow);
        }

        flowId = UUID.randomUUID();
        cache.put(flow, flowId);
        return this.writeFlowAsync(MDFlowMapping.toMDFlow(flow, flowId.toString()), new NodeKey(
                new NodeId(NodeMapping.OPENFLOW_ID_PREFIX + node.getID())));
    }

    private Future<RpcResult<TransactionStatus>> internalModifyFlowAsync(final Node node, final Flow oldFlow, final Flow newFlow, final long rid) {
        final Map<Flow,UUID> cache = this.getCache();

        UUID flowId = cache.remove(oldFlow);
        if (flowId == null) {
            flowId = UUID.randomUUID();
            cache.put(oldFlow, flowId);
            LOG.warn("Could not find flow {} in cache, assigned new ID {}", oldFlow.hashCode(), flowId);
        }

        cache.put(newFlow, flowId);
        return this.writeFlowAsync(MDFlowMapping.toMDFlow(newFlow, flowId.toString()), new NodeKey(
                new NodeId(NodeMapping.OPENFLOW_ID_PREFIX + node.getID())));
    }

    private Future<RpcResult<TransactionStatus>> internalRemoveFlowAsync(final Node node, final Flow adflow, final long rid) {
        final Map<Flow,UUID> cache = this.getCache();

        final UUID flowId = cache.remove(adflow);
        if (flowId == null) {
            LOG.warn("Could not find flow {} in cache, nothing to do", adflow.hashCode());
            return null;
        }

        final org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow = MDFlowMapping.toMDFlow(adflow, flowId.toString());
        final DataModificationTransaction modification = this.dataBrokerService.beginTransaction();
        modification.removeConfigurationData(flowPath(flow, new NodeKey(
                new NodeId(NodeMapping.OPENFLOW_ID_PREFIX + node.getID()))));
        return modification.commit();
    }

    private static Status toFutureStatus(final Future<RpcResult<TransactionStatus>> future) {
        if (future == null) {
            // FIXME: really?
            return FlowProgrammerAdapter.toStatus(true);
        }

        try {
            final RpcResult<TransactionStatus> result = future.get();
            return FlowProgrammerAdapter.toStatus(result);
        } catch (final InterruptedException e) {
            FlowProgrammerAdapter.LOG.error("Interrupted while processing flow", e);
        } catch (ExecutionException e) {
            FlowProgrammerAdapter.LOG.error("Failed to process flow", e);
        }

        return new Status(StatusCode.INTERNALERROR);
    }

    @SuppressWarnings("unchecked")
    private Map<Flow,UUID> getCache() {
        final IClusterGlobalServices cgs = getClusterGlobalServices();
        if (cgs == null) {
            return new ConcurrentHashMap<Flow, UUID>();
        }

        Map<Flow, UUID> cache = (Map<Flow, UUID>) cgs.getCache(FlowProgrammerAdapter.CACHE_NAME);
        if (cache != null) {
            return cache;
        }

        try {
            return (Map<Flow, UUID>) cgs.createCache(CACHE_NAME, EnumSet.of(cacheMode.TRANSACTIONAL));
        } catch (CacheExistException e) {
            return (Map<Flow, UUID>) cgs.getCache(CACHE_NAME);
        } catch (CacheConfigException e) {
            throw new IllegalStateException("Unexpected cache configuration problem", e);
        }
    }

}
