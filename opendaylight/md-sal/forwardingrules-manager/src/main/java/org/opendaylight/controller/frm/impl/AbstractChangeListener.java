/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.frm.ForwardingRulesCommiter;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 * AbstractChangeListner implemented basic {@link AsyncDataChangeEvent} processing for
 * flow node subDataObject (flows, groups and meters).
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public abstract class AbstractChangeListener<T extends DataObject> implements ForwardingRulesCommiter<T> {

    protected ForwardingRulesManager provider;

    public AbstractChangeListener (ForwardingRulesManager provider) {
        this.provider = Preconditions.checkNotNull(provider, "ForwardingRulesManager can not be null!");
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");
        /* All DataObjects for create */
        final Set<Entry<InstanceIdentifier<?>, DataObject>> createdData = changeEvent.getCreatedData() != null
                ? changeEvent.getCreatedData().entrySet() : Collections.<Entry<InstanceIdentifier<?>, DataObject>> emptySet();
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
                ? changeEvent.getRemovedPaths() : Collections.<InstanceIdentifier<?>> emptySet();
        /* All DataObjects for updates */
        final Set<Entry<InstanceIdentifier<?>, DataObject>> updateData = changeEvent.getUpdatedData() != null
                ? changeEvent.getUpdatedData().entrySet() : Collections.<Entry<InstanceIdentifier<?>, DataObject>> emptySet();
        /* All Original DataObjects */
        final Map<InstanceIdentifier<?>, DataObject> originalData = changeEvent.getOriginalData() != null
                ? changeEvent.getOriginalData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();

        this.createData(createdData);
        this.removeData(removeData, originalData);
        this.updateData(updateData, originalData);
    }

    @SuppressWarnings("unchecked")
    private void removeData(final Set<InstanceIdentifier<?>> removeData,
            final Map<InstanceIdentifier<?>, DataObject> originalData) {

        for (final InstanceIdentifier<?> instanceId : removeData) {
            final InstanceIdentifier<FlowCapableNode> nodeIdent = instanceId.firstIdentifierOf(FlowCapableNode.class);
            if (preConfigurationCheck(nodeIdent)) {
                final DataObject removeValue = originalData.get(instanceId);
                this.remove((InstanceIdentifier<T>)instanceId, (T)removeValue, nodeIdent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createData(final Set<Entry<InstanceIdentifier<?>, DataObject>> createdData) {
        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdData) {
            final InstanceIdentifier<? extends DataObject> entryKey = createdEntry.getKey();
            final InstanceIdentifier<FlowCapableNode> nodeIdent = entryKey.firstIdentifierOf(FlowCapableNode.class);
            if (preConfigurationCheck(nodeIdent)) {
                DataObject entryValue = createdEntry.getValue();
                this.add((InstanceIdentifier<T>)entryKey, (T)entryValue, nodeIdent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateData(final Set<Entry<InstanceIdentifier<?>, DataObject>> updateData,
            final Map<InstanceIdentifier<?>, DataObject> originalData) {

        for (final Entry<InstanceIdentifier<?>, DataObject> updatedEntrie : updateData) {
            final InstanceIdentifier<? extends Object> entryKey = updatedEntrie.getKey();
            final InstanceIdentifier<FlowCapableNode> nodeIdent = entryKey.firstIdentifierOf(FlowCapableNode.class);
            if (preConfigurationCheck(nodeIdent)) {
                final DataObject original = originalData.get(entryKey);
                final DataObject updated = updatedEntrie.getValue();
                this.update((InstanceIdentifier<T>)entryKey, (T)original, (T)updated, nodeIdent);
            }
        }
    }

    private boolean preConfigurationCheck(final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        Preconditions.checkNotNull(nodeIdent, "FlowCapableNode ident can not be null!");
        return provider.isNodeActive(nodeIdent);
    }
}



//CheckedFuture<Void,TransactionCommitFailedException> future = writeTx.submit();
//
//Futures.addCallback(future, new FutureCallback<Void>() {
//
//    @Override
//    public void onSuccess(final Void result) {
//        // Commited successfully
//        // Nothing to do
//    }
//
//    @Override
//    public void onFailure(final Throwable t) {
//        // Transaction failed
//
//        if(t instanceof OptimisticLockFailedException) {
//            if( (tries - 1) > 0 ) {
//                LOG.debug("Concurrent modification of data - trying again");
//                readWriteRetry(tries - 1);
//            }
//            else {
//                LOG.error("Concurrent modification of data - out of retries",e);
//            }
//        }
//    }
//});

