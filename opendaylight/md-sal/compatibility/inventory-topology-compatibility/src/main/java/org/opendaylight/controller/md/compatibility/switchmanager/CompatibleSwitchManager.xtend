package org.opendaylight.controller.md.compatibility.switchmanager

import org.opendaylight.controller.switchmanager.ISwitchManager
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.Property
import java.util.List
import org.opendaylight.controller.sal.core.Node
import java.net.InetAddress
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.controller.sal.core.Description
import org.opendaylight.controller.sal.core.Tier
import org.opendaylight.controller.sal.core.Bandwidth
import org.opendaylight.controller.sal.core.ForwardingMode
import org.opendaylight.controller.sal.core.MacAddress

import org.slf4j.LoggerFactory
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import java.util.ArrayList
import org.opendaylight.controller.switchmanager.Switch
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import java.util.Map
import java.util.HashSet
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState

class CompatibleSwitchManager extends ConfigurableSwitchManager implements ISwitchManager {

    private static val log = LoggerFactory.getLogger(CompatibleSwitchManager)

    @org.eclipse.xtend.lib.Property
    var DataBrokerService dataService;

    override addNodeConnectorProp(NodeConnector nodeConnector, Property prop) {
        val it = dataService.beginTransaction
        val path = nodeConnector.toNodeConnectorRef

        // TODO: Update FlowCapableNode
        return null;
    }

    override createProperty(String propName, String propValue) {
        try {
            if (propName.equalsIgnoreCase(Description.propertyName)) {
                return new Description(propValue);
            } else if (propName.equalsIgnoreCase(Tier.TierPropName)) {
                val tier = Integer.parseInt(propValue);
                return new Tier(tier);
            } else if (propName.equalsIgnoreCase(Bandwidth.BandwidthPropName)) {
                val bw = Long.parseLong(propValue);
                return new Bandwidth(bw);
            } else if (propName.equalsIgnoreCase(ForwardingMode.name)) {
                val mode = Integer.parseInt(propValue);
                return new ForwardingMode(mode);
            } else if (propName.equalsIgnoreCase(MacAddress.name)) {
                return new MacAddress(propValue);
            } else {
                log.debug("Not able to create {} property", propName);
            }
        } catch (Exception e) {
            log.debug("createProperty caught exception {}", e.getMessage());
        }
        return null;
    }

