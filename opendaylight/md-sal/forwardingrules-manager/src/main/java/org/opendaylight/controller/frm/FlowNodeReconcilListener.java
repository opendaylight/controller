/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.reconcil.rev140616.NodeReconcilAdd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.reconcil.rev140616.NodeReconcilDel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.reconcil.rev140616.SalNodeReconciliationListener;
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

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 13, 2014
 */
class FlowNodeReconcilListener implements SalNodeReconciliationListener {

    private final FlowNodeReconcilProvider provider;

    public FlowNodeReconcilListener(final FlowNodeReconcilProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Flow Node Reconcil Provider can not be null!");
        }
        this.provider = provider;
    }

    @Override
    public void onNodeReconcilAdd(final NodeReconcilAdd reconcilNotif) {
        final NodeRef nodeRef = reconcilNotif.getNodeRef();
        final InstanceIdentifier<Node> nodeIdent = (InstanceIdentifier<Node>) nodeRef.getValue();

        DataModificationTransaction modifTran = this.provider.startChange();
        DataObject nodeData = modifTran.readConfigurationData(nodeIdent);

        if (nodeData != null && nodeData instanceof Node) {
            final Node node = (Node) nodeData;
            final FlowCapableNode flowCapNode = node.getAugmentation(FlowCapableNode.class);
            final InstanceIdentifier<FlowCapableNode> flowNodeIdent =
                    nodeIdent.augmentation(FlowCapableNode.class);

            /* Groups - have to be first */
            for (Group group : flowCapNode.getGroup()) {
                final AddGroupInputBuilder groupBuilder = new AddGroupInputBuilder(group);
                groupBuilder.setNode(reconcilNotif.getNodeRef());
                groupBuilder.setGroupRef(new GroupRef(flowNodeIdent.child(Group.class, group.getKey())));

                this.provider.getSalGroupService().addGroup(groupBuilder.build());
            }

            /* Meters */
            for (Meter meter : flowCapNode.getMeter()) {
                final AddMeterInputBuilder meterBuilder = new AddMeterInputBuilder(meter);
                meterBuilder.setNode(nodeRef);
                meterBuilder.setMeterRef(new MeterRef(flowNodeIdent.child(Meter.class, meter.getKey())));

                this.provider.getSalMeterService().addMeter(meterBuilder.build());
            }

            /* Flows */
            for (Table flowTable : flowCapNode.getTable()) {
                final InstanceIdentifier<Table> tableInstanceId =
                        flowNodeIdent.child(Table.class, flowTable.getKey());
                for (Flow flow : flowTable.getFlow()) {
                    FlowCookie flowCookie = new FlowCookie(FlowCookieManager.INSTANCE.getNewCookie(tableInstanceId));
                    AddFlowInputBuilder flowBuilder = new AddFlowInputBuilder(flow);
                    flowBuilder.setCookie(flowCookie);
                    flowBuilder.setNode(nodeRef);
                    flowBuilder.setFlowTable(new FlowTableRef(tableInstanceId));
                    flowBuilder.setFlowRef(new FlowRef(tableInstanceId.child(Flow.class, flow.getKey())));

                    FlowCookieManager.flowCookieMapUpdate(this.provider.getDataService(),
                            flowCookie, flow.getId(), tableInstanceId);
                    this.provider.getSalFlowService().addFlow(flowBuilder.build());
                }
            }
        }
    }

    @Override
    public void onNodeReconcilDel(NodeReconcilDel reconcilNotif) {
        final NodeRef nodeRef = reconcilNotif.getNodeRef();
        final InstanceIdentifier<Node> nodeIdent = (InstanceIdentifier<Node>) nodeRef.getValue();

        DataModificationTransaction modifTran = this.provider.startChange();
        DataObject nodeData = modifTran.readConfigurationData(nodeIdent);

        if (nodeData != null && nodeData instanceof Node) {
            final Node node = (Node) nodeData;
            final FlowCapableNode flowCapNode = node.getAugmentation(FlowCapableNode.class);
            final InstanceIdentifier<FlowCapableNode> flowNodeIdent =
                    nodeIdent.augmentation(FlowCapableNode.class);
            /* Cookie Key Map cleaning */
            for (Table flowTable : flowCapNode.getTable()) {
                final InstanceIdentifier<Table> tableInstanceId =
                        flowNodeIdent.child(Table.class, flowTable.getKey());
                FlowCookieManager.INSTANCE.clean(tableInstanceId);
            }
        }
    }
}
