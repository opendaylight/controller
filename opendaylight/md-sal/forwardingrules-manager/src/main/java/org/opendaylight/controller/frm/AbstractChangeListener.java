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
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
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

        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = 
                changeEvent.getCreatedConfigurationData().entrySet();
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = 
                new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();

        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updateConfigEntrySet = 
                changeEvent.getUpdatedConfigurationData().entrySet();
        updatedEntries.addAll(updateConfigEntrySet);
        updatedEntries.removeAll(createdEntries);

        final Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = 
                changeEvent.getRemovedConfigurationData();

        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends DataObject> c_key = createdEntry.getKey();
            DataObject c_value = createdEntry.getValue();
            this.add(c_key, c_value);
        }

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntrie : updatedEntries) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    changeEvent.getOriginalConfigurationData();

            InstanceIdentifier<? extends Object> u_key = updatedEntrie.getKey();
            final DataObject originalFlow = origConfigData.get(u_key);
            final DataObject updatedFlow = updatedEntrie.getValue();
            this.update(u_key, originalFlow, updatedFlow);
        }

        for (final InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    changeEvent.getOriginalConfigurationData();

            final DataObject removeValue = origConfigData.get(instanceId);
            this.remove(instanceId, removeValue);
        }
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    private Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    protected abstract void validate() throws IllegalStateException;

    protected abstract void remove(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject remove);

    protected abstract void update(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject original, final DataObject update);

    protected abstract void add(
            final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject add);
}
