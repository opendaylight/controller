package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// org.opendaylight.controller.sal.compatibility.NodeConnectorDataChangeListener
public class NodeConnectorDataChangeListener implements DataChangeListener{
    private final static Logger LOG = LoggerFactory.getLogger(NodeConnectorDataChangeListener.class);

    private List<IPluginOutInventoryService> inventoryPublisher;

    public List<IPluginOutInventoryService> getInventoryPublisher() {
      return this.inventoryPublisher;
    }

    public void setInventoryPublisher(final List<IPluginOutInventoryService> inventoryPublisher) {
      this.inventoryPublisher = inventoryPublisher;
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final Map<InstanceIdentifier<?>,DataObject> createdOperationalData = change.getCreatedOperationalData();
        final Map<InstanceIdentifier<?>,DataObject> updatedOperationalData = change.getUpdatedOperationalData();

        final Set<Map.Entry<InstanceIdentifier<?>,DataObject>> createdEntries = createdOperationalData.entrySet();
        final Set<Map.Entry<InstanceIdentifier<?>,DataObject>> updatedEntries = new HashSet<>();

        updatedEntries.addAll(updatedOperationalData.entrySet());
        updatedEntries.removeAll(createdEntries);

        for(final Map.Entry<InstanceIdentifier<?>,DataObject> entry : createdEntries){
            publishNodeConnectorUpdate(entry, UpdateType.ADDED);
        }

        for(final Map.Entry<InstanceIdentifier<?>,DataObject> entry : updatedEntries){
            publishNodeConnectorUpdate(entry, UpdateType.CHANGED);
        }
    }

    private void publishNodeConnectorUpdate(final Map.Entry<InstanceIdentifier<?>,DataObject> entry, final UpdateType updateType) {
        if (entry.getKey().getTargetType().equals(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class)) {
            NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(entry.getKey());
            NodeConnector nodeConnector = null;
            try {
                nodeConnector = NodeMapping.toADNodeConnector(nodeConnectorRef);
            } catch (ConstructionException e) {
                e.printStackTrace();
            }
            HashSet<Property> _aDNodeConnectorProperties = NodeMapping.toADNodeConnectorProperties((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector) entry.getValue());
            this.publishNodeConnectorUpdate(nodeConnector, updateType, _aDNodeConnectorProperties);
        }
    }

    private void publishNodeConnectorUpdate(final NodeConnector nodeConnector, final UpdateType updateType, final Set<Property> properties) {
      LOG.debug("Publishing NodeConnector " + updateType.toString() + " nodeConnector Id = " + nodeConnector.getNodeConnectorIdAsString());

      List<IPluginOutInventoryService> _inventoryPublisher = getInventoryPublisher();
      for (final IPluginOutInventoryService publisher : _inventoryPublisher) {
        publisher.updateNodeConnector(nodeConnector, updateType, properties);
      }
    }
}
