package org.opendaylight.controller.sal.compatibility;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public abstract class AbstractDataChangeListener <T extends DataObject> implements AutoCloseable,DataChangeListener{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataChangeListener.class);
    protected InventoryAndReadAdapter adapter;
    private Class<T> clazz;

    public AbstractDataChangeListener(final InventoryAndReadAdapter adapter, final DataBroker db) {
        this.adapter = Preconditions.checkNotNull(adapter, "InventoryAndReadAdapter can not be null!");
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");
        /* All DataObjects for create */
        final Map<InstanceIdentifier<?>, DataObject> createdData = changeEvent.getCreatedData() != null
                ? changeEvent.getCreatedData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
                ? changeEvent.getRemovedPaths() : Collections.<InstanceIdentifier<?>> emptySet();
        /* All DataObjects for updates */
        final Map<InstanceIdentifier<?>, DataObject> updateData = changeEvent.getUpdatedData() != null
                ? changeEvent.getUpdatedData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
        /* All Original DataObjects */
        final Map<InstanceIdentifier<?>, DataObject> originalData = changeEvent.getOriginalData() != null
                ? changeEvent.getOriginalData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
        this.createData(createdData);
        this.updateData(updateData, originalData);
        this.removeData(removeData, originalData);
    }

    @SuppressWarnings("unchecked")
    private void createData(final Map<InstanceIdentifier<?>, DataObject> createdData) {
        final Set<InstanceIdentifier<?>> keys = createdData.keySet() != null
                ? createdData.keySet() : Collections.<InstanceIdentifier<?>> emptySet();
        for (InstanceIdentifier<?> key : keys) {
            if (clazz.equals(key.getTargetType())) {
                final InstanceIdentifier<FlowCapableNode> nodeIdent =
                        key.firstIdentifierOf(FlowCapableNode.class);
                if (preConfigurationCheck(nodeIdent)) {
                    InstanceIdentifier<T> createKeyIdent = key.firstIdentifierOf(clazz);
                    final Optional<DataObject> value = Optional.of(createdData.get(key));
                    if (value.isPresent()) {
                        this.add(createKeyIdent, (T)value.get(), nodeIdent);
                    }
                }
            }
        }
    }

    abstract protected void add(InstanceIdentifier<T> createKeyIdent, T node,
            InstanceIdentifier<FlowCapableNode> nodeIdent);

    @SuppressWarnings("unchecked")
    private void updateData(final Map<InstanceIdentifier<?>, DataObject> updateData, final Map<InstanceIdentifier<?>, DataObject> originalData) {

        final Set<InstanceIdentifier<?>> keys = updateData.keySet() != null
                ? updateData.keySet() : Collections.<InstanceIdentifier<?>> emptySet();
        for (InstanceIdentifier<?> key : keys) {
            if (clazz.equals(key.getTargetType())) {
                final InstanceIdentifier<FlowCapableNode> nodeIdent =
                        key.firstIdentifierOf(FlowCapableNode.class);
                if (preConfigurationCheck(nodeIdent)) {
                    InstanceIdentifier<T> updateKeyIdent = key.firstIdentifierOf(clazz);
                    final Optional<DataObject> value = Optional.of(updateData.get(key));
                    final Optional<DataObject> original = Optional.of(originalData.get(key));
                    if (value.isPresent() && original.isPresent()) {
                        this.update(updateKeyIdent, (T)original.get(), (T)value.get(), nodeIdent);
                    }
                }
            }
        }
    }

    abstract protected void update(InstanceIdentifier<T> updateKeyIdent, T node,
            T node2, InstanceIdentifier<FlowCapableNode> nodeIdent);

    @SuppressWarnings("unchecked")
    private void removeData(final Set<InstanceIdentifier<?>> removeData, final Map<InstanceIdentifier<?>, DataObject> originalData) {

        for (InstanceIdentifier<?> key : removeData) {
            if (clazz.equals(key.getTargetType())) {
                final InstanceIdentifier<FlowCapableNode> nodeIdent =
                        key.firstIdentifierOf(FlowCapableNode.class);
                if (preConfigurationCheck(nodeIdent)) {
                    final InstanceIdentifier<T> ident = key.firstIdentifierOf(clazz);
                    final DataObject removeValue = originalData.get(key);
                    this.remove(ident, (T)removeValue, nodeIdent);
                }
            }
        }
    }

    abstract protected void remove(InstanceIdentifier<T> ident, T removeValue,
            InstanceIdentifier<FlowCapableNode> nodeIdent);

    private boolean preConfigurationCheck(final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        return nodeIdent != null;
    }
}