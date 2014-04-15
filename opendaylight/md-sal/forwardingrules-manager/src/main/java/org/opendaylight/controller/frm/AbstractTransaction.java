/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Deprecated
/**
 * @deprecated please use {@link AbstractChangeListener}
 */
public abstract class AbstractTransaction implements DataCommitTransaction<InstanceIdentifier<? extends DataObject>,DataObject> {

    private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

    @Deprecated
    /**
     * @deprecated please use {@link AbstractChangeListener}
     */
    public AbstractTransaction(final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        this.modification = modification;
    }

    public abstract void validate() throws IllegalStateException;
    
    public RpcResult<Void> finish() throws IllegalStateException {
        this.validate();
        this.callRpcs();
        return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
    }

    public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
        return this.modification;
    }

    public RpcResult<Void> rollback() throws IllegalStateException {
        this.rollbackRpcs();
        return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
    }

    private void callRpcs() {
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = 
                this.modification.getCreatedConfigurationData().entrySet();
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = 
                new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();

        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updateConfigEntrySet = 
                this.modification.getUpdatedConfigurationData().entrySet();
        updatedEntries.addAll(updateConfigEntrySet);
        updatedEntries.removeAll(createdEntries);

        final Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = 
                this.modification.getRemovedConfigurationData();

        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends DataObject> c_key = createdEntry.getKey();
            DataObject c_value = createdEntry.getValue();
            this.add(c_key, c_value);
        }

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntrie : updatedEntries) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    this.modification.getOriginalConfigurationData();

            InstanceIdentifier<? extends Object> u_key = updatedEntrie.getKey();
            final DataObject originalFlow = origConfigData.get(u_key);
            final DataObject updatedFlow = updatedEntrie.getValue();
            this.update(u_key, originalFlow, updatedFlow);
        }

        for (final InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    this.modification.getOriginalConfigurationData();

            final DataObject removeValue = origConfigData.get(instanceId);
            this.remove(instanceId, removeValue);
        }
    }
    
    public abstract void remove(
            final InstanceIdentifier<? extends Object> identifier,
            final DataObject remove);
    
    public abstract void update(
            final InstanceIdentifier<? extends Object> identifier,
            final DataObject original, final DataObject update);
    
    public abstract void add(
            final InstanceIdentifier<? extends Object> identifier,
            final DataObject add);
    
    private void rollbackRpcs() {
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = 
                this.modification.getCreatedConfigurationData().entrySet();
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = 
                new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();

        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updateConfigEntrySet = 
                this.modification.getUpdatedConfigurationData().entrySet();
        updatedEntries.addAll(updateConfigEntrySet);
        updatedEntries.removeAll(createdEntries);

        final Set<InstanceIdentifier<?>> removeEntriesInstanceIdentifiers = 
                this.modification.getRemovedConfigurationData();

        for (final Entry<InstanceIdentifier<?>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends Object> c_key = createdEntry.getKey();
            DataObject c_value = createdEntry.getValue();
            this.remove(c_key, c_value);
        }

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntry : updatedEntries) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    this.modification.getOriginalConfigurationData();
            InstanceIdentifier<? extends Object> key = updatedEntry.getKey();
            final DataObject originalFlow = origConfigData.get(key);
            final DataObject updatedFlow = updatedEntry.getValue();
            this.update(key, updatedFlow, originalFlow);
        }

        for (final InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> origConfigData = 
                    this.modification.getOriginalConfigurationData();
            final DataObject removeValue = origConfigData.get(instanceId);
            this.add(instanceId, removeValue);
        }
    }
}
