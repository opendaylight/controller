/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataPacketAdapter implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(DataPacketAdapter.class);

    // These are injected via Apache DM (see ComponentActivator)
    private IPluginOutDataPacketService dataPacketPublisher;

    @Override
    public void onPacketReceived(final PacketReceived packet) {
        try {
            RawPacket inPacket = toRawPacket(packet);
            if (dataPacketPublisher != null) {
                dataPacketPublisher.receiveDataPacket(inPacket);
            } else {
                LOG.warn("IPluginOutDataPacketService is not available. Not forwarding packet to AD-SAL.");
            }
        } catch (ConstructionException e) {
            LOG.warn("Failed to construct raw packet from {}, dropping it", packet, e);
        }
    }

    public static RawPacket toRawPacket(final PacketReceived received) throws ConstructionException {
        final RawPacket ret = new RawPacket(received.getPayload());
        ret.setIncomingNodeConnector(NodeMapping.toADNodeConnector(received.getIngress()));
        return ret;
    }

    public IPluginOutDataPacketService getDataPacketPublisher() {
        return dataPacketPublisher;
    }

    // These are injected via Apache DM (see ComponentActivator)
    public void setDataPacketPublisher(final IPluginOutDataPacketService dataPacketPublisher) {
        this.dataPacketPublisher = dataPacketPublisher;
    }
}
