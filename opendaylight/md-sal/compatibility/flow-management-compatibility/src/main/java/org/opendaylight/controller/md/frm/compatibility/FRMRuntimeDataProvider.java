/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.frm.compatibility;

import java.util.Collections;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.common.util.Arguments;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class FRMRuntimeDataProvider implements RuntimeDataProvider, DataCommitHandler<InstanceIdentifier<? extends DataObject>,DataObject> {

    private final static InstanceIdentifier<Flows> FLOWS_PATH = InstanceIdentifier.<Flows> builder(Flows.class).toInstance();

    private final FlowManagementReader configuration = new ConfigurationReader();
    private DataChangeListener<InstanceIdentifier<? extends DataObject>, DataObject> changeListener;
    private DataProviderService dataService;
    private IForwardingRulesManager manager;

    public Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> init() {
        return this.dataService.registerCommitHandler(FRMRuntimeDataProvider.FLOWS_PATH, this);
    }

    @Override
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        return this.readFrom(this.configuration, path);
    }

    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        return this.readFrom(this.configuration, path);
    }

    public DataObject readFrom(final FlowManagementReader store, final InstanceIdentifier<? extends DataObject> path) {
        if (Objects.equal(FRMRuntimeDataProvider.FLOWS_PATH, path)) {
            return store.readAllFlows();
        }
        if (FRMRuntimeDataProvider.FLOWS_PATH.contains(path)) {
            return store.readFlow(this.toFlowKey(path));
        }
        return null;
    }

    @Override
    public FlowCommitTransaction requestCommit(final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new FlowCommitTransaction(this, modification);
    }

    public FlowKey toFlowKey(final InstanceIdentifier<? extends DataObject> identifier) {
        Preconditions.<InstanceIdentifier<? extends DataObject>> checkNotNull(identifier);

        Iterable<PathArgument> path = identifier.getPathArguments();
        PathArgument get = path.iterator().next();
        final Identifier itemKey = Arguments.<IdentifiableItem> checkInstanceOf(get, IdentifiableItem.class).getKey();
        return Arguments.<FlowKey> checkInstanceOf(itemKey, FlowKey.class);
    }

    public RpcResult<Void> finish(final FlowCommitTransaction transaction) {
        Iterable<FlowConfig> toRemove = transaction.getToRemove();
        for (final FlowConfig flow : toRemove) {
            this.manager.removeStaticFlow(flow.getName(), flow.getNode());
        }
        Iterable<FlowConfig> toUpdate = transaction.getToUpdate();
        for (final FlowConfig flow : toUpdate) {
            this.manager.removeStaticFlow(flow.getName(), flow.getNode());
            this.manager.addStaticFlow(flow);
        }
        return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
    }

    public RpcResult<Void> rollback(final FlowCommitTransaction transaction) {
        return null;
    }

    public DataProviderService getDataService() {
        return this.dataService;
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    public DataChangeListener<InstanceIdentifier<? extends DataObject>, DataObject> getChangeListener() {
        return this.changeListener;
    }

    public void setChangeListener(final DataChangeListener<InstanceIdentifier<? extends DataObject>, DataObject> changeListener) {
        this.changeListener = changeListener;
    }

    public IForwardingRulesManager getManager() {
        return this.manager;
    }

    public void setManager(final IForwardingRulesManager manager) {
        this.manager = manager;
    }
}
