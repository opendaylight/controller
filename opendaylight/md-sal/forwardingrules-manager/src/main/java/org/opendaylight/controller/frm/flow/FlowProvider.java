/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowProvider.class);

    private SalFlowService salFlowService;
    private DataProviderService dataService;
    private NotificationProviderService notificationService;

    /* DataChangeListener */
    private FlowChangeListener flowDataChangeListener;
    ListenerRegistration<DataChangeListener> flowDataChangeListenerRegistration;

    public void init (final DataProviderService dataService,
                      final NotificationProviderService nps) {
        if (dataService == null) {
            throw new IllegalArgumentException("DataProviderService can not be null !");
        }
        if (nps == null) {
            throw new IllegalArgumentException("NotificationProviderService can not be null !");
        }
        this.dataService = dataService;
        this.notificationService = nps;
    }

    public void start(final RpcConsumerRegistry rpcRegistry) {
        if (rpcRegistry == null) {
            throw new IllegalArgumentException("RpcConsumerRegistry can not be null !");
        }
        this.salFlowService = rpcRegistry.getRpcService(SalFlowService.class);
        /* Build Path */
        InstanceIdentifierBuilder<Nodes> nodesBuilder = InstanceIdentifier.builder(Nodes.class);
        InstanceIdentifierBuilder<Node> nodeChild = nodesBuilder.child(Node.class);
        InstanceIdentifierBuilder<FlowCapableNode> augmentFlowCapNode =
                nodeChild.augmentation(FlowCapableNode.class);
        InstanceIdentifierBuilder<Table> tableChild = augmentFlowCapNode.child(Table.class);
        InstanceIdentifierBuilder<Flow> flowChild = tableChild.child(Flow.class);
        final InstanceIdentifier<? extends DataObject> flowDataObjectPath = flowChild.toInstance();

        /* DataChangeListener registration */
        this.flowDataChangeListener = new FlowChangeListener(FlowProvider.this);
        this.flowDataChangeListenerRegistration =
                this.dataService.registerDataChangeListener(flowDataObjectPath, flowDataChangeListener);
        LOG.info("FRM Flow Config Provider started.");
    }

    protected DataModificationTransaction startChange() {
        return this.dataService.beginTransaction();
    }

    @Override
    public void close() {
        LOG.info("FRM Flow Config Provider stopped.");
        if (flowDataChangeListenerRegistration != null) {
            try {
                flowDataChangeListenerRegistration.close();
            }
            catch (Exception e) {
                String errMsg = "Error by stop FRM Flow Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            }
        }
    }

    public SalFlowService getSalFlowService() {
        return salFlowService;
    }

    public DataProviderService getDataService() {
        return dataService;
    }

    public NotificationProviderService getNotificationService() {
        return notificationService;
    }
}
