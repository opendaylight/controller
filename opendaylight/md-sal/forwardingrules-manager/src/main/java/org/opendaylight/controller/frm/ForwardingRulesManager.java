/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm;

import org.opendaylight.controller.frm.impl.FRMConfig;
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
 * ForwardingRulesManager
 * It represent a central point for whole modul. Implementation
 * Flow Provider registers the link FlowChangeListener} and it holds all needed
 * services for link FlowChangeListener}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 25, 2014
 */
public interface ForwardingRulesManager extends AutoCloseable {

    public void start();

    /**
     * Method returns information :
     * "is Node with send InstanceIdentifier connected"?
     *
     * @param InstanceIdentifier<FlowCapableNode> ident - the key of the node
     * @return boolean - is device connected
     */
    public boolean isNodeActive(InstanceIdentifier<FlowCapableNode> ident);

    /**
     * Method add new {@link FlowCapableNode} to active Node Holder.
     * ActiveNodeHolder prevent unnecessary Operational/DS read for identify
     * pre-configure and serious Configure/DS transactions.
     *
     * @param InstanceIdentifier<FlowCapableNode> ident - the key of the node
     */
    public void registrateNewNode(InstanceIdentifier<FlowCapableNode> ident);

    /**
     * Method remove disconnected {@link FlowCapableNode} from active Node
     * Holder. And all next flows or groups or meters will stay in Config/DS
     * only.
     *
     * @param InstanceIdentifier<FlowCapableNode> ident - the key of the node
     */
    public void unregistrateNode(InstanceIdentifier<FlowCapableNode> ident);

    /**
     * Method returns generated transaction ID, which is unique for
     * every transaction. ID is composite from prefix ("DOM") and unique number.
     *
     * @return String transactionID for RPC transaction identification
     */
    public String getNewTransactionId();

    /**
     * Method returns Read Transacion. It is need for Node reconciliation only.
     *
     * @return ReadOnlyTransaction
     */
    public ReadOnlyTransaction getReadTranaction();

    /**
     * Flow RPC service
     *
     * @return
     */
    public SalFlowService getSalFlowService();

    /**
     * Group RPC service
     *
     * @return
     */
    public SalGroupService getSalGroupService();

    /**
     * Meter RPC service
     *
     * @return
     */
    public SalMeterService getSalMeterService();

    /**
     * Content definition method and prevent code duplicity in Reconcil
     * @return ForwardingRulesCommiter<Flow>
     */
    public ForwardingRulesCommiter<Flow> getFlowCommiter();

    /**
     * Content definition method and prevent code duplicity in Reconcil
     * @return ForwardingRulesCommiter<Group>
     */
    public ForwardingRulesCommiter<Group> getGroupCommiter();

    /**
     * Content definition method and prevent code duplicity
     * @return ForwardingRulesCommiter<Meter>
     */
    public ForwardingRulesCommiter<Meter> getMeterCommiter();

    /**
     * Content definition method
     * @return FlowNodeReconciliation
     */
    public FlowNodeReconciliation getFlowNodeReconciliation();

    /**
     * Returns configuration parameters for ForwardingRulesManager
     * @return FRM configuration
     */
    public FRMConfig getConfiguration();
}

