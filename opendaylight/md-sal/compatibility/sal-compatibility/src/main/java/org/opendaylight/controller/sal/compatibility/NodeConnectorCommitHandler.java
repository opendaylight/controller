package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeConnectorCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final static Logger LOG = LoggerFactory.getLogger(NodeConnectorCommitHandler.class);

    private List<IPluginOutInventoryService> inventoryPublisher;

    public List<IPluginOutInventoryService> getInventoryPublisher() {
      return this.inventoryPublisher;
    }

    public void setInventoryPublisher(final List<IPluginOutInventoryService> inventoryPublisher) {
      this.inventoryPublisher = inventoryPublisher;
    }


    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new NodeConnectorTransaction(modification);
    }

    private class NodeConnectorTransaction implements DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {
        private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        public NodeConnectorTransaction(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification){
            this.modification = modification;
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            final Map<InstanceIdentifier<? extends DataObject>,DataObject> createdOperationalData = getModification().getCreatedOperationalData();
            final Map<InstanceIdentifier<? extends DataObject>,DataObject> updatedOperationalData = getModification().getUpdatedOperationalData();

            final Set<Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject>> createdEntries = createdOperationalData.entrySet();
            final Set<Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject>> updatedEntries = new HashSet<>();

            updatedEntries.addAll(updatedOperationalData.entrySet());
            updatedEntries.removeAll(createdEntries);

            for(final Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject> entry : createdEntries){
                publishNodeConnectorUpdate(entry, UpdateType.ADDED);
            }

            for(final Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject> entry : updatedEntries){
                publishNodeConnectorUpdate(entry, UpdateType.CHANGED);
            }

            Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
            return Rpcs.<Void>getRpcResult(true, null, _emptySet);
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
            return Rpcs.<Void>getRpcResult(true, null, _emptySet);
        }

        private void publishNodeConnectorUpdate(final Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject> entry, final UpdateType updateType) {
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

        private void publishNodeConnectorUpdate(final NodeConnector nodeConnector, final UpdateType updateType, final Set<Property> properties) {
          LOG.debug("Publishing NodeConnector " + updateType.toString() + " nodeConnector Id = " + nodeConnector.getNodeConnectorIdAsString());

          List<IPluginOutInventoryService> _inventoryPublisher = getInventoryPublisher();
          for (final IPluginOutInventoryService publisher : _inventoryPublisher) {
            publisher.updateNodeConnector(nodeConnector, updateType, properties);
          }
        }

    }
}
