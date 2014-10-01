package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCDataChangeListener extends AbstractDataChangeListener<NodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeDataChangeListener.class);
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    public NCDataChangeListener (final InventoryAndReadAdapter adapter, final DataBroker db) {
        super(adapter,db);
        registrationListener(db, 5);
    }

    @Override
    protected void add(InstanceIdentifier<NodeConnector> createKeyIdent, NodeConnector node,
            InstanceIdentifier<FlowCapableNode> nodeIdent) {
        FlowCapableNodeConnector fcnc = node.getAugmentation(FlowCapableNodeConnector.class);
        if(fcnc != null) {
            FlowCapableNodeConnectorUpdatedBuilder fcncub = new FlowCapableNodeConnectorUpdatedBuilder(fcnc);
            NodeConnectorUpdatedBuilder builder = new NodeConnectorUpdatedBuilder();
            builder.setId(node.getId());
            builder.setNodeConnectorRef(new NodeConnectorRef(createKeyIdent));
            builder.addAugmentation(FlowCapableNodeConnectorUpdated.class, fcncub.build());
            adapter.onNodeConnectorUpdated(builder.build());
        }
    }

    @Override
    protected void update(InstanceIdentifier<NodeConnector> updateKeyIdent, NodeConnector original,
            NodeConnector update, InstanceIdentifier<FlowCapableNode> nodeIdent) {
        add(updateKeyIdent,update,nodeIdent);
    }

    @Override
    protected void remove(InstanceIdentifier<NodeConnector> ident, NodeConnector removeValue,
            InstanceIdentifier<FlowCapableNode> nodeIdent) {
        NodeConnectorRemovedBuilder builder = new NodeConnectorRemovedBuilder();
        builder.setNodeConnectorRef(new NodeConnectorRef(ident));
        adapter.onNodeConnectorRemoved(builder.build());
    }

    protected InstanceIdentifier<NodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class);
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
                LOG.error("NCDataChangeListener registration fail!", e);
                throw new IllegalStateException("NCDataChangeListener registration Listener fail! System needs restart.", e);
            }
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error by stop NCDataChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

}