    override doesNodeConnectorExist(NodeConnector nc) {
        val ref = nc.toNodeConnectorRef
        return dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>) !== null
    }

    override getControllerMAC() {
        var byte[] macAddress = null;

        try {
            val nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                val ni = nis.nextElement();
                try {
                    macAddress = ni.getHardwareAddress();
                    return macAddress;
                } catch (SocketException e) {
                    log.error("Failed to acquire controller MAC: ", e);
                }
            }
        } catch (SocketException e) {
            log.error("Failed to acquire controller MAC: ", e);
            return macAddress;
        }

        if (macAddress == null) {
            log.warn("Failed to acquire controller MAC: No physical interface found");

            // This happens when running controller on windows VM, for example
            // Try parsing the OS command output
            }
            return macAddress;
        }

    override getControllerProperties() {
        return Collections.emptyMap()
    }

    override getControllerProperty(String propertyName) {
        return null;
    }

    override getNetworkDevices() {
        val path = InstanceIdentifier.builder().node(Nodes).toInstance;
        val data = dataService.readOperationalData(path) as Nodes;
        val ret = new ArrayList<Switch>();
        for (node : data.node) {
            ret.add(node.toSwitch());
        }
        return ret;
    }

    override getNodeConnector(Node node, String nodeConnectorName) {
        val key = new NodeConnectorKey(new NodeConnectorId(nodeConnectorName));
        return new NodeConnector(MD_SAL_TYPE, key, node);
    }

    override getNodeConnectorProp(NodeConnector nodeConnector, String propName) {
        getNodeConnectorProps(nodeConnector).get(propName);
    }

    override getNodeConnectorProps(NodeConnector nodeConnector) {
        val ref = nodeConnector.toNodeConnectorRef
        val data = readNodeConnector(ref.value);
        return data.toAdProperties();
    }

    override getNodeConnectors(Node node) {
        val ref = node.toNodeRef;
        val data = readNode(ref.value);
        val ret = new HashSet();
        for (nc : data.nodeConnector) {

            val adConnector = new NodeConnector(MD_SAL_TYPE, nc.key, node);
            ret.add(adConnector);
        }
        return ret;
    }

    override getNodeDescription(Node node) {
        (getNodeProps(node).get(Description.propertyName) as Description).value;
    }

    override getNodeMAC(Node node) {
        (getNodeProps(node).get(MacAddress.name) as MacAddress).macAddress;
    }

    override getNodeProp(Node node, String propName) {
        getNodeProps(node).get(propName)
    }

    override getNodeProps(Node node) {
        val ref = node.toNodeRef;
        val data = dataService.readOperationalData(ref.value as InstanceIdentifier<? extends DataObject>) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
        return data.toAdProperties();
    }

    override getNodes() {
        val path = InstanceIdentifier.builder().node(Nodes).toInstance;
        val data = dataService.readOperationalData(path) as Nodes;
        val ret = new HashSet<Node>();
        for (node : data.node) {
            ret.add(new Node(MD_SAL_TYPE, node.key));
        }
        return ret;
    }

    def Switch toSwitch(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node) {
        val adNode = new Node(MD_SAL_TYPE, node.key);
        val sw = new Switch(adNode)
        return sw;
    }

    override getPhysicalNodeConnectors(Node node) {
        val ref = node.toNodeRef;
        val data = readNode(ref.value);
        val ret = new HashSet();
        for (nc : data.nodeConnector) {
            val flowConnector = nc.getAugmentation(FlowCapableNodeConnector)
            val adConnector = new NodeConnector(MD_SAL_TYPE, nc.key, node);
            ret.add(adConnector);
        }
        return ret;
    }

    def Map<String, Property> toAdProperties(
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector connector) {
        return Collections.emptyMap
    }

    def Map<String, Property> toAdProperties(
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node connector) {
        return Collections.emptyMap
    }

    def readNode(InstanceIdentifier<?> ref) {
        dataService.readOperationalData(ref as InstanceIdentifier<? extends DataObject>) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
    }

    def readNodeConnector(InstanceIdentifier<?> ref) {
        dataService.readOperationalData(ref as InstanceIdentifier<? extends DataObject>) as org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
    }

    override getSpanPorts(Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getSubnetByNetworkAddress(InetAddress networkAddress) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getUpNodeConnectors(Node node) {
        val ref = node.toNodeRef
        val data = readNode(ref.value);
        val ret = new HashSet<NodeConnector>();
        for (nc : data.nodeConnector) {
            val flowConn = nc.getAugmentation(FlowCapableNodeConnector);
            if (flowConn != null && flowConn.state == PortState.Live) {
                ret.add(new NodeConnector(MD_SAL_TYPE, nc.key, node));
            }
        }
        return ret;
    }

    override isNodeConnectorEnabled(NodeConnector nodeConnector) {
        val ref = nodeConnector.toNodeConnectorRef
        val data = readNodeConnector(ref.value);

        return true;
    }

    override isSpecial(NodeConnector p) {
        val ref = p.toNodeConnectorRef
        val data = readNodeConnector(ref.value);

        return true;
    }

    override removeControllerProperty(String propertyName) {
        // NOOP
    }

    override removeNodeAllProps(Node node) {
        // NOOP: not supported node has more properties than AD-SAL is capable to see
    }

    override removeNodeConnectorAllProps(NodeConnector nodeConnector) {
        // NOOP: not supported node has more properties than AD-SAL is capable to see
    }

    override removeNodeConnectorProp(NodeConnector nc, String propName) {
        // NOOP: not supported node has more properties than AD-SAL is capable to see
    }

    override removeNodeProp(Node node, String propName) {
        // NOOP: not supported node has more properties than AD-SAL is capable to see
    }

    override removePortsFromSubnet(String name, List<String> nodeConnectors) {
        // NOOP
    }

    override removeSubnet(String name) {
        // NOOP
    }

    override setControllerProperty(Property property) {
        // NOOP
    }

    override setNodeProp(Node node, Property prop) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addPortsToSubnet(String name, List<String> nodeConnectors) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    }
