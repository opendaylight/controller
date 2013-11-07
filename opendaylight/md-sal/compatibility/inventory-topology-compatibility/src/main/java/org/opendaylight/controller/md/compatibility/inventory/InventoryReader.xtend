package org.opendaylight.controller.md.compatibility.inventory

import org.opendaylight.controller.switchmanager.ISwitchManager
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import java.util.ArrayList
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import static extension org.opendaylight.controller.sal.compatibility.InventoryMapping.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder

class InventoryReader implements RuntimeDataProvider {

    @Property
    var ISwitchManager switchManager;

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {

        // Topology and Inventory are operational only
        return null;
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        val type = path.targetType;
        var DataObject data = null;
        switch (type) {
            case Nodes:
                data = readNodes(path as InstanceIdentifier<Nodes>)
            case Node:
                data = readNode(path as InstanceIdentifier<Node>)
            case NodeConnector:
                data = readNodeConnector(path as InstanceIdentifier<NodeConnector>)
        }
        return data;
    }

    def DataObject readNodeConnector(InstanceIdentifier<NodeConnector> identifier) {
        val nodeConnector = identifier.toAdNodeConnector();
        return constructNodeConnector(nodeConnector)
    }

    def DataObject readNode(InstanceIdentifier<Node> identifier) {
        val node = identifier.toAdNode();
        return constructNode(node);
    }


    def Node constructNode(org.opendaylight.controller.sal.core.Node node) {
        val connectors = switchManager.getNodeConnectors(node)

        val tpList = new ArrayList<NodeConnector>(connectors.size)
        for (connector : connectors) {
            tpList.add(constructNodeConnector(connector));
        }

        val it = new NodeBuilder()
        key = node.toNodeKey();
        nodeConnector = tpList
        return build();
    }

    def NodeConnector constructNodeConnector(org.opendaylight.controller.sal.core.NodeConnector connector) {
        val it = new NodeConnectorBuilder()
        key = connector.toNodeConnectorKey()
        return build();
    }

    def readNodes(InstanceIdentifier<Nodes> identifier) {
        val nodes = switchManager.nodes
        val nodeList = new ArrayList<Node>(nodes.size)
        for (node : nodes) {
            nodeList.add(constructNode(node))
        }
        val it = new NodesBuilder();
        node = nodeList
        return build()

    }
}
