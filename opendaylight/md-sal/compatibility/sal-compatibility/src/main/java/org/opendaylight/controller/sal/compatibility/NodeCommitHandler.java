package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>  {

    private List<IPluginOutInventoryService> inventoryPublisher;

    public List<IPluginOutInventoryService> getInventoryPublisher() {
      return this.inventoryPublisher;
    }

    public void setInventoryPublisher(final List<IPluginOutInventoryService> inventoryPublisher) {
      this.inventoryPublisher = inventoryPublisher;
    }

    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new NodeTransaction(modification);
    }

    private class NodeTransaction implements DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {
        private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        public NodeTransaction(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
            this.modification = modification;
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return this.modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            final Map<InstanceIdentifier<? extends DataObject>,DataObject> createdOperationalData = getModification().getCreatedOperationalData();
            final Map<InstanceIdentifier<? extends DataObject>,DataObject> updatedOperationalData = getModification().getUpdatedOperationalData();

            final Set<Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject>> createdEntries = createdOperationalData.entrySet();
            final Set<Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject>> updatedEntries = new HashSet<>();
            final Set<InstanceIdentifier<? extends DataObject>> deletedEntries = getModification().getRemovedOperationalData();

            updatedEntries.addAll(updatedOperationalData.entrySet());
            updatedEntries.removeAll(createdEntries);

            for(final Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject> entry : createdEntries){
                publishNodeUpdate(entry.getKey(), (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) entry.getValue(), UpdateType.ADDED);
            }

            for(final Map.Entry<InstanceIdentifier<? extends DataObject>,DataObject> entry : updatedEntries){
                publishNodeUpdate(entry.getKey(), (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) entry.getValue(), UpdateType.CHANGED);
            }

            for(final InstanceIdentifier<?> instanceId : deletedEntries){
                Map<InstanceIdentifier<? extends DataObject>,DataObject> originalConfigurationData = getModification().getOriginalConfigurationData();
                final DataObject node = originalConfigurationData.get(instanceId);
                publishNodeUpdate(instanceId, (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node) node, UpdateType.REMOVED);

            }

            Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
            return Rpcs.<Void>getRpcResult(true, null, _emptySet);

        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
            return Rpcs.<Void>getRpcResult(true, null, _emptySet);
        }



        private void publishNodeUpdate(final InstanceIdentifier<?> nodeId, final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node mdNode, final UpdateType updateType) {
            NodeRef nodeRef = new NodeRef(nodeId);
            FlowCapableNode fcn = mdNode.getAugmentation(FlowCapableNode.class);

            Node node = null;
            try {
                node = NodeMapping.toADNode(nodeRef);
            } catch (ConstructionException e) {
                e.printStackTrace();
            }

            HashSet<Property> _aDNodeProperties = NodeMapping.toADNodeProperties(fcn, mdNode.getId());
            this.publishNodeUpdate(node, updateType, _aDNodeProperties);

        }

        private void publishNodeUpdate(final Node node, final UpdateType updateType, final Set<Property> properties) {
          List<IPluginOutInventoryService> _inventoryPublisher = getInventoryPublisher();
          for (final IPluginOutInventoryService publisher : _inventoryPublisher) {
            publisher.updateNode(node, updateType, properties);
          }
        }

    }
}
