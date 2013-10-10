package org.opendaylight.controller.sal.compability;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

import static org.opendaylight.controller.sal.compability.ADSALUtils.*;

public class NodeInventoryAdapter implements OpendaylightInventoryListener {
    
    IPluginOutInventoryService adSalPublisher;
    
    public static final String MD_SAL_TYPE = "MD_SAL";
    
    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved notification) {
        
    }
    
    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated notification) {
        // FIMXE
    }

    @Override
    public void onNodeRemoved(NodeRemoved notification) {
     // FIMXE
    }
    
    @Override
    public void onNodeUpdated(NodeUpdated notification) {
     // FIMXE
    }

    public static NodeRef nodeRef(Node node) {
        if(false == MD_SAL_TYPE.equals(node.getType())) {
            throw new IllegalArgumentException();
        }
        NodeKey nodeKey = (NodeKey) node.getID();
        InstanceIdentifier<?> nodePath = InstanceIdentifier.builder().node(Nodes.class) //
                .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey).toInstance();
        return new NodeRef(nodePath);
    }

    // TODO: implement correct conversion
    public static NodeConnectorRef nodeConnectorRef(
            NodeConnector nodeConnector) {
        NodeRef node = nodeRef(nodeConnector.getNode());
        NodeConnectorKey connectorKey = (NodeConnectorKey) nodeConnector.getID();
        InstanceIdentifier path = InstanceIdentifier.builder(node.getValue()) //
                .node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class, connectorKey).toInstance();
        return new NodeConnectorRef(path);
    }

}
