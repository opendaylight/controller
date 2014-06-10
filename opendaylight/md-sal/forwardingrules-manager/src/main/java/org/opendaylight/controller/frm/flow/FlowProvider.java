/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Flow Provider registers the {@link FlowChangeListener} and it holds all needed
 * services for {@link FlowChangeListener}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class FlowProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowProvider.class);

    private SalFlowService salFlowService;
    private DataBroker dataService;

    /* DataChangeListener */
    private DataChangeListener flowDataChangeListener;
    private ListenerRegistration<DataChangeListener> flowDataChangeListenerRegistration;

    /**
     * Provider Initialization Phase.
     *
     * @param DataProviderService dataService
     */
    public void init (final DataBroker dataService) {
        LOG.info("FRM Flow Config Provider initialization.");
        this.dataService = Preconditions.checkNotNull(dataService, "DataProviderService can not be null !");
    }

    /**
     * Listener Registration Phase
     *
     * @param RpcConsumerRegistry rpcRegistry
     */
    public void start(final RpcConsumerRegistry rpcRegistry) {
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");

        this.salFlowService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalFlowService.class),
                "RPC SalFlowService not found.");

        /* Build Path */
        InstanceIdentifier<Flow> flowIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);

        /* DataChangeListener registration */
        this.flowDataChangeListener = new FlowChangeListener(FlowProvider.this);
        this.flowDataChangeListenerRegistration =
                this.dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        flowIdentifier, flowDataChangeListener, DataChangeScope.SUBTREE);

        LOG.info("FRM Flow Config Provider started.");
    }

    @Override
    public void close() {
        LOG.info("FRM Flow Config Provider stopped.");
        if (flowDataChangeListenerRegistration != null) {
            try {
                flowDataChangeListenerRegistration.close();
            } catch (Exception e) {
                String errMsg = "Error by stop FRM Flow Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            } finally {
                flowDataChangeListenerRegistration = null;
            }
        }
    }

    public DataChangeListener getFlowDataChangeListener() {
        return flowDataChangeListener;
    }

    public SalFlowService getSalFlowService() {
        return salFlowService;
    }

    public DataBroker getDataService() {
        return dataService;
    }
}
