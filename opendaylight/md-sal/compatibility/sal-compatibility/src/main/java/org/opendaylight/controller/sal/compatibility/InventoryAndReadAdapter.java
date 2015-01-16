/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.NodeTable.NodeTableIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IPluginOutReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Packets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class InventoryAndReadAdapter implements IPluginInReadService, IPluginInInventoryService, OpendaylightFlowStatisticsListener, OpendaylightFlowTableStatisticsListener, OpendaylightPortStatisticsListener {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryAndReadAdapter.class);
    private static final short OPENFLOWV10_TABLE_ID = 0;
    private static final int SLEEP_FOR_NOTIFICATIONS_MILLIS = 500;

    private final InventoryNotificationProvider inventoryNotificationProvider = new InventoryNotificationProvider();
    private final Map<PathArgument,List<PathArgument>> nodeToNodeConnectorsMap = new ConcurrentHashMap<>();
    private List<IPluginOutInventoryService> inventoryPublisher = new CopyOnWriteArrayList<>();
    private List<IPluginOutReadService> statisticsPublisher = new CopyOnWriteArrayList<>();
    private Cache<String, TransactionNotificationList<? extends TransactionAware>> txCache;

    private OpendaylightFlowTableStatisticsService flowTableStatisticsService;
    private OpendaylightPortStatisticsService nodeConnectorStatisticsService;
    private OpendaylightFlowStatisticsService flowStatisticsService;
    private FlowTopologyDiscoveryService topologyDiscovery;
    private DataProviderService dataProviderService;
    private DataBrokerService dataService;

    public DataBrokerService getDataService() {
        return dataService;
    }

    public void setDataService(final DataBrokerService dataService) {
        this.dataService = dataService;
    }

    public DataProviderService getDataProviderService() {
        return dataProviderService;
    }

    public void setDataProviderService(final DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    public OpendaylightFlowStatisticsService getFlowStatisticsService() {
        return flowStatisticsService;
    }

    public void setFlowStatisticsService(final OpendaylightFlowStatisticsService flowStatisticsService) {
        this.flowStatisticsService = flowStatisticsService;
    }

    public OpendaylightPortStatisticsService getNodeConnectorStatisticsService() {
        return nodeConnectorStatisticsService;
    }

    public void setNodeConnectorStatisticsService(final OpendaylightPortStatisticsService nodeConnectorStatisticsService) {
        this.nodeConnectorStatisticsService = nodeConnectorStatisticsService;
    }

    public OpendaylightFlowTableStatisticsService getFlowTableStatisticsService() {
        return flowTableStatisticsService;
    }

    public void setFlowTableStatisticsService(final OpendaylightFlowTableStatisticsService flowTableStatisticsService) {
        this.flowTableStatisticsService = flowTableStatisticsService;
    }

    public FlowTopologyDiscoveryService getTopologyDiscovery() {
        return topologyDiscovery;
    }

    public void setTopologyDiscovery(final FlowTopologyDiscoveryService topologyDiscovery) {
        this.topologyDiscovery = topologyDiscovery;
    }

    public List<IPluginOutReadService> getStatisticsPublisher() {
        return statisticsPublisher;
    }

    public void setStatisticsPublisher(final List<IPluginOutReadService> statisticsPublisher) {
        this.statisticsPublisher = statisticsPublisher;
    }

    public List<IPluginOutInventoryService> getInventoryPublisher() {
        return inventoryPublisher;
    }

    public void setInventoryPublisher(final List<IPluginOutInventoryService> inventoryPublisher) {
        this.inventoryPublisher = inventoryPublisher;
    }

    public void startAdapter() {
        inventoryNotificationProvider.setDataProviderService(getDataProviderService());
        inventoryNotificationProvider.setInventoryPublisher(getInventoryPublisher());
        txCache = CacheBuilder.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(10000).build();
        // inventoryNotificationProvider.start();
    }

    public boolean setInventoryPublisher(final IPluginOutInventoryService listener) {
        return getInventoryPublisher().add(listener);
    }

    public boolean unsetInventoryPublisher(final IPluginOutInventoryService listener) {
        return getInventoryPublisher().remove(listener);
    }

    public boolean setReadPublisher(final IPluginOutReadService listener) {
        return getStatisticsPublisher().add(listener);
    }

    public Boolean unsetReadPublisher(final IPluginOutReadService listener) {
        if (listener != null) {
            return getStatisticsPublisher().remove(listener);
        }
        return false;
    }

    protected DataModificationTransaction startChange() {
        return getDataProviderService().beginTransaction();
    }

    @Override
    public long getTransmitRate(final NodeConnector connector) {
        final FlowCapableNodeConnector nodeConnector = this.readOperFlowCapableNodeConnector(NodeMapping.toNodeConnectorRef(connector));
        return nodeConnector.getCurrentSpeed().longValue();
    }

    private FlowCapableNode readOperFlowCapableNode(final NodeRef ref) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node =
                (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node)getDataService().readOperationalData(ref.getValue());
        if (node == null) {
            return null;
        }

        return node.getAugmentation(FlowCapableNode.class);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node readConfigNode(final Node node) {
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, InventoryMapping.toNodeKey(node))
                .build();

        return (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) startChange().readConfigurationData(nodeRef);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector readConfigNodeConnector(final NodeConnector connector) {
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> nodeConnectorRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, InventoryMapping.toNodeKey(connector.getNode()))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, InventoryMapping.toNodeConnectorKey(connector))
                .build();

        return((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector) startChange().readConfigurationData(nodeConnectorRef));
    }

    /**
     * Read a table of a node from configuration data store.
     *
     * @param node Node id
     * @param id Table id
     * @return Table contents, or null if not present
     */
    private Table readOperationalTable(final Node node, final short id) {
        final InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, NodeMapping.toNodeKey(node))
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(id))
                .build();

        return (Table) startChange().readOperationalData(tableRef);
    }

    @Override
    public List<FlowOnNode> readAllFlow(final Node node, final boolean cached) {
        final ArrayList<FlowOnNode> ret= new ArrayList<>();
        if (cached) {
            final Table table = readOperationalTable(node, OPENFLOWV10_TABLE_ID);
            if (table != null) {
                final List<Flow> flows = table.getFlow();
                LOG.trace("Number of flows installed in table 0 of node {} : {}", node, flows.size());

                for (final Flow flow : flows) {
                    final FlowStatisticsData statsFromDataStore = flow.getAugmentation(FlowStatisticsData.class);
                    if (statsFromDataStore != null) {
                        final FlowOnNode it = new FlowOnNode(ToSalConversionsUtils.toFlow(flow, node));
                        ret.add(addFlowStats(it, statsFromDataStore.getFlowStatistics()));
                    }
                }
            }
        } else {
            LOG.debug("readAllFlow cached:{}", cached);
            GetAllFlowStatisticsFromFlowTableInput input =
                new GetAllFlowStatisticsFromFlowTableInputBuilder()
                    .setNode(NodeMapping.toNodeRef(node))
                    .setTableId(new TableId(OPENFLOWV10_TABLE_ID))
                    .build();

            Future<RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> future =
                getFlowStatisticsService().getAllFlowStatisticsFromFlowTable(input);

            RpcResult<GetAllFlowStatisticsFromFlowTableOutput> result = null;
            try {
                // having a blocking call is fine here, as we need to join
                // the notifications and return the result
                result = future.get();
            } catch (Exception e) {
               LOG.error("Exception in getAllFlowStatisticsFromFlowTable ", e);
               return ret;
            }

            GetAllFlowStatisticsFromFlowTableOutput output = result.getResult();
            if (output == null) {
                return ret;
            }

            TransactionId transactionId = output.getTransactionId();
            String cacheKey = buildCacheKey(transactionId, NodeMapping.toNodeId(node));
            LOG.info("readAllFlow transactionId:{} cacheKey:{}", transactionId, cacheKey);

            // insert an entry in tempcache, will get updated when notification is received
            txCache.put(cacheKey, new TransactionNotificationList<FlowsStatisticsUpdate>(
                transactionId, node.getNodeIDString()));

            TransactionNotificationList<FlowsStatisticsUpdate> txnList =
                (TransactionNotificationList<FlowsStatisticsUpdate>) txCache.getIfPresent(cacheKey);

            // this loop would not be infinite as the cache will remove an entry
            // after defined time if not written to
            while (txnList != null && !txnList.areAllNotificationsGathered()) {
                LOG.debug("readAllFlow waiting for notification...");
                waitForNotification();
                txnList = (TransactionNotificationList<FlowsStatisticsUpdate>) txCache.getIfPresent(cacheKey);
            }

            if (txnList == null) {
                return ret;
            }

            List<FlowsStatisticsUpdate> notifications = txnList.getNotifications();
            for (FlowsStatisticsUpdate flowsStatisticsUpdate : notifications) {
                List<FlowAndStatisticsMapList> flowAndStatisticsMapList = flowsStatisticsUpdate.getFlowAndStatisticsMapList();
                if (flowAndStatisticsMapList != null) {
                    for (FlowAndStatisticsMapList flowAndStatistics : flowAndStatisticsMapList) {
                        final FlowOnNode it = new FlowOnNode(ToSalConversionsUtils.toFlow(flowAndStatistics, node));
                        ret.add(addFlowStats(it, flowAndStatistics));
                    }
                }
            }
        }
        return ret;
    }

    private String buildCacheKey(final TransactionId id, final NodeId nodeId) {
        return String.valueOf(id.getValue()) + "-" + nodeId.getValue();
    }

    private void waitForNotification() {
        try {
            // going for a simple sleep approach,as wait-notify on a monitor would require
            // us to maintain monitors per txn-node combo
            Thread.sleep(SLEEP_FOR_NOTIFICATIONS_MILLIS);
            LOG.trace("statCollector is waking up from a wait stat Response sleep");
        } catch (final InterruptedException e) {
            LOG.warn("statCollector has been interrupted waiting stat Response sleep", e);
        }
    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(final Node node, final boolean cached) {
        final ArrayList<NodeConnectorStatistics> ret = new ArrayList<>();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dsNode = readConfigNode(node);
        if (dsNode != null) {
            for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector dsNodeConnector : dsNode.getNodeConnector()) {
                final FlowCapableNodeConnectorStatistics stats = (dsNodeConnector.getAugmentation(FlowCapableNodeConnectorStatisticsData.class));
                if (stats != null) {
                    try {
                        ret.add(toNodeConnectorStatistics(stats.getFlowCapableNodeConnectorStatistics(), dsNode.getId(), dsNodeConnector.getId()));
                    } catch (ConstructionException e) {
                        LOG.warn("Failed to instantiate node connector statistics for node {} connector {}, ignoring it",
                                dsNode.getId(), dsNodeConnector.getId(), e);
                    }
                }
            }
        }

        //TODO: Refer TODO (main)
        getNodeConnectorStatisticsService().getAllNodeConnectorsStatistics(
                new GetAllNodeConnectorsStatisticsInputBuilder().setNode(NodeMapping.toNodeRef(node)).build());
        return ret;
    }

    @Override
    public List<NodeTableStatistics> readAllNodeTable(final Node node, final boolean cached) {
        final NodeRef nodeRef = NodeMapping.toNodeRef(node);

        final ArrayList<NodeTableStatistics> ret = new ArrayList<>();
        final FlowCapableNode dsFlowCapableNode = this.readOperFlowCapableNode(nodeRef);
        if (dsFlowCapableNode != null) {
            for (final Table table : dsFlowCapableNode.getTable()) {
                final FlowTableStatisticsData tableStats = table.getAugmentation(FlowTableStatisticsData.class);
                if (tableStats != null) {
                    try {
                        ret.add(toNodeTableStatistics(tableStats.getFlowTableStatistics(), table.getId(), node));
                    } catch (ConstructionException e) {
                        LOG.warn("Failed to instantiate table statistics for node {} table {}, ignoring it", node, table.getId(), e);
                    }
                }
            }
        }

        //TODO: Refer TODO (main)
        getFlowTableStatisticsService().getFlowTablesStatistics(new GetFlowTablesStatisticsInputBuilder().setNode(nodeRef).build());
        return ret;
    }

    @Override
    public NodeDescription readDescription(final Node node, final boolean cached) {
        return this.toNodeDescription(NodeMapping.toNodeRef(node));
    }

    @Override
    public FlowOnNode readFlow(final Node node, final org.opendaylight.controller.sal.flowprogrammer.Flow targetFlow, final boolean cached) {
        FlowOnNode ret = null;
        final Table table = readOperationalTable(node, OPENFLOWV10_TABLE_ID);
        if (table != null) {
            final List<Flow> flows = table.getFlow();
            InventoryAndReadAdapter.LOG.trace("Number of flows installed in table 0 of node {} : {}", node, flows.size());

            for (final Flow mdsalFlow : flows) {
                if(FromSalConversionsUtils.flowEquals(mdsalFlow, MDFlowMapping.toMDSalflow(targetFlow))) {
                    final FlowStatisticsData statsFromDataStore = mdsalFlow.getAugmentation(FlowStatisticsData.class);
                    if (statsFromDataStore != null) {
                        InventoryAndReadAdapter.LOG.debug("Found matching flow in the data store flow table ");
                        ret = addFlowStats(new FlowOnNode(targetFlow), statsFromDataStore.getFlowStatistics());

                        // FIXME: break; ?
                    }
                }
            }
        }

        //TODO: Refer TODO (main)
        final GetFlowStatisticsFromFlowTableInputBuilder input = new GetFlowStatisticsFromFlowTableInputBuilder().setNode(NodeMapping.toNodeRef(node));
        input.fieldsFrom(MDFlowMapping.toMDSalflow(targetFlow));
        getFlowStatisticsService().getFlowStatisticsFromFlowTable(input.build());
        return ret;
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(final NodeConnector connector, final boolean cached) {
        final NodeConnectorId ncId = InventoryMapping.toNodeConnectorKey(connector).getId();

        NodeConnectorStatistics ret = null;
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nodeConnectorFromDS = readConfigNodeConnector(connector);
        if (nodeConnectorFromDS != null) {
            final FlowCapableNodeConnectorStatistics stats = nodeConnectorFromDS.getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
            if (stats != null) {
                try {
                    ret = toNodeConnectorStatistics(stats.getFlowCapableNodeConnectorStatistics(),
                            InventoryMapping.toNodeKey(connector.getNode()).getId(), ncId);
                } catch (ConstructionException e) {
                    LOG.warn("Failed to instantiate node connector statistics for connector {}, ignoring it",
                            connector, e);
                }
            }
        }

        getNodeConnectorStatisticsService().getNodeConnectorStatistics(
                new GetNodeConnectorStatisticsInputBuilder().setNode(NodeMapping.toNodeRef(connector.getNode())).setNodeConnectorId(ncId).build());
        return ret;
    }

    @Override
    public NodeTableStatistics readNodeTable(final NodeTable nodeTable, final boolean cached) {
        NodeTableStatistics nodeStats = null;
        final Table table = readOperationalTable(nodeTable.getNode(), (short) nodeTable.getID());
        if (table != null) {
            final FlowTableStatisticsData tableStats = table.getAugmentation(FlowTableStatisticsData.class);
            if (tableStats != null) {
                try {
                    nodeStats = toNodeTableStatistics(tableStats.getFlowTableStatistics(), table.getId(), nodeTable.getNode());
                } catch (ConstructionException e) {
                    LOG.warn("Failed to instantiate table statistics for node {} table {}, ignoring it",
                            nodeTable.getNode(), table.getId(), e);
                }
            }
        }

        //TODO: Refer TODO (main)
        getFlowTableStatisticsService().getFlowTablesStatistics(
                new GetFlowTablesStatisticsInputBuilder().setNode(NodeMapping.toNodeRef(nodeTable.getNode())).build());
        return nodeStats;
    }

    public void onNodeConnectorRemovedInternal(final NodeConnectorRemoved update) {
        // Never received
    }

    public void onNodeRemovedInternal(final NodeRemoved notification) {
        this.removeNodeConnectors(notification.getNodeRef().getValue());
        try {
            final Node aDNode = NodeMapping.toADNode(notification.getNodeRef());
            this.publishNodeUpdate(aDNode, UpdateType.REMOVED, Collections.<Property>emptySet());
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node for {}, not propagating update", notification.getNodeRef(), e);
        }
    }

    public void onNodeConnectorUpdatedInternal(final NodeConnectorUpdated update) {
        final NodeConnectorRef ref = update.getNodeConnectorRef();
        final UpdateType updateType;
        if (!this.isKnownNodeConnector(ref.getValue())) {
            this.recordNodeConnector(ref.getValue());
            updateType = UpdateType.ADDED;
        } else {
            updateType = UpdateType.CHANGED;
        }

        try {
            final NodeConnector nodeConnector;
            nodeConnector = NodeMapping.toADNodeConnector(ref);
            this.publishNodeConnectorUpdate(nodeConnector, updateType, NodeMapping.toADNodeConnectorProperties(update));
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node connector for {}, not reporting the update", ref, e);
        }
    }

    public void onNodeUpdatedInternal(final NodeUpdated notification) {
        final NodeRef ref = notification.getNodeRef();

        final UpdateType updateType;
        if (dataService.readOperationalData(ref.getValue()) == null) {
            updateType = UpdateType.ADDED;
        } else {
            updateType = UpdateType.CHANGED;
        }

        final Node aDNode;
        try {
            aDNode = NodeMapping.toADNode(ref);
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node for {}, not reporting the update", ref, e);
            return;
        }

        this.publishNodeUpdate(aDNode, updateType, NodeMapping.toADNodeProperties(notification));
        for (final IPluginOutReadService statsPublisher : getStatisticsPublisher()) {
            final NodeDescription description = this.toNodeDescription(ref);
            if (description != null) {
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef =
                        InstanceIdentifier.builder(Nodes.class)
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(notification.getId()))
                        .build();
                try {
                    statsPublisher.descriptionStatisticsUpdated(NodeMapping.toADNode(nodeRef), description);
                } catch (ConstructionException e) {
                    LOG.warn("Failed to construct node for {}, not reporting the update to publisher {}", nodeRef, statsPublisher, e);
                }
            }
        }
    }

    @Override
    public ConcurrentMap<Node,Map<String,Property>> getNodeProps() {
        final ConcurrentHashMap<Node,Map<String,Property>> props = new ConcurrentHashMap<>();
        final Nodes nodes = this.readOperAllMDNodes();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node : nodes.getNode()) {
            final FlowCapableNode fcn = node.getAugmentation(FlowCapableNode.class);
            if (fcn != null) {
                final ConcurrentHashMap<String,Property> perNodePropMap = new ConcurrentHashMap<String, Property>();
                final HashSet<Property> perNodeProps = NodeMapping.toADNodeProperties(fcn, node.getId());
                if (perNodeProps != null) {
                    for (final Property perNodeProp : perNodeProps) {
                        perNodePropMap.put(perNodeProp.getName(), perNodeProp);
                    }
                }

                try {
                    final Node adNode = NodeMapping.toADNode(node.getId());
                    props.put(adNode, perNodePropMap);
                } catch (ConstructionException e) {
                    LOG.warn("Failed to construct node for {}, skipping it", node, e);
                }
            }
        }
        return props;
    }

    private Nodes readOperAllMDNodes() {
        final TypeSafeDataReader reader = TypeSafeDataReader.forReader(getDataService());
        return reader.readOperationalData(InstanceIdentifier.builder(Nodes.class).build());
    }

    @Override
    public ConcurrentMap<NodeConnector,Map<String,Property>> getNodeConnectorProps(final Boolean refresh) {
        final ConcurrentHashMap<NodeConnector,Map<String,Property>> props = new ConcurrentHashMap<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node : this.readOperAllMDNodes().getNode()) {
            for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc : node.getNodeConnector()) {
                final FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
                if (fcnc != null) {
                    final ConcurrentHashMap<String,Property> ncpsm = new ConcurrentHashMap<>();
                    final HashSet<Property> ncps = NodeMapping.toADNodeConnectorProperties(fcnc);
                    if (ncps != null) {
                        for (final Property p : ncps) {
                            ncpsm.put(p.getName(), p);
                        }
                    }

                    try {
                        props.put(NodeMapping.toADNodeConnector(nc.getId(), node.getId()), ncpsm);
                    } catch (ConstructionException e) {
                        LOG.warn("Failed to instantiate node {} connector {}, not reporting it", node.getId(), nc.getId(), e);
                    }
                }
            }
        }
        return props;
    }

    private FlowCapableNodeConnector readOperFlowCapableNodeConnector(final NodeConnectorRef ref) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc =
                (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector)
                getDataService().readOperationalData(ref.getValue());
        return nc.getAugmentation(FlowCapableNodeConnector.class);
    }

    private static NodeConnectorStatistics toNodeConnectorStatistics(final org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.NodeConnectorStatistics nodeConnectorStatistics, final NodeId nodeId, final NodeConnectorId nodeConnectorId) throws ConstructionException {
        final NodeConnectorStatistics it = new NodeConnectorStatistics();

        final Packets packets = nodeConnectorStatistics.getPackets();
        it.setReceivePacketCount(packets.getReceived().longValue());
        it.setTransmitPacketCount(packets.getTransmitted().longValue());

        final Bytes bytes = nodeConnectorStatistics.getBytes();
        it.setReceiveByteCount(bytes.getReceived().longValue());
        it.setTransmitByteCount(bytes.getTransmitted().longValue());

        it.setReceiveDropCount(nodeConnectorStatistics.getReceiveDrops().longValue());
        it.setTransmitDropCount(nodeConnectorStatistics.getTransmitDrops().longValue());
        it.setReceiveErrorCount(nodeConnectorStatistics.getReceiveErrors().longValue());
        it.setTransmitErrorCount(nodeConnectorStatistics.getTransmitErrors().longValue());
        it.setReceiveFrameErrorCount(nodeConnectorStatistics.getReceiveFrameError().longValue());
        it.setReceiveOverRunErrorCount(nodeConnectorStatistics.getReceiveOverRunError().longValue());
        it.setReceiveCRCErrorCount(nodeConnectorStatistics.getReceiveCrcError().longValue());
        it.setCollisionCount(nodeConnectorStatistics.getCollisionCount().longValue());

        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> nodeConnectorRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(nodeId))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, new NodeConnectorKey(nodeConnectorId))
                .build();
        it.setNodeConnector(NodeMapping.toADNodeConnector(new NodeConnectorRef(nodeConnectorRef)));
        return it;
    }

    private static NodeTableStatistics toNodeTableStatistics(final FlowTableStatistics tableStats, final Short tableId, final Node node) throws ConstructionException {
        final NodeTableStatistics it = new NodeTableStatistics();
        it.setActiveCount(tableStats.getActiveFlows().getValue().intValue());
        it.setLookupCount(tableStats.getPacketsLookedUp().getValue().longValue());
        it.setMatchedCount(tableStats.getPacketsMatched().getValue().longValue());
        it.setName(tableId.toString());
        it.setNodeTable(new NodeTable(NodeTableIDType.OPENFLOW, tableId.byteValue(), node));
        return it;
    }

    private NodeDescription toNodeDescription(final NodeRef nodeRef) {
        final FlowCapableNode capableNode = this.readOperFlowCapableNode(nodeRef);
        if (capableNode == null) {
            return null;
        }

        final NodeDescription it = new NodeDescription();
        it.setManufacturer(capableNode.getManufacturer());
        it.setSerialNumber(capableNode.getSerialNumber());
        it.setSoftware(capableNode.getSoftware());
        it.setDescription(capableNode.getDescription());
        return it;
    }

    public Edge toADEdge(final Link link) throws ConstructionException {
        NodeConnectorRef _source = link.getSource();
        NodeConnector _aDNodeConnector = NodeMapping.toADNodeConnector(_source);
        NodeConnectorRef _destination = link.getDestination();
        NodeConnector _aDNodeConnector_1 = NodeMapping.toADNodeConnector(_destination);
        Edge _edge = new Edge(_aDNodeConnector, _aDNodeConnector_1);
        return _edge;
    }

    /**
     * OpendaylightFlowStatisticsListener interface implementation
     */
    @Override
    public void onAggregateFlowStatisticsUpdate(final AggregateFlowStatisticsUpdate notification) {
        // Ignoring this notification as there does not seem to be a way to bubble this up to AD-SAL
    }

    @Override
    public void onFlowsStatisticsUpdate(final FlowsStatisticsUpdate notification) {
        final ArrayList<FlowOnNode> adsalFlowsStatistics = new ArrayList<>();
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(notification.getId()))
                .build();

        final Node aDNode;
        try {
            aDNode = NodeMapping.toADNode(nodeRef);
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node for {}, ignoring it", notification.getId(), e);
            return;
        }

        for (final FlowAndStatisticsMapList flowStats : notification.getFlowAndStatisticsMapList()) {
            if (flowStats.getTableId() == 0) {
                adsalFlowsStatistics.add(InventoryAndReadAdapter.toFlowOnNode(flowStats, aDNode));
            }
        }
        for (final IPluginOutReadService statsPublisher : getStatisticsPublisher()) {
            statsPublisher.nodeFlowStatisticsUpdated(aDNode, adsalFlowsStatistics);
        }

        updateTransactionCache(notification, notification.getId(), !notification.isMoreReplies());
    }

    /**
     * OpendaylightFlowTableStatisticsListener interface implementation
     */
    @Override
    public void onFlowTableStatisticsUpdate(final FlowTableStatisticsUpdate notification) {
        ArrayList<NodeTableStatistics> adsalFlowTableStatistics = new ArrayList<>();
        for (final FlowTableAndStatisticsMap stats : notification.getFlowTableAndStatisticsMap()) {
            if (stats.getTableId().getValue() == 0) {
                final NodeTableStatistics it = new NodeTableStatistics();
                it.setActiveCount(stats.getActiveFlows().getValue().intValue());
                it.setLookupCount(stats.getPacketsLookedUp().getValue().longValue());
                it.setMatchedCount(stats.getPacketsMatched().getValue().longValue());
                adsalFlowTableStatistics.add(it);
            }
        }

        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(notification.getId()))
                .build();

        final Node aDNode;
        try {
            aDNode = NodeMapping.toADNode(nodeRef);
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node for {}, ignoring it", notification.getId(), e);
            return;
        }

        for (final IPluginOutReadService statsPublisher : getStatisticsPublisher()) {
            statsPublisher.nodeTableStatisticsUpdated(aDNode, adsalFlowTableStatistics);
        }
    }

    /**
     * OpendaylightPortStatisticsUpdate interface implementation
     */
    @Override
    public void onNodeConnectorStatisticsUpdate(final NodeConnectorStatisticsUpdate notification) {
        final ArrayList<NodeConnectorStatistics> adsalPortStatistics = new ArrayList<NodeConnectorStatistics>();
        for (final NodeConnectorStatisticsAndPortNumberMap nodeConnectorStatistics : notification.getNodeConnectorStatisticsAndPortNumberMap()) {
            try {
                adsalPortStatistics.add(toNodeConnectorStatistics(
                        nodeConnectorStatistics, notification.getId(), nodeConnectorStatistics.getNodeConnectorId()));
            } catch (ConstructionException e) {
                LOG.warn("Failed to create statistics for node {} connector {}, not updating them",
                        notification.getId(), nodeConnectorStatistics.getNodeConnectorId(), e);
            }
        }

        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeRef =
                InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, new NodeKey(notification.getId()))
                .build();

        final Node aDNode;
        try {
            aDNode = NodeMapping.toADNode(nodeRef);
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct node for {}, ignoring it", notification.getId(), e);
            return;
        }

        for (final IPluginOutReadService statsPublisher : getStatisticsPublisher()) {
            statsPublisher.nodeConnectorStatisticsUpdated(aDNode, adsalPortStatistics);
        }
    }

    private static FlowOnNode toFlowOnNode(final FlowAndStatisticsMapList flowAndStatsMap, final Node node) {
        final FlowOnNode it = new FlowOnNode(ToSalConversionsUtils.toFlow(flowAndStatsMap, node));
        return addFlowStats(it, flowAndStatsMap);
    }

    private static FlowOnNode addFlowStats(final FlowOnNode node, final GenericStatistics stats) {
        node.setByteCount(stats.getByteCount().getValue().longValue());
        node.setPacketCount(stats.getPacketCount().getValue().longValue());
        node.setDurationSeconds(stats.getDuration().getSecond().getValue().intValue());
        node.setDurationNanoseconds(stats.getDuration().getNanosecond().getValue().intValue());
        return node;
    }

    @Override
    public Set<Node> getConfiguredNotConnectedNodes() {
        return Collections.emptySet();
    }

    private void publishNodeUpdate(final Node node, final UpdateType updateType, final Set<Property> properties) {
        for (final IPluginOutInventoryService publisher : getInventoryPublisher()) {
            publisher.updateNode(node, updateType, properties);
        }
    }

    private void publishNodeConnectorUpdate(final NodeConnector nodeConnector, final UpdateType updateType, final Set<Property> properties) {
        for (final IPluginOutInventoryService publisher : getInventoryPublisher()) {
            publisher.updateNodeConnector(nodeConnector, updateType, properties);
        }
    }

    private boolean isKnownNodeConnector(final InstanceIdentifier<? extends Object> nodeConnectorIdentifier) {
        final Iterator<PathArgument> it = nodeConnectorIdentifier.getPathArguments().iterator();

        if (!it.hasNext()) {
            return false;
        }
        it.next();

        if (!it.hasNext()) {
            return false;
        }
        final PathArgument nodePath = it.next();

        if (!it.hasNext()) {
            return false;
        }
        final PathArgument nodeConnectorPath = it.next();

        final List<PathArgument> nodeConnectors = nodeToNodeConnectorsMap.get(nodePath);
        return nodeConnectors == null ? false :
            nodeConnectors.contains(nodeConnectorPath);
    }

    private boolean recordNodeConnector(final InstanceIdentifier<? extends Object> nodeConnectorIdentifier) {
        final Iterator<PathArgument> it = nodeConnectorIdentifier.getPathArguments().iterator();

        if (!it.hasNext()) {
            return false;
        }
        it.next();

        if (!it.hasNext()) {
            return false;
        }
        final PathArgument nodePath = it.next();

        if (!it.hasNext()) {
            return false;
        }
        final PathArgument nodeConnectorPath = it.next();

        synchronized (this) {
            List<PathArgument> nodeConnectors = this.nodeToNodeConnectorsMap.get(nodePath);
            if (nodeConnectors == null) {
                nodeConnectors = new ArrayList<>();
                this.nodeToNodeConnectorsMap.put(nodePath, nodeConnectors);
            }

            return nodeConnectors.add(nodeConnectorPath);
        }
    }

    private List<PathArgument> removeNodeConnectors(final InstanceIdentifier<? extends Object> nodeIdentifier) {
        return this.nodeToNodeConnectorsMap.remove(Iterables.get(nodeIdentifier.getPathArguments(), 1));
    }

    private <T extends TransactionAware> void updateTransactionCache(T notification, NodeId nodeId, boolean lastNotification) {

        String cacheKey = buildCacheKey(notification.getTransactionId(), nodeId);
        TransactionNotificationList<T> txnList = (TransactionNotificationList<T>) txCache.getIfPresent(cacheKey);
        final Optional<TransactionNotificationList<T>> optional = Optional.<TransactionNotificationList<T>>fromNullable(txnList);
        if (optional.isPresent()) {
            LOG.info("updateTransactionCache cacheKey:{}, lastNotification:{}, txnList-present:{}", cacheKey, lastNotification, optional.isPresent());
            TransactionNotificationList<T> txn = optional.get();
            txn.addNotification(notification);
            txn.setAllNotificationsGathered(lastNotification);
        }
    }

    private class TransactionNotificationList<T extends TransactionAware> {
        private TransactionId id;
        private String nId;
        private List<T> notifications;
        private boolean allNotificationsGathered;

        public TransactionNotificationList(TransactionId id, String nId) {
            this.nId = nId;
            this.id = id;
            notifications = new ArrayList<T>();
        }

        public void addNotification(T notification) {
            notifications.add(notification);
        }

        public void setAllNotificationsGathered(boolean allNotificationsGathered) {
            this.allNotificationsGathered = allNotificationsGathered;
        }

        public boolean areAllNotificationsGathered() {
            return allNotificationsGathered;
        }

        public List<T> getNotifications() {
            return notifications;
        }

    }

}
