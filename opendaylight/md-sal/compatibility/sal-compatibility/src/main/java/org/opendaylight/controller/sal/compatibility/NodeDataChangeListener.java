package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDataChangeListener extends AbstractDataChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeDataChangeListener.class);
    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public NodeDataChangeListener (final InventoryAndReadAdapter adapter, final DataBroker db) {
        super(adapter,db);
        registrationListener(db, 5);
    }

    protected void add(InstanceIdentifier<Node> createKeyIdent, Node node,
            InstanceIdentifier<FlowCapableNode> nodeIdent) {
        FlowCapableNode fcn = node.getAugmentation(FlowCapableNode.class);
        if(fcn != null) {
            FlowCapableNodeUpdatedBuilder fcbnu = new FlowCapableNodeUpdatedBuilder(fcn);
            NodeUpdatedBuilder builder = new NodeUpdatedBuilder();
            builder.setId(node.getId());
            builder.setNodeRef(new NodeRef(createKeyIdent));
            builder.setNodeConnector(node.getNodeConnector());
            builder.addAugmentation(FlowCapableNodeUpdated.class, fcbnu.build());
            adapter.onNodeUpdated(builder.build());
        }
    }

    protected void update(InstanceIdentifier<Node> updateKeyIdent, Node original,
            Node update, InstanceIdentifier<FlowCapableNode> nodeIdent) {
        this.add(updateKeyIdent, update, nodeIdent);
    }

    protected void remove(InstanceIdentifier<Node> ident, Node removeValue,
            InstanceIdentifier<FlowCapableNode> nodeIdent) {
        NodeRemovedBuilder builder = new NodeRemovedBuilder();
        builder.setNodeRef(new NodeRef(ident));
        adapter.onNodeRemoved(builder.build());
    }

    protected InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class);
    }

    protected void registrationListener(final DataBroker db, int i) {
        try {
            listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    getWildCardPath(), this, DataChangeScope.BASE);
        } catch (final Exception e) {
            if (i >= 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    LOG.error("Thread interrupted '{}'", e1);
                    Thread.currentThread().interrupt();
                }
                registrationListener(db, --i);
            } else {
                LOG.error("NodeDataChangeListener registration fail!", e);
                throw new IllegalStateException("NodeDataChangeListener registration Listener fail! System needs restart.", e);
            }
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error by stop NodeDataChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

}
