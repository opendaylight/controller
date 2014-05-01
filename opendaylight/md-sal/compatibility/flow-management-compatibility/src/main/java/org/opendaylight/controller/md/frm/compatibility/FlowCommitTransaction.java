/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.frm.compatibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class FlowCommitTransaction implements DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;
    private final HashSet<FlowConfig> toAdd = new HashSet<FlowConfig>();
    private final FRMRuntimeDataProvider flowManager;
    private Iterable<FlowConfig> toUpdate;
    private Iterable<FlowConfig> toRemove;

    public FlowCommitTransaction(
            final FRMRuntimeDataProvider flowManager,
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        this.flowManager = flowManager;
        this.modification = modification;
        this.processModification();
    }

    @Override
    public RpcResult<Void> finish() throws IllegalStateException {
        return this.flowManager.finish(this);
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        return this.flowManager.rollback(this);
    }

    public void processModification() {
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updated =
                this.modification.getUpdatedConfigurationData().entrySet();
        final List<FlowConfig> forUpdate = new ArrayList<FlowConfig>(updated.size());

        if (updated != null && !(updated.isEmpty())) {
            for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : updated) {
                if (FlowConfigMapping.isFlowPath(entry.getKey())) {
                    forUpdate.add(FlowConfigMapping.toFlowConfig((Flow) entry.getValue()));
                }
            }
        }
        this.toUpdate = Collections.unmodifiableCollection(forUpdate);

        final Set<InstanceIdentifier<? extends DataObject>> removedConfigurationData =
                this.modification.getRemovedConfigurationData();
        final List<FlowConfig> forRemove = new ArrayList<FlowConfig>(removedConfigurationData.size());

        if (removedConfigurationData != null && !(removedConfigurationData.isEmpty())) {
            for (InstanceIdentifier<? extends DataObject> data : removedConfigurationData) {
                if (FlowConfigMapping.isFlowPath(data)) {
                    forRemove.add(FlowConfigMapping.toFlowConfig(data));
                }
            }
        }
        this.toRemove = Collections.unmodifiableCollection(forRemove);
    }

    @Override
    public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
        return this.modification;
    }

    public FRMRuntimeDataProvider getFlowManager() {
        return this.flowManager;
    }

    public HashSet<FlowConfig> getToAdd() {
        return this.toAdd;
    }

    public Iterable<FlowConfig> getToUpdate() {
        return this.toUpdate;
    }

    public Iterable<FlowConfig> getToRemove() {
        return this.toRemove;
    }
}
