package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.reader.IPluginInReadService
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.flowprogrammer.Flow
import org.opendaylight.controller.sal.core.NodeTable
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService

import static extension org.opendaylight.controller.sal.common.util.Arguments.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import static org.opendaylight.controller.sal.compatibility.MDFlowMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics
import org.opendaylight.controller.sal.reader.FlowOnNode
import org.opendaylight.controller.sal.reader.NodeDescription
import org.slf4j.LoggerFactory
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsInputBuilder
import java.util.ArrayList
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllNodeConnectorStatisticsInputBuilder
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

class InventoryAndReadAdapter implements IPluginInTopologyService, IPluginInReadService, IPluginInInventoryService, OpendaylightInventoryListener, FlowTopologyDiscoveryListener {

    private static val LOG = LoggerFactory.getLogger(InventoryAndReadAdapter);

    @Property
    DataBrokerService dataService;

    @Property
    OpendaylightFlowStatisticsService flowStatisticsService;

    @Property
    IPluginOutInventoryService inventoryPublisher;

    @Property
    IPluginOutTopologyService topologyPublisher;
    
    @Property
    IDiscoveryService discoveryPublisher;

    @Property
    FlowTopologyDiscoveryService topologyDiscovery;

    override getTransmitRate(NodeConnector connector) {
        val nodeConnector = readFlowCapableNodeConnector(connector.toNodeConnectorRef);
        return nodeConnector.currentSpeed
    }

    override readAllFlow(Node node, boolean cached) {
        val input = new GetAllFlowStatisticsInputBuilder;
        input.setNode(node.toNodeRef);
        val result = flowStatisticsService.getAllFlowStatistics(input.build)

        val statistics = result.get.result;
        val output = new ArrayList<FlowOnNode>();
        for (stat : statistics.flowStatistics) {
            // FIXME: Create FlowOnNode
        }
        return output;
    }

    override readAllNodeConnector(Node node, boolean cached) {
        val input = new GetAllNodeConnectorStatisticsInputBuilder();
        input.setNode(node.toNodeRef);
        val result = flowStatisticsService.getAllNodeConnectorStatistics(input.build());
        val statistics = result.get.result.nodeConnectorStatistics;
        val ret = new ArrayList<NodeConnectorStatistics>();
        for (stat : statistics) {
            ret.add(stat.toNodeConnectorStatistics())
        }
        return ret;
    }

    override readAllNodeTable(Node node, boolean cached) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override readDescription(Node node, boolean cached) {
        val capableNode = readFlowCapableNode(node.toNodeRef)

        val it = new NodeDescription()
        manufacturer = capableNode.manufacturer
        serialNumber = capableNode.serialNumber
        software = capableNode.software
        description = capableNode.description

        return it;
    }

    override readFlow(Node node, Flow flow, boolean cached) {
        val input = flowStatisticsInput(node, flow);
        val output = flowStatisticsService.getFlowStatistics(input);

        try {
            val statistics = output.get().getResult();
            if (statistics != null) {
                val it = new FlowOnNode(flow);
                byteCount = statistics.byteCount.value.longValue
                durationNanoseconds = statistics.duration.getNanosecond().getValue().intValue();
                durationSeconds = statistics.duration.getSecond().getValue().intValue();
                packetCount = statistics.getPacketCount().getValue().longValue();
                return it;
            }
        } catch (Exception e) {
            LOG.error("Read flow not processed", e);
        }
        return null;
    }

    override readNodeConnector(NodeConnector connector, boolean cached) {

        val getNodeConnectorStatisticsInput = FromSalConversionsUtils.nodeConnectorStatistics(connector);
        val future = flowStatisticsService.getNodeConnectorStatistics(getNodeConnectorStatisticsInput);
        try {
            val rpcResult = future.get();
            val output = rpcResult.getResult();

            if (output != null) {
                return output.toNodeConnectorStatistics;
            }
        } catch (Exception e) {
            LOG.error("Read node connector not processed", e);
        }

        return null;
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved update) {
        // NOOP
    }

    override onNodeRemoved(NodeRemoved notification) {
        // NOOP
    }

    override onNodeConnectorUpdated(NodeConnectorUpdated update) {
        val properties = Collections.<org.opendaylight.controller.sal.core.Property>emptySet();
        inventoryPublisher.updateNodeConnector(update.nodeConnectorRef.toADNodeConnector, UpdateType.CHANGED, properties);
    }

    override onNodeUpdated(NodeUpdated notification) {
        val properties = Collections.<org.opendaylight.controller.sal.core.Property>emptySet();
        inventoryPublisher.updateNode(notification.nodeRef.toADNode, UpdateType.CHANGED, properties);
    }

    override getNodeProps() {

        // FIXME: Read from MD-SAL inventory service
        return null;
    }

    override getNodeConnectorProps(Boolean refresh) {

        // FIXME: Read from MD-SAL Invcentory Service
        return null;
    }

    override readNodeTable(NodeTable table, boolean cached) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
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

    private static def toNodeConnectorStatistics(
        org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.NodeConnectorStatistics output) {
        val it = new NodeConnectorStatistics

        collisionCount = output.getCollisionCount().longValue();
        receiveCRCErrorCount = output.getReceiveCrcError().longValue();
        receiveFrameErrorCount = output.getReceiveFrameError().longValue();
        receiveOverRunErrorCount = output.getReceiveOverRunError().longValue();

        receiveDropCount = output.getReceiveDrops().longValue();
        receiveErrorCount = output.getReceiveErrors().longValue();
        receivePacketCount = output.getPackets().getReceived().longValue();
        receiveByteCount = output.getBytes().getReceived().longValue();

        transmitDropCount = output.getTransmitDrops().longValue();
        transmitErrorCount = output.getTransmitErrors().longValue();
        transmitPacketCount = output.getPackets().getTransmitted().longValue();
        transmitByteCount = output.getBytes().getTransmitted().longValue();
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

}
