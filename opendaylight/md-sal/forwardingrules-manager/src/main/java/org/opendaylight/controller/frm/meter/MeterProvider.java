/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

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

    private final static Logger LOG = LoggerFactory.getLogger(MeterProvider.class);

    private DataProviderService dataService;
    private SalMeterService salMeterService;

    /* DataChangeListener */
    private MeterChangeListener meterDataChangeListener;
    ListenerRegistration<DataChangeListener> meterDataChangeListenerRegistration;

    public void start() {
        /* Build Path */
        InstanceIdentifierBuilder<Nodes> nodesBuilder = InstanceIdentifier.<Nodes> builder(Nodes.class);
        InstanceIdentifierBuilder<Node> nodeChild = nodesBuilder.<Node> child(Node.class);
        InstanceIdentifierBuilder<FlowCapableNode> augmentFlowCapNode = nodeChild.<FlowCapableNode> augmentation(FlowCapableNode.class);
        InstanceIdentifierBuilder<Meter> meterChild = augmentFlowCapNode.<Meter> child(Meter.class);
        final InstanceIdentifier<? extends DataObject> meterDataObjectPath = meterChild.toInstance();

        /* DataChangeListener registration */
        this.meterDataChangeListener = new MeterChangeListener(this.salMeterService);
        this.meterDataChangeListenerRegistration = this.dataService.registerDataChangeListener(meterDataObjectPath, meterDataChangeListener);
        LOG.info("Meter Config Provider started.");
    }
    
    protected DataModificationTransaction startChange() {
        return this.dataService.beginTransaction();
    }

    public void close() throws Exception {
        if(meterDataChangeListenerRegistration != null){
            meterDataChangeListenerRegistration.close();
        }
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    public void setSalMeterService(final SalMeterService salMeterService) {
        this.salMeterService = salMeterService;
    }
}