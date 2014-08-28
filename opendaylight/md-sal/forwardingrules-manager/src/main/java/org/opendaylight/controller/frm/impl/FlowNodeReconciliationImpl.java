/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.frm.FlowNodeReconciliation;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 * FlowNode Reconciliation Listener
 * Reconciliation for a new FlowNode
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 13, 2014
 */
public class FlowNodeReconciliationImpl implements FlowNodeReconciliation {

    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeReconciliationImpl.class);

    private final ForwardingRulesManager provider;

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public FlowNodeReconciliationImpl (final ForwardingRulesManager manager, final DataBroker db) {
        this.provider = Preconditions.checkNotNull(manager, "ForwardingRulesManager can not be null!");
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        /* Build Path */
        InstanceIdentifier<FlowCapableNode> flowNodeWildCardIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class);
        this.listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                flowNodeWildCardIdentifier, FlowNodeReconciliationImpl.this, DataChangeScope.BASE);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Error by stop FRM FlowNodeReconilListener.", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");
        /* All DataObjects for create */
        final Set<InstanceIdentifier<?>>  createdData = changeEvent.getCreatedData() != null
                ? changeEvent.getCreatedData().keySet() : Collections.<InstanceIdentifier<?>> emptySet();
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
                ? changeEvent.getRemovedPaths() : Collections.<InstanceIdentifier<?>> emptySet();

        for (InstanceIdentifier<?> entryKey : removeData) {
            final InstanceIdentifier<FlowCapableNode> nodeIdent = entryKey
                    .firstIdentifierOf(FlowCapableNode.class);
            if ( ! nodeIdent.isWildcarded()) {
                flowNodeDisconnected(nodeIdent);
            }
        }
        for (InstanceIdentifier<?> entryKey : createdData) {
            final InstanceIdentifier<FlowCapableNode> nodeIdent = entryKey
                    .firstIdentifierOf(FlowCapableNode.class);
            if ( ! nodeIdent.isWildcarded()) {
                flowNodeConnected(nodeIdent);
            }
        }
    }

    @Override
    public void flowNodeDisconnected(InstanceIdentifier<FlowCapableNode> disconnectedNode) {
        provider.unregistrateNode(disconnectedNode);
    }

    @Override
    public void flowNodeConnected(InstanceIdentifier<FlowCapableNode> connectedNode) {
        if ( ! provider.isNodeActive(connectedNode)) {
            provider.registrateNewNode(connectedNode);
            reconciliation(connectedNode);
        }
    }

    private void reconciliation(final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        ReadOnlyTransaction trans = provider.getReadTranaction();
        Optional<FlowCapableNode> flowNode = Optional.absent();

        try {
            flowNode = trans.read(LogicalDatastoreType.CONFIGURATION, nodeIdent).get();
        }
        catch (Exception e) {
            LOG.error("Fail with read Config/DS for Node {} !", nodeIdent, e);
        }

        if (flowNode.isPresent()) {
            /* Groups - have to be first */
            List<Group> groups = flowNode.get().getGroup() != null
                    ? flowNode.get().getGroup() : Collections.<Group> emptyList();
            for (Group group : groups) {
                final KeyedInstanceIdentifier<Group, GroupKey> groupIdent =
                        nodeIdent.child(Group.class, group.getKey());
                this.provider.getGroupCommiter().add(groupIdent, group, nodeIdent);
            }
            /* Meters */
            List<Meter> meters = flowNode.get().getMeter() != null
                    ? flowNode.get().getMeter() : Collections.<Meter> emptyList();
            for (Meter meter : meters) {
                final KeyedInstanceIdentifier<Meter, MeterKey> meterIdent =
                        nodeIdent.child(Meter.class, meter.getKey());
                this.provider.getMeterCommiter().add(meterIdent, meter, nodeIdent);
            }
            /* Flows */
            List<Table> tables = flowNode.get().getTable() != null
                    ? flowNode.get().getTable() : Collections.<Table> emptyList();
            for (Table table : tables) {
                final KeyedInstanceIdentifier<Table, TableKey> tableIdent =
                        nodeIdent.child(Table.class, table.getKey());
                List<Flow> flows = table.getFlow() != null ? table.getFlow() : Collections.<Flow> emptyList();
                for (Flow flow : flows) {
                    final KeyedInstanceIdentifier<Flow, FlowKey> flowIdent =
                            tableIdent.child(Flow.class, flow.getKey());
                    this.provider.getFlowCommiter().add(flowIdent, flow, nodeIdent);
                }
            }
        }
        /* clean transaction */
        trans.close();
    }
}

