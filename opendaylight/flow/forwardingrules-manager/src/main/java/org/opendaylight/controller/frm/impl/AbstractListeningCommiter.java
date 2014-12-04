/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.frm.ForwardingRulesCommiter;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * AbstractChangeListner implemented basic {@link AsyncDataChangeEvent} processing for
 * flow node subDataObject (flows, groups and meters).
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public abstract class AbstractListeningCommiter <T extends DataObject> implements ForwardingRulesCommiter<T> {

    protected ForwardingRulesManager provider;

    protected final Class<T> clazz;

    public AbstractListeningCommiter (ForwardingRulesManager provider, Class<T> clazz) {
        this.provider = Preconditions.checkNotNull(provider, "ForwardingRulesManager can not be null!");
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
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

    /**
     * Method return wildCardPath for Listener registration
     * and for identify the correct KeyInstanceIdentifier from data;
     */
    protected abstract InstanceIdentifier<T> getWildCardPath();



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

    @SuppressWarnings("unchecked")
    private void updateData(final Map<InstanceIdentifier<?>, DataObject> updateData,
            final Map<InstanceIdentifier<?>, DataObject> originalData) {

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

    @SuppressWarnings("unchecked")
    private void removeData(final Set<InstanceIdentifier<?>> removeData,
            final Map<InstanceIdentifier<?>, DataObject> originalData) {

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

    private boolean preConfigurationCheck(final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        Preconditions.checkNotNull(nodeIdent, "FlowCapableNode ident can not be null!");
        return provider.isNodeActive(nodeIdent);
    }
}

