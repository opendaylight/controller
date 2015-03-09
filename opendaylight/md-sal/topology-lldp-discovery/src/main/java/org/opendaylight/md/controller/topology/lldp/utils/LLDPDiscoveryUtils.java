/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp.utils;

import java.security.NoSuchAlgorithmException;

import java.security.MessageDigest;
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
     * @param srcNodeConnectorId
     * @return
     */
    private static byte[] calculateHash(final NodeConnectorId srcNodeConnectorId) {
        final String pureValue = srcNodeConnectorId.getValue()+ManagementFactory.getRuntimeMXBean().getName();
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            return messageDigest.digest(pureValue.getBytes());
        } catch (NoSuchAlgorithmException e) {
            LOG.debug("Message digest instance for calculating MD5 wasn't found while calculating hash for LLDP TLV CustomSec.");
        }
        return null;
    }
}
