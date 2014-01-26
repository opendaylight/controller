/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.adsal;

import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;

public class DataPacketServiceAdapter implements IPluginInDataPacketService {

    PacketProcessingService delegate;

    @Override
    public void transmitDataPacket(RawPacket outPkt) {
        TransmitPacketInput packet = toTransmitPacketInput(outPkt);
        delegate.transmitPacket(packet);
    }

    private TransmitPacketInput toTransmitPacketInput(RawPacket rawPacket) {
        TransmitPacketInputBuilder builderTPIB = new TransmitPacketInputBuilder();

        builderTPIB.setNode(NodeMapping.toNodeRef(rawPacket.getOutgoingNodeConnector().getNode()));

        NodeConnectorRef egress = rawPacket.getOutgoingNodeConnector() == null ? null :
                NodeMapping.toNodeConnectorRef(rawPacket.getOutgoingNodeConnector());
        NodeConnectorRef ingress = rawPacket.getIncomingNodeConnector() == null ? null :
                NodeMapping.toNodeConnectorRef(rawPacket.getIncomingNodeConnector());
        byte[] payload = rawPacket.getPacketData();

        builderTPIB.setEgress(egress);
        builderTPIB.setIngress(ingress);
        builderTPIB.setPayload(payload);

        return builderTPIB.build();
    }

    public PacketProcessingService getDelegate() {
        return delegate;
    }

    public void setDelegate(PacketProcessingService delegate) {
        this.delegate = delegate;
    }



}
