/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp.utils;

import com.google.common.hash.Hasher;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.LLDP;
import org.opendaylight.controller.liblldp.LLDPTLV;
import org.opendaylight.controller.liblldp.NetUtils;
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
                NodeId srcNodeId = null;
                NodeConnectorId srcNodeConnectorId = null;
                byte[] lLDPHash = new byte[0];
                for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
                    if (lldptlv.getType() == LLDPTLV.TLVType.Custom.getValue()) {
                        srcNodeConnectorId = new NodeConnectorId(LLDPTLV.getCustomString(lldptlv.getValue(), lldptlv.getLength()));
                    } else if (lldptlv.getType() == LLDPTLV.TLVType.SystemName.getValue()) {
                        String srcNodeIdString = new String(lldptlv.getValue(),Charset.defaultCharset());
                        srcNodeId = new NodeId(srcNodeIdString);
                    } else if (lldptlv.getType() == LLDPTLV.TLVType.CustomSec.getValue()) {
                        lLDPHash = lldptlv.getValue();
                    }
                }
                byte[] calculatedHash = calculateHash(srcNodeConnectorId);
                if (calculatedHash != lLDPHash) {
                    LOG.warn("Attack. LLDP packet witch inconsistent CustomSec field was sent.");
                } else if(srcNodeId != null && srcNodeConnectorId != null) {
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

    /**
     * Calculate CustomSec value for LLDP packet.
     * 
     * @return calculated md5 value for combination of node connector ID and running PID of JAVA
     */
    private static byte[] calculateHash(final NodeConnectorId srcNodeConnectorId) {
        final String pureValue = srcNodeConnectorId.getValue()+ManagementFactory.getRuntimeMXBean().getName();
        final HashFunction hashFunction = Hashing.md5();
        final Hasher hasher = hashFunction.newHasher();
        hasher.putString(pureValue);
        return hasher.hash().asBytes();
    }
}
