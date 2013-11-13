package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem

import static com.google.common.base.Preconditions.*;
import static extension org.opendaylight.controller.sal.common.util.Arguments.*;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.controller.sal.core.ConstructionException
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes

public class NodeMapping {

    public static val MD_SAL_TYPE = "MD_SAL";
    private static val NODE_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
    private static val NODECONNECTOR_CLASS = org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.
        NodeConnector;

    private new() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }

    public static def toADNode(InstanceIdentifier<?> node) throws ConstructionException {
        checkNotNull(node);
        checkNotNull(node.getPath());
        checkArgument(node.getPath().size() >= 2);
        val arg = node.getPath().get(1);
        val item = arg.checkInstanceOf(IdentifiableItem);
        val nodeKey = item.getKey().checkInstanceOf(NodeKey);
        return new Node(MD_SAL_TYPE, nodeKey);
    }

    public static def toADNodeConnector(NodeConnectorRef source) throws ConstructionException {
        checkNotNull(source);
        val InstanceIdentifier<?> path = checkNotNull(source.getValue());
        val node = path.toADNode();
        checkArgument(path.path.size() >= 3);
        val arg = path.getPath().get(2);
        val item = arg.checkInstanceOf(IdentifiableItem);
        val connectorKey = item.getKey().checkInstanceOf(NodeConnectorKey);
        return new NodeConnector(MD_SAL_TYPE, connectorKey, node);
    }

    public static def toNodeRef(Node node) {
        checkArgument(MD_SAL_TYPE.equals(node.getType()));
        val nodeKey = node.ID.checkInstanceOf(NodeKey);
        val nodePath = InstanceIdentifier.builder().node(Nodes).child(NODE_CLASS, nodeKey).toInstance();
        return new NodeRef(nodePath);
    }

    public static def toNodeConnectorRef(NodeConnector nodeConnector) {
        val node = nodeConnector.node.toNodeRef();
        val nodePath = node.getValue() as InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
        val connectorKey = nodeConnector.ID.checkInstanceOf(NodeConnectorKey);
        val path = InstanceIdentifier.builder(nodePath).child(NODECONNECTOR_CLASS, connectorKey).toInstance();
        return new NodeConnectorRef(path);
    }

    public static def toADNode(NodeRef node) throws ConstructionException {
        return toADNode(node.getValue());
    }

}
