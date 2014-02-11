package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

public class InventoryNotificationProvider implements AutoCloseable{

    private Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> nodeConnectorCommitHandlerRegistration;

    private Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject>> nodeCommitHandlerRegistration;

    private NodeConnectorCommitHandler nodeConnectorCommitHandler;

    private NodeCommitHandler nodeCommitHandler;

    private DataProviderService dataProviderService;

    private List<IPluginOutInventoryService> inventoryPublisher;

    public void start(){
        if(dataProviderService != null
                && inventoryPublisher!= null){

            if(nodeConnectorCommitHandler == null){
                nodeConnectorCommitHandler = new NodeConnectorCommitHandler();
                nodeConnectorCommitHandler.setInventoryPublisher(inventoryPublisher);

                // Is this path correct? I want to be called whenever a NodeConnector is added, modified or removed
                InstanceIdentifier nodeConnectorPath = InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).build();
                nodeConnectorCommitHandlerRegistration = dataProviderService.registerCommitHandler(nodeConnectorPath, nodeConnectorCommitHandler);
            }

//            if(nodeCommitHandler == null){
//                nodeCommitHandler = new NodeCommitHandler();
//                nodeCommitHandler.setInventoryPublisher(inventoryPublisher);
                  // Is this path correct? I want to be called whenever a Node is added, modified or removed
                  // In practice I see objects other than Node coming into the commit handlers. How do I avoid that?
//                InstanceIdentifier nodePath = InstanceIdentifier.builder(Nodes.class).child(Node.class).build();
//                nodeConnectorCommitHandlerRegistration = dataProviderService.registerCommitHandler(nodePath, nodeCommitHandler);
//            }
        }
    }

    @Override
    public void close() throws Exception {
        if(nodeConnectorCommitHandlerRegistration != null){
            nodeConnectorCommitHandlerRegistration.close();
        }

//        if(nodeCommitHandlerRegistration != null){
//            nodeCommitHandlerRegistration.close();
//        }

    }

    public void setDataProviderService(DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    public void setInventoryPublisher(List<IPluginOutInventoryService> inventoryPublisher) {
        this.inventoryPublisher = inventoryPublisher;
    }
}
