/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp.utils;

import org.apache.commons.lang3.ArrayUtils;
import com.google.common.hash.HashCode;
import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.CustomTLVKey;
import com.google.common.hash.HashCode;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;
import java.security.NoSuchAlgorithmException;
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
                byte[] lLDPHash = null;

                final LLDPTLV systemIdTLV = lldp.getSystemNameId();
                if (systemIdTLV != null) {
                    String srcNodeIdString = new String(systemIdTLV.getValue(),Charset.defaultCharset());
                    srcNodeId = new NodeId(srcNodeIdString);
                } else {
                    return null;
                }
                
                final LLDPTLV nodeConnectorIdLldptlv = lldp.getCustomTLV(
                        new CustomTLVKey(BitBufferHelper.getInt(LLDPTLV.OFOUI), LLDPTLV.CUSTOM_TLV_SUB_TYPE_NODE_CONNECTOR_ID[0]));
                if (nodeConnectorIdLldptlv != null) {
                    srcNodeConnectorId = new NodeConnectorId(LLDPTLV.getCustomString(
                            nodeConnectorIdLldptlv.getValue(), nodeConnectorIdLldptlv.getLength()));
                } else {
                    LOG.debug("Node connector wasn't specified via Custom TLV in LLDP packet.");
                }

                final LLDPTLV hashLldptlv = lldp.getCustomTLV(
                        new CustomTLVKey(BitBufferHelper.getInt(LLDPTLV.OFOUI), LLDPTLV.CUSTOM_TLV_SUB_TYPE_CUSTOM_SEC[0]));
                if (hashLldptlv != null) {
                    byte[] value = hashLldptlv.getValue();
                    lLDPHash = ArrayUtils.subarray(value, 4, value.length);
                }

                byte[] calculatedHash = getValueForLLDPPacketIntegrityEnsuring(srcNodeConnectorId);
                if (calculatedHash != lLDPHash) {
                    LOG.warn("Attack. LLDP packet witch inconsistent CustomSec field was sent.");
                } else {
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

    public static byte[] getValueForLLDPPacketIntegrityEnsuring(final NodeConnectorId nodeConnectorId) throws NoSuchAlgorithmException {
        final String pureValue = nodeConnectorId+ManagementFactory.getRuntimeMXBean().getName();
        final byte[] pureBytes = pureValue.getBytes();
        HashFunction hashFunction = Hashing.md5();
        Hasher hasher = hashFunction.newHasher();
        HashCode hashedValue = hasher.putBytes(pureBytes).hash();
        return hashedValue.asBytes();
    }

}
