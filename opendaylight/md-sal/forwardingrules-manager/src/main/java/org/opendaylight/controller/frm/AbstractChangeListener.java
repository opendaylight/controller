/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractChangeListner implemented basic {@link AsyncDataChangeEvent} processing for
 * flow node subDataObject (flows, groups and meters).
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public abstract class AbstractChangeListener implements DataChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(AbstractChangeListener.class);

    private final AtomicLong txNum = new AtomicLong();
    private String transactionId;

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        this.transactionId = this.newTransactionIdentifier().toString();
        /* All DataObjects for create */
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries =
                changeEvent.getCreatedData().entrySet();
        /* All DataObjects for updates - init HashSet */
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<>();
        /* Filtered DataObject for update processing only */
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updateConfigEntrySet =
                changeEvent.getUpdatedData().entrySet();
        updatedEntries.addAll(updateConfigEntrySet);
        updatedEntries.removeAll(createdEntries);
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers =
                changeEvent.getRemovedPaths();
        /* Create DataObject processing (send to device) */
        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends DataObject> entryKey = createdEntry.getKey();
            DataObject entryValue = createdEntry.getValue();
            if (preconditionForChange(entryKey, entryValue, null)) {
                this.add(entryKey, entryValue);
            }
        }

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntrie : updatedEntries) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData =
                    changeEvent.getOriginalData();

            InstanceIdentifier<? extends Object> entryKey = updatedEntrie.getKey();
            final DataObject original = origConfigData.get(entryKey);
            final DataObject updated = updatedEntrie.getValue();
            if (preconditionForChange(entryKey, original, updated)) {
                this.update(entryKey, original, updated);
            }
        }

        for (final InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData =
                    changeEvent.getOriginalData();

            final DataObject removeValue = origConfigData.get(instanceId);
            if (preconditionForChange(instanceId, removeValue, null)) {
                this.remove(instanceId, removeValue);
            }
        }
    }

    /**
     * Method returns generated transaction ID, which is unique for
     * every transaction. ID is composite from prefix ("DOM") and unique number.
     *
     * @return String transactionID
     */
    public String getTransactionId() {
        return this.transactionId;
    }

    private Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    /**
     * Method check all local preconditions for apply relevant changes.
     *
     * @param InstanceIdentifier identifier - the whole path to DataObject
     * @param DataObject original - original DataObject (for update)
     *                              or relevant DataObject (add/delete operations)
     * @param DataObject update - changed DataObject (contain updates)
     *                              or should be null for (add/delete operations)
     *
     * @return boolean - applicable
     */
    protected abstract boolean preconditionForChange(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject original, final DataObject update);

    /**
     * Method checks the node data path in DataStore/OPERATIONAL because
     * without the Node Identifier in DataStore/OPERATIONAL, device
     * is not connected and device pre-configuration is allowed only.
     *
     * @param InstanceIdentifier identifier - could be whole path to DataObject,
     *            but parent Node.class InstanceIdentifier is used for a check only
     *
     * @return boolean - is the Node available in DataStore/OPERATIONAL (is connected)
     */
    protected boolean isNodeAvailable(final InstanceIdentifier<? extends DataObject> identifier,
            final ReadOnlyTransaction readTrans) {
        final InstanceIdentifier<Node> nodeInstanceId = identifier.firstIdentifierOf(Node.class);
        try {
            return readTrans.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceId).get().isPresent();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected exception by reading Node ".concat(nodeInstanceId.toString()), e);
            return false;
        }
        finally {
            readTrans.close();
        }
    }

    /**
     * Method removes DataObject which is identified by InstanceIdentifier
     * from device.
     *
     * @param InstanceIdentifier identifier - the whole path to DataObject
     * @param DataObject remove - DataObject for removing
     */
    protected abstract void remove(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject remove);

    /**
     * Method updates the original DataObject to the update DataObject
     * in device. Both are identified by same InstanceIdentifier
     *
     * @param InstanceIdentifier identifier - the whole path to DataObject
     * @param DataObject original - original DataObject (for update)
     * @param DataObject update - changed DataObject (contain updates)
     */
    protected abstract void update(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject original, final DataObject update);

    /**
     * Method adds the DataObject which is identified by InstanceIdentifier
     * to device.
     *
     * @param InstanceIdentifier identifier - the whole path to new DataObject
     * @param DataObject add - new DataObject
     */
    protected abstract void add(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject add);
}
