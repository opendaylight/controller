/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm.reconil;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.controller.frm.FlowCookieProducer;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 * FlowNode Reconciliation Listener
 * Reconciliation for a new FlowNode
 * Remove CookieMapKey for removed FlowNode
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 13, 2014
 */
public class FlowNodeReconcilListener extends AbstractChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeReconcilListener.class);

    private final FlowNodeReconcilProvider provider;

    public FlowNodeReconcilListener(final FlowNodeReconcilProvider provider) {
        this.provider = Preconditions.checkNotNull(provider, "Flow Node Reconcil Provider can not be null!");
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        /* FlowCapableNode DataObjects for reconciliation */
        final Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries =
                changeEvent.getCreatedData().entrySet();
        /* FlowCapableNode DataObjects for clean FlowCookieHolder */
        final Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers =
                changeEvent.getRemovedPaths();
        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> createdEntry : createdEntries) {
            InstanceIdentifier<? extends DataObject> entryKey = createdEntry.getKey();
            DataObject entryValue = createdEntry.getValue();
            if (preconditionForChange(entryKey, entryValue, null)) {
                this.add(entryKey, entryValue);
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

    @Override
    /* Cleaning FlowCookieManager holder for all node tables */
    protected void remove(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject removeDataObj) {

        final InstanceIdentifier<FlowCapableNode> flowNodeIdent =
                identifier.firstIdentifierOf(FlowCapableNode.class);
        final FlowCapableNode flowNode = ((FlowCapableNode) removeDataObj);

        for (Table flowTable : flowNode.getTable()) {
            final InstanceIdentifier<Table> tableIdent =
                    flowNodeIdent.child(Table.class, flowTable.getKey());
            FlowCookieProducer.INSTANCE.clean(tableIdent);
        }
    }

    @Override
    /* Reconciliation by connect new FlowCapableNode */
    protected void add(final InstanceIdentifier<? extends DataObject> identifier,
                       final DataObject addDataObj) {

        final InstanceIdentifier<FlowCapableNode> flowNodeIdent =
                identifier.firstIdentifierOf(FlowCapableNode.class);
        final Optional<FlowCapableNode> flowCapNode = this.readFlowCapableNode(flowNodeIdent);

        if (flowCapNode.isPresent()) {
            final InstanceIdentifier<Node> nodeIdent = identifier.firstIdentifierOf(Node.class);
            final NodeRef nodeRef = new NodeRef(nodeIdent);
            /* Groups - have to be first */
            List<Group> groups = flowCapNode.get().getGroup();
            if(groups != null) {
                for (Group group : groups) {
                    final GroupRef groupRef = new GroupRef(flowNodeIdent.child(Group.class, group.getKey()));
                    final AddGroupInputBuilder groupBuilder = new AddGroupInputBuilder(group);
                    groupBuilder.setGroupRef(groupRef);
                    groupBuilder.setNode(nodeRef);
                    this.provider.getSalGroupService().addGroup(groupBuilder.build());
                }
            }
            /* Meters */
            List<Meter> meters = flowCapNode.get().getMeter();
            if(meters != null) {
                for (Meter meter : meters) {
                    final MeterRef meterRef = new MeterRef(flowNodeIdent.child(Meter.class, meter.getKey()));
                    final AddMeterInputBuilder meterBuilder = new AddMeterInputBuilder(meter);
                    meterBuilder.setMeterRef(meterRef);
                    meterBuilder.setNode(nodeRef);
                    this.provider.getSalMeterService().addMeter(meterBuilder.build());
                }
            }
            /* Flows */
            List<Table> tables = flowCapNode.get().getTable();
            if(tables != null) {
                for (Table flowTable : tables) {
                    final InstanceIdentifier<Table> tableIdent = flowNodeIdent.child(Table.class, flowTable.getKey());
                    List<Flow> flows = flowTable.getFlow();
                    if(flows != null) {
                        for (Flow flow : flows) {
                            final FlowCookie flowCookie = new FlowCookie(FlowCookieProducer.INSTANCE.getNewCookie(tableIdent));
                            final FlowRef flowRef = new FlowRef(tableIdent.child(Flow.class, flow.getKey()));
                            final FlowTableRef flowTableRef = new FlowTableRef(tableIdent);
                            final AddFlowInputBuilder flowBuilder = new AddFlowInputBuilder(flow);
                            flowBuilder.setCookie(flowCookie);
                            flowBuilder.setNode(nodeRef);
                            flowBuilder.setFlowTable(flowTableRef);
                            flowBuilder.setFlowRef(flowRef);
                            this.provider.getSalFlowService().addFlow(flowBuilder.build());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void update(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject original, final DataObject update) {
        // NOOP - Listener is registered for DataChangeScope.BASE only
    }

    @Override
    protected boolean preconditionForChange(final InstanceIdentifier<? extends DataObject> identifier,
                                            final DataObject dataObj, final DataObject update) {
        return (dataObj instanceof FlowCapableNode);
    }

    private Optional<FlowCapableNode> readFlowCapableNode(final InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
        ReadOnlyTransaction readTrans = this.provider.getDataService().newReadOnlyTransaction();
        try {
            ListenableFuture<Optional<FlowCapableNode>> confFlowNode =
                    readTrans.read(LogicalDatastoreType.CONFIGURATION, flowNodeIdent);
            if (confFlowNode.get().isPresent()) {
                return Optional.<FlowCapableNode> of(confFlowNode.get().get());
            } else {
                return Optional.absent();
            }
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected exception by reading flow ".concat(flowNodeIdent.toString()), e);
            return Optional.absent();
        }
        finally {
            readTrans.close();
        }
    }
}
