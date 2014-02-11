/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility

import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.CopyOnWriteArrayList;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeTable
import org.opendaylight.controller.sal.core.UpdateType
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService
import org.opendaylight.controller.sal.reader.FlowOnNode
import org.opendaylight.controller.sal.reader.IPluginInReadService
import org.opendaylight.controller.sal.reader.IPluginOutReadService
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics
import org.opendaylight.controller.sal.reader.NodeDescription
import org.opendaylight.controller.sal.reader.NodeTableStatistics
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatistics
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory

import static extension org.opendaylight.controller.sal.common.util.Arguments.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import java.util.concurrent.ConcurrentHashMap
import java.util.Map
import java.util.HashMap

class InventoryAndReadAdapter implements IPluginInReadService,
                                             IPluginInInventoryService,
                                             OpendaylightInventoryListener,
                                             OpendaylightFlowStatisticsListener,
                                             OpendaylightFlowTableStatisticsListener,
                                             OpendaylightPortStatisticsListener {

    private static val LOG = LoggerFactory.getLogger(InventoryAndReadAdapter);

    private static val OPENFLOWV10_TABLE_ID = new Integer(0).shortValue;
    @Property
    DataBrokerService dataService;

    @Property
    DataProviderService dataProviderService;

    @Property
    OpendaylightFlowStatisticsService flowStatisticsService;

    @Property
    OpendaylightPortStatisticsService nodeConnectorStatisticsService;
    
    @Property
    OpendaylightFlowTableStatisticsService flowTableStatisticsService;

    @Property
    FlowTopologyDiscoveryService topologyDiscovery;
    
    @Property
    List<IPluginOutReadService> statisticsPublisher = new CopyOnWriteArrayList<IPluginOutReadService>();

    @Property
    List<IPluginOutInventoryService> inventoryPublisher = new CopyOnWriteArrayList<IPluginOutInventoryService>();

    private final InventoryNotificationProvider inventoryNotificationProvider = new InventoryNotificationProvider();

    private final Map<InstanceIdentifier.PathArgument, List<InstanceIdentifier.PathArgument>> nodeToNodeConnectorsMap = new ConcurrentHashMap<InstanceIdentifier.PathArgument, List<InstanceIdentifier.PathArgument>>();

    private final Lock nodeToNodeConnectorsLock = new ReentrantLock();


    def start(){
        inventoryNotificationProvider.dataProviderService = dataProviderService;
        inventoryNotificationProvider.inventoryPublisher = inventoryPublisher;
        // inventoryNotificationProvider.start();
    }

    def setInventoryPublisher(IPluginOutInventoryService listener){
        inventoryPublisher.add(listener);
    }

    def unsetInventoryPublisher(IPluginOutInventoryService listener){
        inventoryPublisher.remove(listener);
    }

    def setReadPublisher(IPluginOutReadService listener) {
        statisticsPublisher.add(listener);
    }
    
    def unsetReadPublisher (IPluginOutReadService listener) {
        if( listener != null)
            statisticsPublisher.remove(listener);
    }

    protected def startChange() {
        return dataProviderService.beginTransaction;
    }

    override getTransmitRate(org.opendaylight.controller.sal.core.NodeConnector connector) {
        val nodeConnector = readFlowCapableNodeConnector(connector.toNodeConnectorRef);
        return nodeConnector.currentSpeed
    }

    override readAllFlow(Node node, boolean cached) {

        val output = new ArrayList<FlowOnNode>();
        val tableRef = InstanceIdentifier.builder(Nodes)
                                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(node))
                                        .augmentation(FlowCapableNode).child(Table, new TableKey(OPENFLOWV10_TABLE_ID)).toInstance();
        
        val it = this.startChange();
        
        val table= it.readConfigurationData(tableRef) as Table;
        
        if(table != null){
            LOG.trace("Number of flows installed in table 0 of node {} : {}",node,table.flow.size);
            
            for(flow : table.flow){
                
                val adsalFlow = ToSalConversionsUtils.toFlow(flow,node);
                val statsFromDataStore = flow.getAugmentation(FlowStatisticsData);
                
                if(statsFromDataStore != null){
                    val it = new FlowOnNode(adsalFlow);
                    byteCount =  statsFromDataStore.flowStatistics.byteCount.value.longValue;
                    packetCount = statsFromDataStore.flowStatistics.packetCount.value.longValue;
                    durationSeconds = statsFromDataStore.flowStatistics.duration.second.value.intValue;
                    durationNanoseconds = statsFromDataStore.flowStatistics.duration.nanosecond.value.intValue;
                    
                    output.add(it);
                }
            }
        }
        
        //TODO (main): Shell we send request to the switch? It will make async request to the switch.
        // Once plugin receive response, it will let adaptor know through onFlowStatisticsUpdate()
        // If we assume that md-sal statistics manager will always be running, then its not required
        // But if not, then sending request will collect the latest data for adaptor atleast.
        val input = new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
        input.setNode(node.toNodeRef);
        flowStatisticsService.getAllFlowsStatisticsFromAllFlowTables(input.build)
        
        return output;
    }

    override readAllNodeConnector(Node node, boolean cached) {
        
        val ret = new ArrayList<NodeConnectorStatistics>();
        val nodeRef = InstanceIdentifier.builder(Nodes)
                                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(node))
                                    .toInstance();
        
        val provider = this.startChange();
        
        val dsNode= provider.readConfigurationData(nodeRef) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
        
         if(dsNode != null){
             
             for (dsNodeConnector : dsNode.nodeConnector){
                val nodeConnectorRef = InstanceIdentifier.builder(Nodes)
                                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(node))
                                    .child(NodeConnector, dsNodeConnector.key)
                                    .toInstance();
                 
                 val nodeConnectorFromDS = provider.readConfigurationData(nodeConnectorRef) as NodeConnector;
                 
                 if(nodeConnectorFromDS != null){
                     val nodeConnectorStatsFromDs = nodeConnectorFromDS.getAugmentation(FlowCapableNodeConnectorStatisticsData) as FlowCapableNodeConnectorStatistics;
                     
                    ret.add(toNodeConnectorStatistics(nodeConnectorStatsFromDs.flowCapableNodeConnectorStatistics,dsNode.id,dsNodeConnector.id));
                 }
             }
         }

        //TODO: Refer TODO (main)
        val input = new GetAllNodeConnectorsStatisticsInputBuilder();
        input.setNode(node.toNodeRef);
        nodeConnectorStatisticsService.getAllNodeConnectorsStatistics(input.build());
        return ret;
    }

    override readAllNodeTable(Node node, boolean cached) {
        val ret = new ArrayList<NodeTableStatistics>();
        
        val dsFlowCapableNode= readFlowCapableNode(node.toNodeRef)
        
         if(dsFlowCapableNode != null){
             
             for (table : dsFlowCapableNode.table){
                 
                 val tableStats = table.getAugmentation(FlowTableStatisticsData);
                 
                 if(tableStats != null){
                     ret.add(toNodeTableStatistics(tableStats.flowTableStatistics,table.id,node));
                 }
             }
         }

        //TODO: Refer TODO (main)
        val input = new GetFlowTablesStatisticsInputBuilder();
        input.setNode(node.toNodeRef);
        flowTableStatisticsService.getFlowTablesStatistics(input.build);
        return ret;
    }

    override readDescription(Node node, boolean cached) {
        return toNodeDescription(node.toNodeRef);
    }

    override readFlow(Node node, Flow targetFlow, boolean cached) {
        var FlowOnNode ret= null;
        
        val tableRef = InstanceIdentifier.builder(Nodes)
                                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(node))
                                        .augmentation(FlowCapableNode).child(Table, new TableKey(OPENFLOWV10_TABLE_ID)).toInstance();
        
        val it = this.startChange();
        
        val table= it.readConfigurationData(tableRef) as Table;
        
        if(table != null){
            LOG.trace("Number of flows installed in table 0 of node {} : {}",node,table.flow.size);
            
            for(mdsalFlow : table.flow){
                if(FromSalConversionsUtils.flowEquals(mdsalFlow, MDFlowMapping.toMDSalflow(targetFlow))){
                    val statsFromDataStore = mdsalFlow.getAugmentation(FlowStatisticsData);
                    
                    if(statsFromDataStore != null){
                        LOG.debug("Found matching flow in the data store flow table ");
                        val it = new FlowOnNode(targetFlow);
                        byteCount =  statsFromDataStore.flowStatistics.byteCount.value.longValue;
                        packetCount = statsFromDataStore.flowStatistics.packetCount.value.longValue;
                        durationSeconds = statsFromDataStore.flowStatistics.duration.second.value.intValue;
                        durationNanoseconds = statsFromDataStore.flowStatistics.duration.nanosecond.value.intValue;
                        
                        ret = it;
                    }
                }            
            }
        }
        
        //TODO: Refer TODO (main)
        val input = new GetFlowStatisticsFromFlowTableInputBuilder;
        input.setNode(node.toNodeRef);
        input.fieldsFrom(MDFlowMapping.toMDSalflow(targetFlow));
        flowStatisticsService.getFlowStatisticsFromFlowTable(input.build)
        
        return ret;
        
    }

    override readNodeConnector(org.opendaylight.controller.sal.core.NodeConnector connector, boolean cached) {
        var NodeConnectorStatistics  nodeConnectorStatistics = null;
    
        val nodeConnectorRef = InstanceIdentifier.builder(Nodes)
                                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(connector.node))
                                    .child(NodeConnector, InventoryMapping.toNodeConnectorKey(connector))
                                    .toInstance();
         val provider = this.startChange();
                 
         val nodeConnectorFromDS = provider.readConfigurationData(nodeConnectorRef) as NodeConnector;
                 
         if(nodeConnectorFromDS != null){
            val nodeConnectorStatsFromDs = nodeConnectorFromDS.getAugmentation(FlowCapableNodeConnectorStatisticsData) as FlowCapableNodeConnectorStatistics;
            if(nodeConnectorStatsFromDs != null) {
                nodeConnectorStatistics = toNodeConnectorStatistics(nodeConnectorStatsFromDs.flowCapableNodeConnectorStatistics,
                                                                        InventoryMapping.toNodeKey(connector.node).id,
                                                                        InventoryMapping.toNodeConnectorKey(connector).id);
            }
        }

        //TODO: Refer TODO (main)
        val input = new GetNodeConnectorStatisticsInputBuilder();
        input.setNode(connector.node.toNodeRef);
        input.setNodeConnectorId(InventoryMapping.toNodeConnectorKey(connector).id);
        nodeConnectorStatisticsService.getNodeConnectorStatistics(input.build());
        return nodeConnectorStatistics;
    }

    override readNodeTable(NodeTable nodeTable, boolean cached) {
        var NodeTableStatistics nodeStats = null
        
        val tableRef = InstanceIdentifier.builder(Nodes)
                                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(nodeTable.node))
                                        .augmentation(FlowCapableNode).child(Table, new TableKey(nodeTable.ID as Short)).toInstance();
        
        val it = this.startChange();
        
        val table= it.readConfigurationData(tableRef) as Table;
        
        if(table != null){
            val tableStats = table.getAugmentation(FlowTableStatisticsData);
                 
             if(tableStats != null){
                 nodeStats =  toNodeTableStatistics(tableStats.flowTableStatistics,table.id,nodeTable.node);
            }
        }

        //TODO: Refer TODO (main)
        val input = new GetFlowTablesStatisticsInputBuilder();
        input.setNode(nodeTable.node.toNodeRef);
        flowTableStatisticsService.getFlowTablesStatistics(input.build);
        
        return nodeStats;
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved update) {
        // Never received
    }

    override onNodeRemoved(NodeRemoved notification) {
        val properties = Collections.<org.opendaylight.controller.sal.core.Property>emptySet();

        removeNodeConnectors(notification.nodeRef.value);

        publishNodeUpdate(notification.nodeRef.toADNode, UpdateType.REMOVED, properties);
    }

    override onNodeConnectorUpdated(NodeConnectorUpdated update) {
        var updateType = UpdateType.CHANGED;
        if(!isKnownNodeConnector(update.nodeConnectorRef.value)){
            updateType = UpdateType.ADDED;
            recordNodeConnector(update.nodeConnectorRef.value);
        }

        var nodeConnector = update.nodeConnectorRef.toADNodeConnector

        publishNodeConnectorUpdate(nodeConnector , updateType , update.toADNodeConnectorProperties);
    }

    override onNodeUpdated(NodeUpdated notification) {
        val InstanceIdentifier<? extends DataObject> identifier = notification.nodeRef.value  as InstanceIdentifier<? extends DataObject>;

        var updateType = UpdateType.CHANGED;
        if ( this._dataService.readOperationalData(identifier) == null ){
            updateType = UpdateType.ADDED;
        }
        publishNodeUpdate(notification.nodeRef.toADNode, updateType, notification.toADNodeProperties);

        //Notify the listeners of IPluginOutReadService

        for (statsPublisher : statisticsPublisher){
            val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
            val description = notification.nodeRef.toNodeDescription
            if(description != null) {
              statsPublisher.descriptionStatisticsUpdated(nodeRef.toADNode,description);
            }
        }
    }

    override getNodeProps() {
        val props = new ConcurrentHashMap<Node, Map<String, org.opendaylight.controller.sal.core.Property>>()
        
        val nodes = readAllMDNodes()
        for (node : nodes.node ) {
            val fcn = node.getAugmentation(FlowCapableNode)
            if(fcn != null) {
                val perNodeProps = fcn.toADNodeProperties(node.id)
                val perNodePropMap = new ConcurrentHashMap<String, org.opendaylight.controller.sal.core.Property>
                if(perNodeProps != null ) {
                    for(perNodeProp : perNodeProps) {
                        perNodePropMap.put(perNodeProp.name,perNodeProp)
                    }
                }
                props.put(new Node(MD_SAL_TYPE, node.id.toADNodeId),perNodePropMap)
            }
        }
        return props;
    }
    
    private def readAllMDNodes() {
        val nodesRef = InstanceIdentifier.builder(Nodes)
            .toInstance
        val reader = TypeSafeDataReader.forReader(dataService)
        return reader.readOperationalData(nodesRef)
    }
    
    private def readAllMDNodeConnectors(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node) {
        val nodeRef = InstanceIdentifier.builder(Nodes)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(node.id))
            .toInstance
        val reader = TypeSafeDataReader.forReader(dataService)
        return reader.readOperationalData(nodeRef).nodeConnector
    }

    override getNodeConnectorProps(Boolean refresh) {
        // Note, because the MD-SAL has a unified data store, we can ignore the Boolean refresh, as we have no secondary 
        // data store to refresh from
        val props = new ConcurrentHashMap<org.opendaylight.controller.sal.core.NodeConnector, Map<String, org.opendaylight.controller.sal.core.Property>>()
        val nodes = readAllMDNodes()
        for (node : nodes.node) {
            val ncs = node.readAllMDNodeConnectors
            if(ncs != null) {
                for( nc : ncs ) {
                    val fcnc = nc.getAugmentation(FlowCapableNodeConnector)
                    if(fcnc != null) {
                        val ncps = fcnc.toADNodeConnectorProperties
                        val ncpsm = new ConcurrentHashMap<String, org.opendaylight.controller.sal.core.Property>
                        if(ncps != null) {
                            for(p : ncps) {
                                ncpsm.put(p.name,p)
                            }
                        }  
                        props.put(nc.id.toADNodeConnector(node.id),ncpsm)
                    }
                }
            }
        }
        return props
    }

    private def FlowCapableNode readFlowCapableNode(NodeRef ref) {
        val dataObject = dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>);
        if(dataObject != null) {
            val node = dataObject.checkInstanceOf(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node);
            return node.getAugmentation(FlowCapableNode);
        }
        return null;
    }

    private def FlowCapableNodeConnector readFlowCapableNodeConnector(NodeConnectorRef ref) {
        val dataObject = dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>);
        val node = dataObject.checkInstanceOf(
            NodeConnector);
        return node.getAugmentation(FlowCapableNodeConnector);
    }

    private def toNodeConnectorStatistics(
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.NodeConnectorStatistics nodeConnectorStatistics, NodeId nodeId, NodeConnectorId nodeConnectorId) {
            
            val it = new NodeConnectorStatistics();
            
            receivePacketCount = nodeConnectorStatistics.packets.received.longValue;
            transmitPacketCount = nodeConnectorStatistics.packets.transmitted.longValue;
            
            receiveByteCount = nodeConnectorStatistics.bytes.received.longValue;
            transmitByteCount = nodeConnectorStatistics.bytes.transmitted.longValue;
            
            receiveDropCount = nodeConnectorStatistics.receiveDrops.longValue;
            transmitDropCount = nodeConnectorStatistics.transmitDrops.longValue;
            
            receiveErrorCount = nodeConnectorStatistics.receiveErrors.longValue;
            transmitErrorCount = nodeConnectorStatistics.transmitErrors.longValue;
            
            receiveFrameErrorCount = nodeConnectorStatistics.receiveFrameError.longValue;
            receiveOverRunErrorCount = nodeConnectorStatistics.receiveOverRunError.longValue;
            receiveCRCErrorCount = nodeConnectorStatistics.receiveCrcError.longValue;
            collisionCount = nodeConnectorStatistics.collisionCount.longValue;
            
            val nodeConnectorRef = InstanceIdentifier.builder(Nodes)
                                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(nodeId))
                                .child(NodeConnector,new NodeConnectorKey(nodeConnectorId)).toInstance;
            
            nodeConnector = NodeMapping.toADNodeConnector(new NodeConnectorRef(nodeConnectorRef));
            
            return it;
    }

    private def toNodeTableStatistics(
        FlowTableStatistics tableStats,
        Short tableId,Node node){
        var it = new NodeTableStatistics();
        
        activeCount = tableStats.activeFlows.value.intValue;
        lookupCount = tableStats.packetsLookedUp.value.intValue;
        matchedCount = tableStats.packetsMatched.value.intValue;
        name = tableId.toString;
        nodeTable = new NodeTable(NodeMapping.MD_SAL_TYPE,tableId,node);
        return it;
    }
    
    private def toNodeDescription(NodeRef nodeRef){
        val capableNode = readFlowCapableNode(nodeRef);
        if(capableNode !=null) {
            val it = new NodeDescription()
            manufacturer = capableNode.manufacturer
            serialNumber = capableNode.serialNumber
            software = capableNode.software
            description = capableNode.description
            
            return it;
         }
         return null;
    }
    
    
    def Edge toADEdge(Link link) {
        new Edge(link.source.toADNodeConnector,link.destination.toADNodeConnector)
    }
    
    /*
     * OpendaylightFlowStatisticsListener interface implementation
     */
    override onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
        //Ignoring this notification as there does not seem to be a way to bubble this up to AD-SAL
    }
    
    override onFlowsStatisticsUpdate(FlowsStatisticsUpdate notification) {
        
        val adsalFlowsStatistics = new ArrayList<FlowOnNode>();
        val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
        
        for(flowStats : notification.flowAndStatisticsMapList){
            if(flowStats.tableId == 0)
                adsalFlowsStatistics.add(toFlowOnNode(flowStats,nodeRef.toADNode));
        }
        
        for (statsPublisher : statisticsPublisher){
            statsPublisher.nodeFlowStatisticsUpdated(nodeRef.toADNode,adsalFlowsStatistics);
        }
        
    }
    /*
     * OpendaylightFlowTableStatisticsListener interface implementation
     */    
    override onFlowTableStatisticsUpdate(FlowTableStatisticsUpdate notification) {
        var adsalFlowTableStatistics = new ArrayList<NodeTableStatistics>();
        
        for(stats : notification.flowTableAndStatisticsMap){
            if (stats.tableId.value == 0){
                val it = new NodeTableStatistics();
                activeCount = stats.activeFlows.value.intValue;
                lookupCount = stats.packetsLookedUp.value.longValue;
                matchedCount = stats.packetsMatched.value.longValue;
                
                adsalFlowTableStatistics.add(it);
            }
        }
        for (statsPublisher : statisticsPublisher){
            val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
            statsPublisher.nodeTableStatisticsUpdated(nodeRef.toADNode,adsalFlowTableStatistics);
        }
    }
    
    /*
     * OpendaylightPortStatisticsUpdate interface implementation
     */
    override onNodeConnectorStatisticsUpdate(NodeConnectorStatisticsUpdate notification) {
        
        val adsalPortStatistics  = new ArrayList<NodeConnectorStatistics>();
        
        for(nodeConnectorStatistics : notification.nodeConnectorStatisticsAndPortNumberMap){
            adsalPortStatistics.add(toNodeConnectorStatistics(nodeConnectorStatistics,notification.id,nodeConnectorStatistics.nodeConnectorId));
        }
        
        for (statsPublisher : statisticsPublisher){
            val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
            statsPublisher.nodeConnectorStatisticsUpdated(nodeRef.toADNode,adsalPortStatistics);
        }
        
    }
    
    private static def toFlowOnNode (FlowAndStatisticsMapList flowAndStatsMap,Node node){
        
        val it = new FlowOnNode(ToSalConversionsUtils.toFlow(flowAndStatsMap,node));
        
        byteCount = flowAndStatsMap.byteCount.value.longValue;
        packetCount = flowAndStatsMap.packetCount.value.longValue;
        durationSeconds = flowAndStatsMap.duration.second.value.intValue;
        durationNanoseconds = flowAndStatsMap.duration.nanosecond.value.intValue;
        
        return it;
    }

    override  getConfiguredNotConnectedNodes() {
        return Collections.emptySet();
    }


    private def publishNodeUpdate(Node node, UpdateType updateType, Set<org.opendaylight.controller.sal.core.Property> properties){
        for( publisher : inventoryPublisher){
            publisher.updateNode(node, updateType, properties);
        }
    }

    private def publishNodeConnectorUpdate(org.opendaylight.controller.sal.core.NodeConnector nodeConnector, UpdateType updateType, Set<org.opendaylight.controller.sal.core.Property> properties){
        for( publisher : inventoryPublisher){
            publisher.updateNodeConnector(nodeConnector, updateType, properties);
        }
    }

    private def isKnownNodeConnector(InstanceIdentifier<? extends Object> nodeConnectorIdentifier){
        if(nodeConnectorIdentifier.path.size() < 3) {
            return false;
        }

        val nodePath = nodeConnectorIdentifier.path.get(1);
        val nodeConnectorPath = nodeConnectorIdentifier.getPath().get(2);

        val nodeConnectors = nodeToNodeConnectorsMap.get(nodePath);

        if(nodeConnectors == null){
            return false;
        }
        return nodeConnectors.contains(nodeConnectorPath);
    }


    private def recordNodeConnector(InstanceIdentifier<? extends Object> nodeConnectorIdentifier){
        if(nodeConnectorIdentifier.path.size() < 3) {
            return false;
        }

        val nodePath = nodeConnectorIdentifier.path.get(1);
        val nodeConnectorPath = nodeConnectorIdentifier.getPath().get(2);

        nodeToNodeConnectorsLock.lock();

        try {
            var nodeConnectors = nodeToNodeConnectorsMap.get(nodePath);

            if(nodeConnectors == null){
                nodeConnectors = new ArrayList<InstanceIdentifier.PathArgument>();
                nodeToNodeConnectorsMap.put(nodePath, nodeConnectors);
            }

            nodeConnectors.add(nodeConnectorPath);
        } finally {
            nodeToNodeConnectorsLock.unlock();
        }
    }

    private def removeNodeConnectors(InstanceIdentifier<? extends Object> nodeIdentifier){
        val nodePath = nodeIdentifier.path.get(1);

        nodeToNodeConnectorsMap.remove(nodePath);
    }
}
