package org.opendaylight.controller.sal.compability.adsal;

import org.opendaylight.controller.sal.compability.NodeMapping;
import org.opendaylight.controller.sal.packet.*;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.*;

public class DataPacketServiceAdapter implements IPluginInDataPacketService {

    PacketProcessingService delegate;

    @Override
    public void transmitDataPacket(RawPacket outPkt) {
        TransmitPacketInput packet = toTransmitPacketInput(outPkt);
        delegate.transmitPacket(packet);
    }

    private TransmitPacketInput toTransmitPacketInput(RawPacket rawPacket) {
        TransmitPacketInputBuilder builderTPIB = new TransmitPacketInputBuilder();

        NodeConnectorRef egress = NodeMapping.toNodeConnectorRef(rawPacket.getOutgoingNodeConnector());
        NodeConnectorRef ingress = NodeMapping.toNodeConnectorRef(rawPacket.getIncomingNodeConnector());
        byte[] payload = rawPacket.getPacketData();

        builderTPIB.setEgress(egress);
        builderTPIB.setIngress(ingress);
        builderTPIB.setPayload(payload);

        return builderTPIB.build();
    }

}
