/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm

import java.util.Collections
import java.util.HashSet
import java.util.Map.Entry
import java.util.Set
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.common.util.Rpcs
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.RpcError

abstract class AbstractTransaction implements DataCommitTransaction<InstanceIdentifier<?extends DataObject>, DataObject> {
        
    @Property
    val DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        _modification = modification;
    }
    
    def void validate() throws IllegalStateException
    
    override finish() throws IllegalStateException {
        validate()
        callRpcs();
        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());     
    }
    
    override getModification() {
        return _modification;
    }
    
    override rollback() throws IllegalStateException {
        rollbackRpcs();
        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
    }
    
    def private callRpcs() {
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = _modification.getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and modified entries
         * The reason I created a new HashSet is because the collections we are returned are immutable.
         */
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
        updatedEntries.addAll(_modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        val Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = _modification.getRemovedConfigurationData();
        for (Entry<InstanceIdentifier<? extends DataObject >, DataObject> entry : createdEntries) {
            add(entry.key,entry.value);
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
                val originalFlow = _modification.originalConfigurationData.get(entry.key);
                val updatedFlow = entry.value
                update(entry.key, originalFlow ,updatedFlow);
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
            val removeValue = _modification.getOriginalConfigurationData.get(instanceId);
                remove(instanceId,removeValue);
        }
    }
    
    def void remove(InstanceIdentifier<?> identifier, DataObject remove)
    
    def void update(InstanceIdentifier<?> identifier, DataObject original, DataObject update)
    
    def void add(InstanceIdentifier<?> identifier, DataObject add)
    
    def private rollbackRpcs() {
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = _modification.getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and modified entries
         * The reason I created a new HashSet is because the collections we are returned are immutable.
         */
        val Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
        updatedEntries.addAll(_modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        val Set<InstanceIdentifier<? >> removeEntriesInstanceIdentifiers = _modification.getRemovedConfigurationData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
            remove(entry.key,entry.value); // because we are rolling back, remove what we would have added.            
        }
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            val originalFlow = _modification.originalConfigurationData.get(entry.key);
            val updatedFlow = entry.value
            update(entry.key, updatedFlow ,originalFlow);// because we are rolling back, replace the updated with the original
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
            val removeValue = _modification.getOriginalConfigurationData.get(instanceId);
            add(instanceId,removeValue);// because we are rolling back, add what we would have removed.
        }
    }    
}
