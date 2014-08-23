/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.OriginalMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterRef;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Meter Change Listener
 *  add, update and remove {@link Meter} processing from {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class MeterChangeListener extends AbstractChangeListener<Meter> {

    private static final Logger LOG = LoggerFactory.getLogger(MeterChangeListener.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public MeterChangeListener (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        /* Build Path */
        InstanceIdentifier<Meter> meterWildCardIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).child(Meter.class);
        this.listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                meterWildCardIdentifier, MeterChangeListener.this, DataChangeScope.SUBTREE);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Error by stop FRM MeterChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Meter> identifier, final Meter removeDataObj,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final RemoveMeterInputBuilder builder = new RemoveMeterInputBuilder(removeDataObj);

        builder.setNode(new NodeRef(nodeIdent));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(provider.getNewTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalMeterService().removeMeter(builder.build());
        LOG.debug("Transaction {} - Remove Meter has removed meter: {}", uri, removeDataObj);
    }

    @Override
    public void update(final InstanceIdentifier<Meter> identifier,
                       final Meter original, final Meter update,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final UpdateMeterInputBuilder builder = new UpdateMeterInputBuilder();

        builder.setNode(new NodeRef(nodeIdent));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(provider.getNewTransactionId());
        builder.setTransactionUri(uri);

        builder.setUpdatedMeter((new UpdatedMeterBuilder(update)).build());
        builder.setOriginalMeter((new OriginalMeterBuilder(original)).build());

        this.provider.getSalMeterService().updateMeter(builder.build());
        LOG.debug("Transaction {} - Update Meter has updated meter {} with {}", uri, original, update);

    }

    @Override
    public void add(final InstanceIdentifier<Meter> identifier, final Meter addDataObj,
                    final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final AddMeterInputBuilder builder = new AddMeterInputBuilder(addDataObj);

        builder.setNode(new NodeRef(nodeIdent));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(provider.getNewTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalMeterService().addMeter(builder.build());
        LOG.debug("Transaction {} - Add Meter has added meter: {}", uri, addDataObj);
    }
}

