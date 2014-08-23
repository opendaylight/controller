/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 * Flow Provider registers the link FlowChangeListener} and it holds all needed
 * services for link FlowChangeListener}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 25, 2014
 */
public interface ForwardingRulesManager extends AutoCloseable {

    public void start();

    public boolean isNodeActive(InstanceIdentifier<FlowCapableNode> ident);

    public void registerNewNode(InstanceIdentifier<FlowCapableNode> ident);

    public void unregisterNode(InstanceIdentifier<FlowCapableNode> ident);

    /**
     * Method returns generated transaction ID, which is unique for
     * every transaction. ID is composite from prefix ("DOM") and unique number.
     *
     * @return String transactionID for RPC transaction identification
     */
    public String getNewTransactionId();

    public ReadOnlyTransaction getReadTranaction();

    public SalFlowService getSalFlowService();

    public SalGroupService getSalGroupService();

    public SalMeterService getSalMeterService();

    public ForwardingRulesCommiter<Flow> getFlowCommiter();

    public ForwardingRulesCommiter<Group> getGroupCommiter();

    public ForwardingRulesCommiter<Meter> getMeterCommiter();

    public FlowNodeReconciliation getFlowNodeReconciliation();
}

