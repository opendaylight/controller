/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility

import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived
import org.opendaylight.controller.sal.packet.RawPacket

class DataPacketAdapter implements PacketProcessingListener {

    @Property
    IPluginOutDataPacketService dataPacketPublisher;

    override onPacketReceived(PacketReceived packet) {
        val RawPacket inPacket = packet.toRawPacket();
        dataPacketPublisher.receiveDataPacket(inPacket);
    }

    public static def RawPacket toRawPacket(PacketReceived received) {        
        val ret = new RawPacket(received.payload);
        ret.setIncomingNodeConnector(NodeMapping.toADNodeConnector(received.ingress))
        return ret;
    }

}
