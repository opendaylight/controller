/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.OriginalMeter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.OriginalMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class MeterChangeListener extends AbstractChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(MeterChangeListener.class);

    private final SalMeterService salMeterService;

    public SalMeterService getSalMeterService() {
        return this.salMeterService;
    }
    
    public MeterChangeListener(final SalMeterService manager) {
        this.salMeterService = manager;
    }

    @Override
    protected void validate() throws IllegalStateException {
        MeterTransactionValidator.validate(this);
    }

    @Override
    protected void remove(InstanceIdentifier<? extends DataObject> identifier, DataObject removeDataObj) {
        if ((removeDataObj instanceof Meter)) {
            
            final Meter meter = ((Meter) removeDataObj);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final RemoveMeterInputBuilder builder = new RemoveMeterInputBuilder(meter);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salMeterService.removeMeter((RemoveMeterInput) builder.build());
            LOG.debug("Transaction {} - Remove Meter has removed meter: {}", new Object[]{uri, removeDataObj});
        }
    }

    @Override
    protected void update(InstanceIdentifier<? extends DataObject> identifier, DataObject original, DataObject update) {
        if (original instanceof Meter && update instanceof Meter) {
            
            final Meter originalMeter = ((Meter) original);
            final Meter updatedMeter = ((Meter) update);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final UpdateMeterInputBuilder builder = new UpdateMeterInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            
            builder.setUpdatedMeter((UpdatedMeter) (new UpdatedMeterBuilder(updatedMeter)).build());
            builder.setOriginalMeter((OriginalMeter) (new OriginalMeterBuilder(originalMeter)).build());
            
            this.salMeterService.updateMeter((UpdateMeterInput) builder.build());
            LOG.debug("Transaction {} - Update Meter has updated meter {} with {}", new Object[]{uri, original, update});
        }
    }

    @Override
    protected void add(InstanceIdentifier<? extends DataObject> identifier, DataObject addDataObj) {
        if ((addDataObj instanceof Meter)) {
            
            final Meter meter = ((Meter) addDataObj);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final AddMeterInputBuilder builder = new AddMeterInputBuilder(meter);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salMeterService.addMeter((AddMeterInput) builder.build());
            LOG.debug("Transaction {} - Add Meter has added meter: {}", new Object[]{uri, addDataObj});
        }
    }
}