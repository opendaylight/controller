/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MeterProvider.class);

    private DataProviderService dataService;
    private SalMeterService salMeterService;

    /* DataChangeListener */
    private DataChangeListener meterDataChangeListener;
    private ListenerRegistration<DataChangeListener> meterDataChangeListenerRegistration;

    public void init(final DataProviderService dataService) {
        LOG.info("FRM Meter Config Provider initialization.");
        if (dataService == null) {
            throw new IllegalArgumentException("DataProviderService can not be null !");
        }
        this.dataService = dataService;
    }

    public void start(final RpcConsumerRegistry rpcRegistry) {
        if (rpcRegistry == null) {
            throw new IllegalArgumentException("RpcConsumerRegistry can not be null !");
        }
        if (rpcRegistry.getRpcService(SalMeterService.class) == null) {
            throw new IllegalStateException("RPC SalMeterService not found.");
        }

        this.salMeterService = rpcRegistry.getRpcService(SalMeterService.class);
        /* Build Path */
        InstanceIdentifierBuilder<Nodes> nodesBuilder = InstanceIdentifier.builder(Nodes.class);
        InstanceIdentifierBuilder<Node> nodeChild = nodesBuilder.child(Node.class);
        InstanceIdentifierBuilder<FlowCapableNode> augmentFlowCapNode =
                nodeChild.augmentation(FlowCapableNode.class);
        InstanceIdentifierBuilder<Meter> meterChild = augmentFlowCapNode.child(Meter.class);
        final InstanceIdentifier<? extends DataObject> meterDataObjectPath = meterChild.toInstance();

        /* DataChangeListener registration */
        this.meterDataChangeListener = new MeterChangeListener(MeterProvider.this);
        this.meterDataChangeListenerRegistration =
                this.dataService.registerDataChangeListener(meterDataObjectPath, meterDataChangeListener);
        LOG.info("FRM Meter Config Provider started.");
    }

    protected DataModificationTransaction startChange() {
        return this.dataService.beginTransaction();
    }

    @Override
    public void close() {
        LOG.info("FRM Meter Config Provider stopped.");
        if(meterDataChangeListenerRegistration != null){
            try {
                meterDataChangeListenerRegistration.close();
            }
            catch (Exception e) {
                String errMsg = "Error by stop FRM Meter Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            }
        }
    }

    public DataChangeListener getMeterDataChangeListener() {
        return meterDataChangeListener;
    }

    public DataProviderService getDataService() {
        return dataService;
    }

    public SalMeterService getSalMeterService() {
        return salMeterService;
    }
}
