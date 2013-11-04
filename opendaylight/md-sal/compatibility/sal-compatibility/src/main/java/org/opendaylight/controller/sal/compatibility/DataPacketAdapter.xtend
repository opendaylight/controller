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
        return ret;
    }

}
