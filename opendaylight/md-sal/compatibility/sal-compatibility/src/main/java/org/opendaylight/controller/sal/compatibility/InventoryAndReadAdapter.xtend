package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.reader.IPluginInReadService
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.controller.sal.core.NodeTable
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService

import static extension org.opendaylight.controller.sal.common.util.Arguments.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics
import org.opendaylight.controller.sal.reader.FlowOnNode
import org.opendaylight.controller.sal.reader.NodeDescription
import org.slf4j.LoggerFactory
import java.util.ArrayList
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import java.util.Collections
import org.opendaylight.controller.sal.core.UpdateType
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.controller.sal.topology.IPluginInTopologyService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.controller.sal.discovery.IDiscoveryService
import org.opendaylight.controller.sal.reader.IPluginOutReadService
import java.util.List
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate
import org.opendaylight.controller.sal.reader.NodeTableStatistics
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatistics
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder

class InventoryAndReadAdapter implements IPluginInTopologyService,
											 IPluginInReadService,
											 IPluginInInventoryService,
											 OpendaylightInventoryListener,
											 FlowTopologyDiscoveryListener,
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
    IPluginOutInventoryService inventoryPublisher;

    @Property
    IPluginOutTopologyService topologyPublisher;
    
    @Property
    IDiscoveryService discoveryPublisher;

    @Property
    FlowTopologyDiscoveryService topologyDiscovery;
    
    @Property
    List<IPluginOutReadService> statisticsPublisher = new ArrayList<IPluginOutReadService>();
	
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

    override getTransmitRate(NodeConnector connector) {
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
			LOG.info("Number of flows installed in table 0 of node {} : {}",node,table.flow.size);
			
			for(flow : table.flow){
				
				val adsalFlow = ToSalConversionsUtils.toFlow(flow);
				val statsFromDataStore = flow.getAugmentation(FlowStatisticsData) as FlowStatisticsData;
				
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
									.child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, dsNodeConnector.key)
									.toInstance();
 				
 				val nodeConnectorFromDS = provider.readConfigurationData(nodeConnectorRef) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
 				
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
 				
 				val tableStats = table.getAugmentation(FlowTableStatisticsData) as FlowTableStatisticsData;
 				
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
			LOG.info("Number of flows installed in table 0 of node {} : {}",node,table.flow.size);
			
			for(mdsalFlow : table.flow){
				if(FromSalConversionsUtils.flowEquals(mdsalFlow, MDFlowMapping.toMDSalflow(targetFlow))){
					val statsFromDataStore = mdsalFlow.getAugmentation(FlowStatisticsData) as FlowStatisticsData;
					
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

    override readNodeConnector(NodeConnector connector, boolean cached) {
    	var NodeConnectorStatistics  nodeConnectorStatistics = null;
	
		val nodeConnectorRef = InstanceIdentifier.builder(Nodes)
									.child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node, InventoryMapping.toNodeKey(connector.node))
									.child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, InventoryMapping.toNodeConnectorKey(connector))
									.toInstance();
 		val provider = this.startChange();
 				
 		val nodeConnectorFromDS = provider.readConfigurationData(nodeConnectorRef) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
 				
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
			val tableStats = table.getAugmentation(FlowTableStatisticsData) as FlowTableStatisticsData;
 				
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
        // NOOP
    }

    override onNodeRemoved(NodeRemoved notification) {
        val properties = Collections.<org.opendaylight.controller.sal.core.Property>emptySet();

        inventoryPublisher.updateNode(notification.nodeRef.toADNode, UpdateType.REMOVED, properties);
    }

    override onNodeConnectorUpdated(NodeConnectorUpdated update) {
        val properties = new java.util.HashSet<org.opendaylight.controller.sal.core.Property>();


        val org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> identifier = update.nodeConnectorRef.value as org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>;
        var updateType = UpdateType.CHANGED;
        if ( this._dataService.readOperationalData(identifier) == null ){
            updateType = UpdateType.ADDED;
        }

        var nodeConnector = update.nodeConnectorRef.toADNodeConnector


        properties.add(new org.opendaylight.controller.sal.core.Name(nodeConnector.ID.toString()));

        inventoryPublisher.updateNodeConnector(nodeConnector , updateType , properties);
    }

    override onNodeUpdated(NodeUpdated notification) {
        val properties = Collections.<org.opendaylight.controller.sal.core.Property>emptySet();
        val org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> identifier = notification.nodeRef.value  as org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>;

        var updateType = UpdateType.CHANGED;
        if ( this._dataService.readOperationalData(identifier) == null ){
            updateType = UpdateType.ADDED;
        }
        inventoryPublisher.updateNode(notification.nodeRef.toADNode, updateType, properties);
        
		//Notify the listeners of IPluginOutReadService
        
        for (statsPublisher : statisticsPublisher){
			val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
			statsPublisher.descriptionStatisticsUpdated(nodeRef.toADNode,toNodeDescription(notification.nodeRef));
		}
    }

    override getNodeProps() {

        // FIXME: Read from MD-SAL inventory service
        return null;
    }

    override getNodeConnectorProps(Boolean refresh) {

        // FIXME: Read from MD-SAL Invcentory Service
        return null;
    }

    private def FlowCapableNode readFlowCapableNode(NodeRef ref) {
        val dataObject = dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>);
        val node = dataObject.checkInstanceOf(
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node);
        return node.getAugmentation(FlowCapableNode);
    }

    private def FlowCapableNodeConnector readFlowCapableNodeConnector(NodeConnectorRef ref) {
        val dataObject = dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>);
        val node = dataObject.checkInstanceOf(
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector);
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
								.child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector,new NodeConnectorKey(nodeConnectorId)).toInstance;
			
			nodeConnector = NodeMapping.toADNodeConnector(new NodeConnectorRef(nodeConnectorRef));
			
			return it;
    }

	private def toNodeTableStatistics(
		org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics tableStats,
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

        val it = new NodeDescription()
        manufacturer = capableNode.manufacturer
        serialNumber = capableNode.serialNumber
        software = capableNode.software
        description = capableNode.description
        
        return it;
	}

    override sollicitRefresh() {
        topologyDiscovery.solicitRefresh
    }
    
    override onLinkDiscovered(LinkDiscovered notification) {
        val update = new TopoEdgeUpdate(notification.toADEdge,Collections.emptySet(),UpdateType.ADDED);
        discoveryPublisher.notifyEdge(notification.toADEdge,UpdateType.ADDED,Collections.emptySet());
        topologyPublisher.edgeUpdate(Collections.singletonList(update))
    }
    
    override onLinkOverutilized(LinkOverutilized notification) {
        topologyPublisher.edgeOverUtilized(notification.toADEdge)
    }
    
    override onLinkRemoved(LinkRemoved notification) {
        val update = new TopoEdgeUpdate(notification.toADEdge,Collections.emptySet(),UpdateType.REMOVED);
        topologyPublisher.edgeUpdate(Collections.singletonList(update))
    }
    
    override onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        topologyPublisher.edgeUtilBackToNormal(notification.toADEdge)
    }
    
    
    def Edge toADEdge(Link link) {
        new Edge(link.source.toADNodeConnector,link.destination.toADNodeConnector)
    }
	
	/*
	 * OpendaylightFlowStatisticsListener interface implementation
	 */
	override onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	override onFlowsStatisticsUpdate(FlowsStatisticsUpdate notification) {
		
		val adsalFlowsStatistics = new ArrayList<FlowOnNode>();
		
		for(flowStats : notification.flowAndStatisticsMapList){
			if(flowStats.tableId == 0)
				adsalFlowsStatistics.add(toFlowOnNode(flowStats));
		}
		
		for (statsPublisher : statisticsPublisher){
			val nodeRef = InstanceIdentifier.builder(Nodes).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node,new NodeKey(notification.id)).toInstance;
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
	
	private static def toFlowOnNode (FlowAndStatisticsMapList flowAndStatsMap){
		
		val it = new FlowOnNode(ToSalConversionsUtils.toFlow(flowAndStatsMap));
		
		byteCount = flowAndStatsMap.byteCount.value.longValue;
		packetCount = flowAndStatsMap.packetCount.value.longValue;
		durationSeconds = flowAndStatsMap.duration.second.value.intValue;
		durationNanoseconds = flowAndStatsMap.duration.nanosecond.value.intValue;
		
		return it;
	}
}
