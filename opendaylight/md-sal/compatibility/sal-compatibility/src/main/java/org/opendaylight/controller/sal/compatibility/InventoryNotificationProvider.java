package org.opendaylight.controller.sal.compatibility;

import java.util.List;

import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryNotificationProvider implements AutoCloseable{

    private ListenerRegistration<DataChangeListener> nodeConnectorDataChangeListenerRegistration;

    private NodeConnectorDataChangeListener nodeConnectorDataChangeListener;

    private DataProviderService dataProviderService;

    private List<IPluginOutInventoryService> inventoryPublisher;

    private final static Logger LOG = LoggerFactory.getLogger(NodeConnectorDataChangeListener.class);

    public void start(){

        LOG.info("InventoryNotificationProvider started");

        if(dataProviderService != null
                && inventoryPublisher!= null){

            if(nodeConnectorDataChangeListener == null){
                InstanceIdentifier<NodeConnector> nodeConnectorPath = InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).build();
                nodeConnectorDataChangeListener = new NodeConnectorDataChangeListener();
                nodeConnectorDataChangeListener.setInventoryPublisher(inventoryPublisher);
                nodeConnectorDataChangeListenerRegistration = dataProviderService.registerDataChangeListener(nodeConnectorPath, nodeConnectorDataChangeListener);
            }

        }
    }

    @Override
    public void close() throws Exception {
        if(nodeConnectorDataChangeListenerRegistration != null){
            nodeConnectorDataChangeListenerRegistration.close();
        }
    }

    public void setDataProviderService(DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    public void setInventoryPublisher(List<IPluginOutInventoryService> inventoryPublisher) {
        this.inventoryPublisher = inventoryPublisher;
    }
}
