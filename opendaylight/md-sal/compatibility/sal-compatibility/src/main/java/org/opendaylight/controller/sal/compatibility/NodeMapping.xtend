package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem

import static com.google.common.base.Preconditions.*;
import static extension org.opendaylight.controller.sal.common.util.Arguments.*;
import static extension org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils.*;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.controller.sal.core.ConstructionException
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures
import org.opendaylight.controller.sal.core.Bandwidth
import org.opendaylight.controller.sal.core.AdvertisedBandwidth
import org.opendaylight.controller.sal.core.SupportedBandwidth
import org.opendaylight.controller.sal.core.PeerBandwidth
import org.opendaylight.controller.sal.core.Name
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig
import org.opendaylight.controller.sal.core.Config
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import java.util.HashSet
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated
import org.opendaylight.controller.sal.core.Tables
import java.util.List
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability
import org.opendaylight.controller.sal.core.Buffers
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityIpReasm
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityStp
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityArpMatchIp
import org.opendaylight.controller.sal.core.Capabilities
import org.opendaylight.controller.sal.core.MacAddress
import java.util.Date
import org.opendaylight.controller.sal.core.TimeStamp

public class NodeMapping {

    public static val MD_SAL_TYPE = "MD_SAL";
    private static val NODE_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
    private static val NODECONNECTOR_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;


