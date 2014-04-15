/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.frm.AbstractTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
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

@Deprecated
/**
 * @deprecated please use {@link MeterDataChangeListener}
 */
public class MeterTransaction extends AbstractTransaction {

    private final SalMeterService salMeterService;
    
    public SalMeterService getSalMeterService() {
        return this.salMeterService;
    }
    
    @Deprecated
    /**
     * @deprecated please use {@link MeterDataChangeListener}
     */
    public MeterTransaction(
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,
            final SalMeterService salMeterService) {
        super(modification);
        this.salMeterService = salMeterService;
    }
    
    public void remove(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Meter)) {
            
            final Meter meter = ((Meter) obj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final RemoveMeterInputBuilder builder = new RemoveMeterInputBuilder(meter);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            this.salMeterService.removeMeter((RemoveMeterInput) builder.build());
        }
    }

    public void update(final InstanceIdentifier<? extends Object> instanceId, final DataObject originalObj, final DataObject updatedObj) {
        if (originalObj instanceof Meter && updatedObj instanceof Meter) {
            
            final Meter originalMeter = ((Meter) originalObj);
            final Meter updatedMeter = ((Meter) updatedObj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final UpdateMeterInputBuilder builder = new UpdateMeterInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            
            builder.setUpdatedMeter((UpdatedMeter) (new UpdatedMeterBuilder(updatedMeter)).build());
            builder.setOriginalMeter((OriginalMeter) (new OriginalMeterBuilder(originalMeter)).build());
            
            this.salMeterService.updateMeter((UpdateMeterInput) builder.build());
        }
    }
    
    public void add(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Meter)) {
            
            final Meter meter = ((Meter) obj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final AddMeterInputBuilder builder = new AddMeterInputBuilder(meter);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setMeterRef(new MeterRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            this.salMeterService.addMeter((AddMeterInput) builder.build());
        }
    }
    
    public void validate() throws IllegalStateException {
        MeterTransactionValidator.validate(this);
    }
}