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
import org.opendaylight.controller.frm.FlowNodeReconciliation;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 * <p/>
 * FlowNode Reconciliation Listener
 * Reconciliation for a new FlowNode
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *         <p/>
 *         Created: Jun 13, 2014
 */
public class FlowNodeReconciliationImpl implements FlowNodeReconciliation {

    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeReconciliationImpl.class);

    private final ForwardingRulesManager provider;

    private ListenerRegistration notifListenerRegistration;

    public FlowNodeReconciliationImpl(final ForwardingRulesManager manager, NotificationProviderService notif) {
        this.provider = Preconditions.checkNotNull(manager, "ForwardingRulesManager can not be null!");
        this.notifListenerRegistration = notif.registerNotificationListener(this);
    }

    @Override
    public void close() {
        if (notifListenerRegistration != null) {
            try {
                notifListenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Error by stop FRM FlowNodeReconilListener.", e);
            }
            notifListenerRegistration = null;
        }
    }

    @Override
    public void flowNodeDisconnected(InstanceIdentifier<FlowCapableNode> disconnectedNode) {
        provider.unregistrateNode(disconnectedNode);
    }

    @Override
    public void flowNodeConnected(InstanceIdentifier<FlowCapableNode> connectedNode) {
        if (!provider.isNodeActive(connectedNode)) {
            provider.registrateNewNode(connectedNode);
            if (provider.getConfiguration().isCleanAlienFlowsOnReconcil()) {
                cleanUpAlienFlows(connectedNode);
            }
            reconciliation(connectedNode);
        }
    }

    private void cleanUpAlienFlows(final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        RemoveFlowInputBuilder rfiBuilder = new RemoveFlowInputBuilder();
        rfiBuilder.setBarrier(false);
        rfiBuilder.setNode(new NodeRef(nodeIdent));
        // setting tableId to 255 will delete flows from all tables
        rfiBuilder.setTableId((short) 255);
        provider.getSalFlowService().removeFlow(rfiBuilder.build());
    }

    private void reconciliation(final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        ReadOnlyTransaction trans = provider.getReadTranaction();
        Optional<FlowCapableNode> flowNode = Optional.absent();

        try {
            flowNode = trans.read(LogicalDatastoreType.CONFIGURATION, nodeIdent).get();
        } catch (Exception e) {
            LOG.error("Fail with read Config/DS for Node {} !", nodeIdent, e);
        }

        if (flowNode.isPresent()) {
            /* Groups - have to be first */
            List<Group> groups = flowNode.get().getGroup() != null
                    ? flowNode.get().getGroup() : Collections.<Group>emptyList();
            for (Group group : groups) {
                final KeyedInstanceIdentifier<Group, GroupKey> groupIdent =
                        nodeIdent.child(Group.class, group.getKey());
                this.provider.getGroupCommiter().add(groupIdent, group, nodeIdent);
            }
            /* Meters */
            List<Meter> meters = flowNode.get().getMeter() != null
                    ? flowNode.get().getMeter() : Collections.<Meter>emptyList();
            for (Meter meter : meters) {
                final KeyedInstanceIdentifier<Meter, MeterKey> meterIdent =
                        nodeIdent.child(Meter.class, meter.getKey());
                this.provider.getMeterCommiter().add(meterIdent, meter, nodeIdent);
            }
            /* Flows */
            List<Table> tables = flowNode.get().getTable() != null
                    ? flowNode.get().getTable() : Collections.<Table>emptyList();
            for (Table table : tables) {
                final KeyedInstanceIdentifier<Table, TableKey> tableIdent =
                        nodeIdent.child(Table.class, table.getKey());
                List<Flow> flows = table.getFlow() != null ? table.getFlow() : Collections.<Flow>emptyList();
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

    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved notification) {
        // NO-OP
    }

    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated notification) {
        // NO-OP
    }

    @Override
    public void onNodeRemoved(NodeRemoved notification) {
        if (notification.getNodeRef() != null && notification.getNodeRef().getValue() != null) {
            InstanceIdentifier<Node> nodeIdent = notification.getNodeRef().getValue().firstIdentifierOf(Node.class);
            if (nodeIdent != null && !nodeIdent.isWildcarded()) {
                InstanceIdentifier<FlowCapableNode> flowNodeIdent= nodeIdent.augmentation(FlowCapableNode.class);
                flowNodeDisconnected(flowNodeIdent);
            }
        }
    }

    @Override
    public void onNodeUpdated(NodeUpdated notification) {
        final FlowCapableNodeUpdated newFlowNode =
                notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (newFlowNode != null && newFlowNode.getSwitchFeatures() != null) {
            if (notification.getNodeRef() != null && notification.getNodeRef().getValue() != null) {
                InstanceIdentifier<Node> nodeIdent = notification.getNodeRef().getValue().firstIdentifierOf(Node.class);
                if (nodeIdent != null && !nodeIdent.isWildcarded()) {
                    InstanceIdentifier<FlowCapableNode> flowNodeIdent= nodeIdent.augmentation(FlowCapableNode.class);
                    flowNodeConnected(flowNodeIdent);
                }
            }
        }
    }
}