    private new() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }

    public static def toADNode(InstanceIdentifier<?> node) throws ConstructionException {
        return new Node(MD_SAL_TYPE, node.toNodeId.toADNodeId);
    }
    
    public static def toNodeId(InstanceIdentifier<?> node) {
        checkNotNull(node);
        checkNotNull(node.getPath());
        checkArgument(node.getPath().size() >= 2);
        val arg = node.getPath().get(1);
        val item = arg.checkInstanceOf(IdentifiableItem);
        val nodeKey = item.getKey().checkInstanceOf(NodeKey);
        return nodeKey.id
    }
    
    public static def toADNodeId(NodeId nodeId) {
        checkNotNull(nodeId);
        return nodeId.value
    }


    public static def toADNodeConnector(NodeConnectorRef source) throws ConstructionException {
        checkNotNull(source);
        val InstanceIdentifier<?> path = checkNotNull(source.getValue());
        val node = path.toADNode();
        checkArgument(path.path.size() >= 3);
        val arg = path.getPath().get(2);
        val item = arg.checkInstanceOf(IdentifiableItem);
        val connectorKey = item.getKey().checkInstanceOf(NodeConnectorKey);
        return new NodeConnector(connectorKey.id.toNodeConnectorType(path.toNodeId), connectorKey.id.toADNodeConnectorId(path.toNodeId), node);
    }
    
    public static def toNodeConnectorType(NodeConnectorId ncId,NodeId nodeId) {
        if (ncId.equals(nodeId.toLocalNodeConnectorId)) {
            return NodeConnector.NodeConnectorIDType.SWSTACK
        } else if (ncId.equals(nodeId.toNormalNodeConnectorId)) {
            return NodeConnector.NodeConnectorIDType.HWPATH
        } else if (ncId.equals(nodeId.toControllerNodeConnectorId)){
            return NodeConnector.NodeConnectorIDType.CONTROLLER
        }
        return MD_SAL_TYPE
    }
    
    public static def toADNodeConnectorId(NodeConnectorId nodeConnectorId,NodeId nodeId) {
        if(nodeConnectorId.equals(nodeId.toLocalNodeConnectorId) ||
            nodeConnectorId.equals(nodeId.toNormalNodeConnectorId) || 
            nodeConnectorId.equals(nodeId.toControllerNodeConnectorId)
        ) {
            return NodeConnector.SPECIALNODECONNECTORID
        }
        return nodeConnectorId.value
    }
    
    public static def  toControllerNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967293L)
    }
    public static def  toLocalNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967294L)
    }
    public static def  toNormalNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967290L)
    }
    
    public static def toNodeRef(Node node) {
        checkArgument(MD_SAL_TYPE.equals(node.getType()));
        var nodeId = node.ID.checkInstanceOf(String)
        val nodeKey = new NodeKey(new NodeId(nodeId));
        val nodePath = InstanceIdentifier.builder().node(Nodes).child(NODE_CLASS, nodeKey).toInstance();
        return new NodeRef(nodePath);
    }
    
    public static def toNodeConnectorRef(NodeConnector nodeConnector) {
        val node = nodeConnector.node.toNodeRef();
        val nodePath = node.getValue() as InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
        var NodeConnectorId nodeConnectorId
        if(nodeConnector.ID.equals(NodeConnector.SPECIALNODECONNECTORID)){
            if(nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.SWSTACK)) {
                nodeConnectorId = nodePath.toNodeId.toLocalNodeConnectorId
            } else if (nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.HWPATH)) {
                nodeConnectorId = nodePath.toNodeId.toNormalNodeConnectorId
            } else if (nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.CONTROLLER)) {
                nodeConnectorId = nodePath.toNodeId.toControllerNodeConnectorId
            }            
        } else {
            nodeConnectorId = new NodeConnectorId(nodeConnector.ID.checkInstanceOf(String))       
        }
        val connectorKey = new NodeConnectorKey(nodeConnectorId);
        val path = InstanceIdentifier.builder(nodePath).child(NODECONNECTOR_CLASS, connectorKey).toInstance();
        return new NodeConnectorRef(path);
    }

    public static def toADNode(NodeRef node) throws ConstructionException {
        return toADNode(node.getValue());
    }
    
    public static def toADNodeConnectorProperties(NodeConnectorUpdated nc) {
        val props = new HashSet<org.opendaylight.controller.sal.core.Property>();
        val fcncu = nc.getAugmentation(FlowCapableNodeConnectorUpdated)
        if(fcncu != null) {
            if(fcncu.currentFeature != null && fcncu.currentFeature.toAdBandwidth != null) {
                props.add(fcncu.currentFeature.toAdBandwidth)
            }
            if(fcncu.advertisedFeatures != null && fcncu.advertisedFeatures.toAdAdvertizedBandwidth != null) {
                props.add(fcncu.advertisedFeatures.toAdAdvertizedBandwidth)
            }
            if(fcncu.supported != null && fcncu.supported.toAdSupportedBandwidth != null) {
                props.add(fcncu.supported.toAdSupportedBandwidth)
            }
            if(fcncu.peerFeatures != null && fcncu.peerFeatures.toAdPeerBandwidth != null) {
                props.add(fcncu.peerFeatures.toAdPeerBandwidth)
            }
            if(fcncu.name != null && fcncu.name.toAdName != null) {
                props.add(fcncu.name.toAdName)
            }
            if(fcncu.configuration != null && fcncu.configuration.toAdConfig != null) {
                props.add(fcncu.configuration.toAdConfig)
            }
            if(fcncu.state != null && fcncu.state.toAdState != null) {
                props.add(fcncu.state.toAdState)
            }
        }
        return props
    }
    
    public static def toAdName(String name) {
        return new Name(name)
    } 
    
    public static def toAdConfig(PortConfig pc) {
        var Config config;
        if(pc.PORTDOWN){
            config = new Config(Config.ADMIN_DOWN)
        } else {
            config = new Config(Config.ADMIN_UP)
        }
        return config
    }
    
    public static def toAdState(State s) {
        var org.opendaylight.controller.sal.core.State state
        if(s.linkDown) {
            state = new org.opendaylight.controller.sal.core.State(org.opendaylight.controller.sal.core.State.EDGE_DOWN)
        } else {
            state = new org.opendaylight.controller.sal.core.State(org.opendaylight.controller.sal.core.State.EDGE_UP)
        }
        return state
    }
    
    public static def toAdBandwidth(PortFeatures pf) {
        var Bandwidth bw = null
        if (pf.is_10mbHd || pf.is_10mbFd ) {
            bw= new Bandwidth(Bandwidth.BW10Mbps)
        } else if (pf.is_100mbHd || pf.is_100mbFd ) {
            bw= new Bandwidth(Bandwidth.BW100Mbps)
        } else if (pf.is_1gbHd || pf.is_1gbFd ) {
            bw= new Bandwidth(Bandwidth.BW1Gbps)
        } else if (pf.is_1gbFd ) {
            bw= new Bandwidth(Bandwidth.BW10Gbps)
        } else if ( pf.is_10gbFd ) {
            bw= new Bandwidth(Bandwidth.BW10Gbps)
        } else if ( pf.is_40gbFd ) {
            bw= new Bandwidth(Bandwidth.BW40Gbps)
        } else if ( pf.is_100gbFd ) {
            bw= new Bandwidth(Bandwidth.BW100Gbps)
        } else if ( pf.is_1tbFd ) {
            bw= new Bandwidth(Bandwidth.BW1Tbps)
        } 
        return bw;
    }
    
    public static def toAdAdvertizedBandwidth(PortFeatures pf) {
        var AdvertisedBandwidth abw
        val bw = pf.toAdBandwidth
        if(bw != null) {
            abw = new AdvertisedBandwidth(bw.value)
        }
        return abw
    }
    
    public static def toAdSupportedBandwidth(PortFeatures pf) {
        var SupportedBandwidth sbw
        val bw = pf.toAdBandwidth
        if(bw != null ) {
            sbw = new SupportedBandwidth(bw.value)
        }
        return sbw
    }
    
    public static def toAdPeerBandwidth(PortFeatures pf) {
        var PeerBandwidth pbw
        val bw = pf.toAdBandwidth
        if(bw != null) {
            pbw = new PeerBandwidth(bw.value)
        }
        return pbw
    }
    
    public static def toADNodeProperties(NodeUpdated nu) {
        val props = new HashSet<org.opendaylight.controller.sal.core.Property>();
        val fcnu = nu.getAugmentation(FlowCapableNodeUpdated) 
        if(fcnu != null) {
             props.add(toADTimestamp)
             // props.add(fcnu.supportedActions.toADActions) - TODO
             if(nu.id != null) {
                props.add(nu.id.toADMacAddress)
             }
             if(fcnu.switchFeatures != null) {
                 if(fcnu.switchFeatures.maxTables != null) {
                    props.add(fcnu.switchFeatures.maxTables.toADTables) 
                 }
                 if(fcnu.switchFeatures.capabilities != null) {
                    props.add(fcnu.switchFeatures.capabilities.toADCapabiliities)
                 } 
                 if(fcnu.switchFeatures.maxBuffers != null) {
                    props.add(fcnu.switchFeatures.maxBuffers.toADBuffers)
                 }   
             }
        } 
        return props;   
        
    }
    
    public static def toADTimestamp() {
        val date = new Date();
        val timestamp = new TimeStamp(date.time,"connectedSince")
        return timestamp;
    }
    
    public static def toADMacAddress(NodeId id) {
        return new MacAddress(Long.parseLong(id.value.replaceAll("openflow:","")).longValue.bytesFromDpid)
    }
    
    public static def toADTables(Short tables) {
        return new Tables(tables.byteValue)
    }
    
    public static def toADCapabiliities(List<Class<? extends FeatureCapability>> capabilities) {
        var int b
        for(capability : capabilities) {
            if(capability.equals(FlowFeatureCapabilityFlowStats)) {
                b = Capabilities.CapabilitiesType.FLOW_STATS_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityTableStats)) {
                b = Capabilities.CapabilitiesType.TABLE_STATS_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityPortStats)) {
                b = Capabilities.CapabilitiesType.PORT_STATS_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityStp)) {
                b = Capabilities.CapabilitiesType.STP_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityIpReasm)) {
                b = Capabilities.CapabilitiesType.IP_REASSEM_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityQueueStats)) {
                b = Capabilities.CapabilitiesType.QUEUE_STATS_CAPABILITY.value.bitwiseOr(b)
            } else if (capability.equals(FlowFeatureCapabilityArpMatchIp)) {
                b = Capabilities.CapabilitiesType.ARP_MATCH_IP_CAPABILITY.value.bitwiseOr(b)
            }
        }
        return new Capabilities(b)
    }
    
    public static def toADBuffers(Long buffers) {
        return new Buffers(buffers.intValue)
    }
    
}
