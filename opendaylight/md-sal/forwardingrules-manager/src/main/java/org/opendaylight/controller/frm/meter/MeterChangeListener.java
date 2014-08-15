/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.OriginalMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
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
public class MeterChangeListener extends AbstractChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(MeterChangeListener.class);

    private final MeterProvider provider;

    public MeterChangeListener (final MeterProvider provider) {
        this.provider = Preconditions.checkNotNull(provider, "MeterProvider can not be null !");
    }

    @Override
    protected void remove(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject removeDataObj) {

        final Meter meter = ((Meter) removeDataObj);
        final InstanceIdentifier<Node> nodeIdent = identifier.firstIdentifierOf(Node.class);
        final RemoveMeterInputBuilder builder = new RemoveMeterInputBuilder(meter);

        builder.setNode(new NodeRef(nodeIdent));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalMeterService().removeMeter(builder.build());
        LOG.debug("Transaction {} - Remove Meter has removed meter: {}", new Object[]{uri, removeDataObj});
    }

    @Override
    protected void update(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject original, final DataObject update) {

        final Meter originalMeter = ((Meter) original);
        final Meter updatedMeter = ((Meter) update);
        final InstanceIdentifier<Node> nodeInstanceId = identifier.firstIdentifierOf(Node.class);
        final UpdateMeterInputBuilder builder = new UpdateMeterInputBuilder();

        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);

        builder.setUpdatedMeter((new UpdatedMeterBuilder(updatedMeter)).build());
        builder.setOriginalMeter((new OriginalMeterBuilder(originalMeter)).build());

        this.provider.getSalMeterService().updateMeter(builder.build());
        LOG.debug("Transaction {} - Update Meter has updated meter {} with {}", new Object[]{uri, original, update});

    }

    @Override
    protected void add(final InstanceIdentifier<? extends DataObject> identifier,
                       final DataObject addDataObj) {

        final Meter meter = ((Meter) addDataObj);
        final InstanceIdentifier<Node> nodeInstanceId = identifier.firstIdentifierOf(Node.class);
        final AddMeterInputBuilder builder = new AddMeterInputBuilder(meter);

        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setMeterRef(new MeterRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalMeterService().addMeter(builder.build());
        LOG.debug("Transaction {} - Add Meter has added meter: {}", new Object[]{uri, addDataObj});
    }

    @Override
    protected boolean preconditionForChange(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject dataObj, final DataObject update) {

        final ReadOnlyTransaction trans = this.provider.getDataService().newReadOnlyTransaction();
        return update != null
                ? (dataObj instanceof Meter && update instanceof Meter && isNodeAvailable(identifier, trans))
                : (dataObj instanceof Meter && isNodeAvailable(identifier, trans));
    }
}
