/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.opendaylight.controller.frm.FlowNodeReconciliation;
import org.opendaylight.controller.frm.ForwardingRulesCommiter;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm.impl
 *
 * Manager and middle point for whole module.
 * It contains ActiveNodeHolder and provide all RPC services.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 25, 2014
 */
public class ForwardingRulesManagerImpl implements ForwardingRulesManager {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardingRulesManagerImpl.class);

    private final AtomicLong txNum = new AtomicLong();
    private final Object lockObj = new Object();
    private final FRMConfig frmConfig;
    private Set<InstanceIdentifier<FlowCapableNode>> activeNodes = Collections.emptySet();

    private final DataBroker dataService;
    private final NotificationProviderService notificationsService;
    private final SalFlowService salFlowService;
    private final SalGroupService salGroupService;
    private final SalMeterService salMeterService;

    private ForwardingRulesCommiter<Flow> flowListener;
    private ForwardingRulesCommiter<Group> groupListener;
    private ForwardingRulesCommiter<Meter> meterListener;
    private FlowNodeReconciliation nodeListener;

    public ForwardingRulesManagerImpl(final DataBroker dataBroker, final RpcConsumerRegistry rpcRegistry,
                                      NotificationProviderService notificationsService, FRMConfig frmConfig) {
        this.dataService = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
        this.notificationsService = Preconditions.checkNotNull(notificationsService);
        this.frmConfig = frmConfig;

        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");

        this.salFlowService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalFlowService.class),
                "RPC SalFlowService not found.");
        this.salGroupService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalGroupService.class),
                "RPC SalGroupService not found.");
        this.salMeterService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalMeterService.class),
                "RPC SalMeterService not found.");
    }

    @Override
    public void start() {
        this.flowListener = new FlowForwarder(this, dataService);
        this.groupListener = new GroupForwarder(this, dataService);
        this.meterListener = new MeterForwarder(this, dataService);
        this.nodeListener = new FlowNodeReconciliationImpl(this, notificationsService);
        LOG.info("ForwardingRulesManager has started successfull.");
    }

    @Override
    public void close() throws Exception {
        if(this.flowListener != null) {
            this.flowListener.close();
            this.flowListener = null;
        }
        if (this.groupListener != null) {
            this.groupListener.close();
            this.groupListener = null;
        }
        if (this.meterListener != null) {
            this.meterListener.close();
            this.meterListener = null;
        }
        if (this.nodeListener != null) {
            this.nodeListener.close();
            this.nodeListener = null;
        }
    }

    @Override
    public ReadOnlyTransaction getReadTranaction() {
        return dataService.newReadOnlyTransaction();
    }

    @Override
    public String getNewTransactionId() {
        return "DOM-" + txNum.getAndIncrement();
    }

    @Override
    public boolean isNodeActive(InstanceIdentifier<FlowCapableNode> ident) {
        return activeNodes.contains(ident);
    }

    @Override
    public void registrateNewNode(InstanceIdentifier<FlowCapableNode> ident) {
        if ( ! activeNodes.contains(ident)) {
            synchronized (lockObj) {
                if ( ! activeNodes.contains(ident)) {
                    Set<InstanceIdentifier<FlowCapableNode>> set =
                            Sets.newHashSet(activeNodes);
                    set.add(ident);
                    activeNodes = Collections.unmodifiableSet(set);
                }
            }
        }
    }

    @Override
    public void unregistrateNode(InstanceIdentifier<FlowCapableNode> ident) {
        if (activeNodes.contains(ident)) {
            synchronized (lockObj) {
                if (activeNodes.contains(ident)) {
                    Set<InstanceIdentifier<FlowCapableNode>> set =
                            Sets.newHashSet(activeNodes);
                    set.remove(ident);
                    activeNodes = Collections.unmodifiableSet(set);
                }
            }
        }
    }

    @Override
    public SalFlowService getSalFlowService() {
        return salFlowService;
    }

    @Override
    public SalGroupService getSalGroupService() {
        return salGroupService;
    }

    @Override
    public SalMeterService getSalMeterService() {
        return salMeterService;
    }

    @Override
    public ForwardingRulesCommiter<Flow> getFlowCommiter() {
        return flowListener;
    }

    @Override
    public ForwardingRulesCommiter<Group> getGroupCommiter() {
        return groupListener;
    }

    @Override
    public ForwardingRulesCommiter<Meter> getMeterCommiter() {
        return meterListener;
    }

    @Override
    public FlowNodeReconciliation getFlowNodeReconciliation() {
        return nodeListener;
    }

    @Override
    public FRMConfig getConfiguration() {
        return frmConfig;
    }
}

