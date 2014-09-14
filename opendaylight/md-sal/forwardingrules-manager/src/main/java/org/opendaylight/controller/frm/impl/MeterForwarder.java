/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import com.google.common.base.Preconditions;
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

/**
 * MeterForwarder
 * It implements {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}}
 * for WildCardedPath to {@link Meter} and ForwardingRulesCommiter interface for methods:
 *  add, update and remove {@link Meter} processing for
 *  {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class MeterForwarder extends AbstractListeningCommiter<Meter> {

    private static final Logger LOG = LoggerFactory.getLogger(MeterForwarder.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public MeterForwarder (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, Meter.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        this.listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                getWildCardPath(), MeterForwarder.this, DataChangeScope.SUBTREE);
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
    protected InstanceIdentifier<Meter> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Meter.class);
    }

    @Override
    public void remove(final InstanceIdentifier<Meter> identifier, final Meter removeDataObj,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final RemoveMeterInputBuilder builder = new RemoveMeterInputBuilder(removeDataObj);

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setMeterRef(new MeterRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        this.provider.getSalMeterService().removeMeter(builder.build());
    }

    @Override
    public void update(final InstanceIdentifier<Meter> identifier,
                       final Meter original, final Meter update,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final UpdateMeterInputBuilder builder = new UpdateMeterInputBuilder();

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setMeterRef(new MeterRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        builder.setUpdatedMeter((new UpdatedMeterBuilder(update)).build());
        builder.setOriginalMeter((new OriginalMeterBuilder(original)).build());

        this.provider.getSalMeterService().updateMeter(builder.build());
    }

    @Override
    public void add(final InstanceIdentifier<Meter> identifier, final Meter addDataObj,
                    final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final AddMeterInputBuilder builder = new AddMeterInputBuilder(addDataObj);

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setMeterRef(new MeterRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        this.provider.getSalMeterService().addMeter(builder.build());
    }
}

