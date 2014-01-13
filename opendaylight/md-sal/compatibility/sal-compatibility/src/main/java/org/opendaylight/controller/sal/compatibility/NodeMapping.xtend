package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri

public class NodeMapping {

    public static val MD_SAL_NODE_TYPE = NodeIDType.OPENFLOW;
    public static val MD_SAL_NODECONNECTOR_TYPE = NodeConnectorIDType.OPENFLOW;
    public static val OF_PREFIX = "openflow:"
    private static val NODE_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
    private static val NODECONNECTOR_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.
        NodeConnector;

    private new() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }

    public static def toADNode(InstanceIdentifier<?> node) throws ConstructionException {
        return node.toNodeId.toADNode
    }

    public static def toADNode(NodeId id) {
        return new Node(MD_SAL_NODE_TYPE, id.toADNodeId);
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
        return nodeId.toDpid
    }

    public static def toADNodeConnector(NodeConnectorRef source) throws ConstructionException {
        checkNotNull(source);
        val InstanceIdentifier<?> path = checkNotNull(source.getValue());
        checkArgument(path.path.size() >= 3);
        val arg = path.getPath().get(2);
        val item = arg.checkInstanceOf(IdentifiableItem);
        val connectorKey = item.getKey().checkInstanceOf(NodeConnectorKey);
        return connectorKey.id.toADNodeConnector(path.toNodeId)
    }
    
    public static def toADNodeConnector(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId ncid,
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nid) {
            return new NodeConnector(ncid.toNodeConnectorType(nid),
            ncid.toADNodeConnectorId(nid), nid.toADNode);
     }

    public static def toNodeConnectorType(NodeConnectorId ncId, NodeId nodeId) {
        if (ncId.equals(nodeId.toLocalNodeConnectorId)) {
            return NodeConnector.NodeConnectorIDType.SWSTACK
        } else if (ncId.equals(nodeId.toNormalNodeConnectorId)) {
            return NodeConnector.NodeConnectorIDType.HWPATH
        } else if (ncId.equals(nodeId.toControllerNodeConnectorId)) {
            return NodeConnector.NodeConnectorIDType.CONTROLLER
        }
        return MD_SAL_NODECONNECTOR_TYPE
    }

    public static def toADNodeConnectorId(NodeConnectorId nodeConnectorId, NodeId nodeId) {
        if (nodeConnectorId.equals(nodeId.toLocalNodeConnectorId) ||
            nodeConnectorId.equals(nodeId.toNormalNodeConnectorId) ||
            nodeConnectorId.equals(nodeId.toControllerNodeConnectorId)) {
            return NodeConnector.SPECIALNODECONNECTORID
        }
        return nodeConnectorId.toPortNumber
    }
    
    public static def toPortNumber(NodeConnectorId ncId){
        val split = ncId.value.split(":").toList;

        val portNoString = split.get(split.length-1);
        val portNo = Short.decode(portNoString);
        return portNo;
    }

    public static def toControllerNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967293L)
    }

    public static def toLocalNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967294L)
    }

    public static def toNormalNodeConnectorId(NodeId node) {
        return new NodeConnectorId(node.value + ":" + 4294967290L)
    }

    public static def toNodeRef(Node node) {
        checkArgument(MD_SAL_NODE_TYPE.equals(node.getType()));
        var nodeId = node.ID.checkInstanceOf(Long)
        val nodeKey = new NodeKey(new NodeId(OF_PREFIX + nodeId));
        val nodePath = InstanceIdentifier.builder().node(Nodes).child(NODE_CLASS, nodeKey).toInstance();
        return new NodeRef(nodePath);
    }

    public static def toNodeConnectorRef(NodeConnector nodeConnector) {
        val node = nodeConnector.node.toNodeRef();
        val nodePath = node.getValue() as InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
        var NodeConnectorId nodeConnectorId
        if (nodeConnector.ID.equals(NodeConnector.SPECIALNODECONNECTORID)) {
            if (nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.SWSTACK)) {
                nodeConnectorId = nodePath.toNodeId.toLocalNodeConnectorId
            } else if (nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.HWPATH)) {
                nodeConnectorId = nodePath.toNodeId.toNormalNodeConnectorId
            } else if (nodeConnector.type.equals(NodeConnector.NodeConnectorIDType.CONTROLLER)) {
                nodeConnectorId = nodePath.toNodeId.toControllerNodeConnectorId
            }
        } else {
            nodeConnectorId = new NodeConnectorId(OF_PREFIX + nodeConnector.node.ID.checkInstanceOf(Long) + ":" + nodeConnector.ID.checkInstanceOf(Short))
        }
        val connectorKey = new NodeConnectorKey(nodeConnectorId);
        val path = InstanceIdentifier.builder(nodePath).child(NODECONNECTOR_CLASS, connectorKey).toInstance();
        return new NodeConnectorRef(path);
    }

    public static def toADNode(NodeRef node) throws ConstructionException {
        return toADNode(node.getValue());
    }

    public static def toADNodeConnectorProperties(NodeConnectorUpdated nc) {
        val fcncu = nc.getAugmentation(FlowCapableNodeConnectorUpdated)
        if (fcncu != null) {
            return fcncu.toADNodeConnectorProperties
        }
        return new HashSet<org.opendaylight.controller.sal.core.Property>();
    }

    public static def toADNodeConnectorProperties(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc) {
        val fcnc = nc.getAugmentation(FlowCapableNodeConnector)
        if (fcnc != null) {
            return fcnc.toADNodeConnectorProperties
        }
        return new HashSet<org.opendaylight.controller.sal.core.Property>();
    }

    public static def toADNodeConnectorProperties(FlowNodeConnector fcncu) {
        val props = new HashSet<org.opendaylight.controller.sal.core.Property>();
        if (fcncu != null) {
            if (fcncu.currentFeature != null && fcncu.currentFeature.toAdBandwidth != null) {
                props.add(fcncu.currentFeature.toAdBandwidth)
            }
            if (fcncu.advertisedFeatures != null && fcncu.advertisedFeatures.toAdAdvertizedBandwidth != null) {
                props.add(fcncu.advertisedFeatures.toAdAdvertizedBandwidth)
            }
            if (fcncu.supported != null && fcncu.supported.toAdSupportedBandwidth != null) {
                props.add(fcncu.supported.toAdSupportedBandwidth)
            }
            if (fcncu.peerFeatures != null && fcncu.peerFeatures.toAdPeerBandwidth != null) {
                props.add(fcncu.peerFeatures.toAdPeerBandwidth)
            }
            if (fcncu.name != null && fcncu.name.toAdName != null) {
                props.add(fcncu.name.toAdName)
            }
            if (fcncu.configuration != null && fcncu.configuration.toAdConfig != null) {
                props.add(fcncu.configuration.toAdConfig)
            }
            if (fcncu.state != null && fcncu.state.toAdState != null) {
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
        if (pc.PORTDOWN) {
            config = new Config(Config.ADMIN_DOWN)
        } else {
            config = new Config(Config.ADMIN_UP)
        }
        return config
    }

    public static def toAdState(State s) {
        var org.opendaylight.controller.sal.core.State state
        if (s.linkDown) {
            state = new org.opendaylight.controller.sal.core.State(org.opendaylight.controller.sal.core.State.EDGE_DOWN)
        } else {
            state = new org.opendaylight.controller.sal.core.State(org.opendaylight.controller.sal.core.State.EDGE_UP)
        }
        return state
    }

    public static def toAdBandwidth(PortFeatures pf) {
        var Bandwidth bw = null
        if (pf.isTenMbHd || pf.isTenMbFd) {
            bw = new Bandwidth(Bandwidth.BW10Mbps)
        } else if (pf.isHundredMbHd || pf.isHundredMbFd) {
            bw = new Bandwidth(Bandwidth.BW100Mbps)
        } else if (pf.isOneGbHd || pf.isOneGbFd) {
            bw = new Bandwidth(Bandwidth.BW1Gbps)
        } else if (pf.isOneGbFd) {
            bw = new Bandwidth(Bandwidth.BW10Gbps)
        } else if (pf.isTenGbFd) {
            bw = new Bandwidth(Bandwidth.BW10Gbps)
        } else if (pf.isFortyGbFd) {
            bw = new Bandwidth(Bandwidth.BW40Gbps)
        } else if (pf.isHundredGbFd) {
            bw = new Bandwidth(Bandwidth.BW100Gbps)
        } else if (pf.isOneTbFd) {
            bw = new Bandwidth(Bandwidth.BW1Tbps)
        }
        return bw;
    }

    public static def toAdAdvertizedBandwidth(PortFeatures pf) {
        var AdvertisedBandwidth abw
        val bw = pf.toAdBandwidth
        if (bw != null) {
            abw = new AdvertisedBandwidth(bw.value)
        }
        return abw
    }

    public static def toAdSupportedBandwidth(PortFeatures pf) {
        var SupportedBandwidth sbw
        val bw = pf.toAdBandwidth
        if (bw != null) {
            sbw = new SupportedBandwidth(bw.value)
        }
        return sbw
    }

    public static def toAdPeerBandwidth(PortFeatures pf) {
        var PeerBandwidth pbw
        val bw = pf.toAdBandwidth
        if (bw != null) {
            pbw = new PeerBandwidth(bw.value)
        }
        return pbw
    }

    public static def toADNodeProperties(NodeUpdated nu) {
        val fcnu = nu.getAugmentation(FlowCapableNodeUpdated)
        if (fcnu != null) {
            return fcnu.toADNodeProperties(nu.id)
        }
        return new HashSet<org.opendaylight.controller.sal.core.Property>();

    }

    public static def toADNodeProperties(FlowNode fcnu, NodeId id) {
        val props = new HashSet<org.opendaylight.controller.sal.core.Property>();
        if (fcnu != null) {
            props.add(toADTimestamp)

            // props.add(fcnu.supportedActions.toADActions) - TODO
            if (id != null) {
                props.add(id.toADMacAddress)
            }
            if (fcnu.switchFeatures != null) {
                if (fcnu.switchFeatures.maxTables != null) {
                    props.add(fcnu.switchFeatures.maxTables.toADTables)
                }
                if (fcnu.switchFeatures.capabilities != null) {
                    props.add(fcnu.switchFeatures.capabilities.toADCapabiliities)
                }
                if (fcnu.switchFeatures.maxBuffers != null) {
                    props.add(fcnu.switchFeatures.maxBuffers.toADBuffers)
                }
            }
        }
        return props;
    }

    public static def toADTimestamp() {
        val date = new Date();
        val timestamp = new TimeStamp(date.time, "connectedSince")
        return timestamp;
    }

    public static def toADMacAddress(NodeId id) {
        return new MacAddress(id.toDpid.longValue.bytesFromDpid)
    }
    
    public static def toDpid(NodeId id) {
        return Long.parseLong(id.value.replaceAll(OF_PREFIX, ""))
    }

    public static def toADTables(Short tables) {
        return new Tables(tables.byteValue)
    }

    public static def toADCapabiliities(List<Class<? extends FeatureCapability>> capabilities) {
        var int b
        for (capability : capabilities) {
            if (capability.equals(FlowFeatureCapabilityFlowStats)) {
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
