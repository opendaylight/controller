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
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * AbstractChangeListner implemented basic DataChangeEvent processing for
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public abstract class AbstractChangeListener implements DataChangeListener {

    private final AtomicLong txNum = new AtomicLong();
    private String transactionId;

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        this.transactionId = this.newTransactionIdentifier().toString();
        /* All DataObjects for create */
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries =
                changeEvent.getCreatedConfigurationData().entrySet();
        /* All DataObjects for updates - init HashSet */
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries =
                new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
        /* Filtered DataObject for update processing only */
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updateConfigEntrySet =
                changeEvent.getUpdatedConfigurationData().entrySet();
        updatedEntries.addAll(updateConfigEntrySet);
        updatedEntries.removeAll(createdEntries);
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers =
                changeEvent.getRemovedConfigurationData();
        /* Create DataObject processing (send to device) */
        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends DataObject> c_key = createdEntry.getKey();
            DataObject c_value = createdEntry.getValue();

            if (this.isNodeAvaliable(c_key)) {
                this.add(c_key, c_value);
            }
        }

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntrie : updatedEntries) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData =
                    changeEvent.getOriginalConfigurationData();

            InstanceIdentifier<? extends Object> u_key = updatedEntrie.getKey();
            final DataObject originalFlow = origConfigData.get(u_key);
            final DataObject updatedFlow = updatedEntrie.getValue();
            if (this.isNodeAvaliable(u_key)) {
                this.update(u_key, originalFlow, updatedFlow);
            }
        }

        for (final InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData =
                    changeEvent.getOriginalConfigurationData();

            final DataObject removeValue = origConfigData.get(instanceId);
            if (this.isNodeAvaliable(instanceId)) {
                this.remove(instanceId, removeValue);
            }
        }
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    private Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    /**
     * Method checks the node data path in DataStore/operational because
     * without the Node Identifier in DataStore/operational, device
     * is not connected and device pre-configuration is allowed only.
     *
     * @param identifier
     * @return
     */
    protected abstract boolean isNodeAvaliable(
            final InstanceIdentifier<? extends DataObject> identifier);

    /**
     * Method removes DataObject which is identified by InstanceIdentifier
     * from device.
     *
     * @param InstanceIdentifier identifier
     * @param DataObject remove
     */
    protected abstract void remove(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject remove);

    /**
     * Method updates the original DataObject to the update DataObject
     * in device. Both are identified by same InstanceIdentifier
     *
     * @param identifier
     * @param original
     * @param update
     */
    protected abstract void update(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject original, final DataObject update);

    /**
     * Method adds the DataObject which is identified by InstanceIdentifier
     * to device.
     *
     * @param identifier
     * @param add
     */
    protected abstract void add(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject add);
}
