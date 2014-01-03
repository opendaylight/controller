package org.opendaylight.md.controller.topology.lldp.utils;

import java.nio.charset.Charset;
import java.util.List;

import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.LLDP;
import org.opendaylight.controller.sal.packet.LLDPTLV;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LLDPDiscoveryUtils {
    static Logger LOG = LoggerFactory.getLogger(LLDPDiscoveryUtils.class);
    
    public static final Long LLDP_INTERVAL = (long) (1000*5); // Send LLDP every five seconds
    public static final Long LLDP_EXPIRATION_TIME = LLDP_INTERVAL*3; // Let up to three intervals pass before we decide we are expired.
    
    public static String macToString(byte[] mac) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            b.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }

        return b.toString();
    }
    
    public static NodeConnectorRef lldpToNodeConnectorRef(byte[] payload)  {
        Ethernet ethPkt = new Ethernet();
        try {
            ethPkt.deserialize(payload, 0,payload.length * NetUtils.NumBitsInAByte);
        } catch (Exception e) {
            LOG.warn("Failed to decode LLDP packet {}", e);
        }

        if (ethPkt.getPayload() instanceof LLDP) {
            LLDP lldp = (LLDP) ethPkt.getPayload();
    
            try {
                List<LLDPTLV> optionalTLVList = lldp.getOptionalTLVList();
                if (optionalTLVList == null) {
                    return null;
                }
                NodeId srcNodeId = null;
                NodeConnectorId srcNodeConnectorId = null;
                for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
                    if (lldptlv.getType() == LLDPTLV.TLVType.Custom.getValue()) {
                        srcNodeConnectorId = new NodeConnectorId(LLDPTLV.getCustomString(lldptlv.getValue(), lldptlv.getLength()));
                    }
                    if (lldptlv.getType() == LLDPTLV.TLVType.SystemName.getValue()) {
                        String srcNodeIdString = new String(lldptlv.getValue(),Charset.defaultCharset());
                        srcNodeId = new NodeId(srcNodeIdString);
                    }
                }
                if(srcNodeId != null && srcNodeConnectorId != null) {
                    InstanceIdentifier<NodeConnector> srcInstanceId = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class,new NodeKey(srcNodeId))
                            .child(NodeConnector.class, new NodeConnectorKey(srcNodeConnectorId))
                            .toInstance();
                    return new NodeConnectorRef(srcInstanceId);
                }
            } catch (Exception e) {
                LOG.warn("Caught exception ", e);
            }
        }
        return null;
    }
}
