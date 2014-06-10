/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm.reconil;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 * FlowNode Reconciliation Provider registers the FlowNodeReconilListener
 * and it holds all needed services for FlowNodeReconcilListener.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 13, 2014
 */
public class FlowNodeReconcilProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeReconcilProvider.class);

    private SalFlowService salFlowService;
    private SalMeterService salMeterService;
    private SalGroupService salGroupService;
    private DataBroker dataService;

    /* DataChangeListener */
    private DataChangeListener flowNodeReconcilListener;
    private ListenerRegistration<DataChangeListener> flowNodeReconcilListenerRegistration;

    public void init (final DataBroker dataService) {
        LOG.info("FRM Flow Node Config Reconcil Provider initialization.");

        this.dataService = Preconditions.checkNotNull(dataService, "DataProviderService can not be null !");
    }

    public void start( final RpcConsumerRegistry rpcRegistry ) {
        Preconditions.checkArgument(rpcRegistry != null, "RpcConcumerRegistry can not be null !");

        this.salFlowService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalFlowService.class),
                "RPC SalFlowService not found.");
        this.salMeterService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalMeterService.class),
                "RPC SalMeterService not found.");
        this.salGroupService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalGroupService.class),
                "RPC SalGroupService not found.");

        /* Build Path */
        InstanceIdentifier<FlowCapableNode> flowCapableNodeIdent =
                InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class);

        /* ReconcilNotificationListener registration */
        this.flowNodeReconcilListener = new FlowNodeReconcilListener(FlowNodeReconcilProvider.this);
        this.flowNodeReconcilListenerRegistration = this.dataService.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, flowCapableNodeIdent, flowNodeReconcilListener, DataChangeScope.BASE);
        LOG.info("FRM Flow Node Config Reconcil Provider started.");
    }

    @Override
    public void close() {
        LOG.info("FRM Flow Node Config Reconcil Provider stopped.");
        if (flowNodeReconcilListenerRegistration != null) {
            try {
                flowNodeReconcilListenerRegistration.close();
            } catch (Exception e) {
                String errMsg = "Error by stop FRM Flow Node Config Reconcil Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            } finally {
                flowNodeReconcilListenerRegistration = null;
            }
        }
    }

    public DataChangeListener getFlowNodeReconcilListener() {
        return flowNodeReconcilListener;
    }

    public DataBroker getDataService() {
        return dataService;
    }

    public SalFlowService getSalFlowService() {
        return salFlowService;
    }

    public SalMeterService getSalMeterService() {
        return salMeterService;
    }

    public SalGroupService getSalGroupService() {
        return salGroupService;
    }
}
