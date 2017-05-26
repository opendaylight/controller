/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.liblldp;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;

/**
 *
 */
public class LLDPTLVTest {

    /** dummy custom tlv value */
    private static final String CUSTOM_TLV_ULTIMATE = "What do you get when you multiply 6 by 9?";
    /** dummy custom tlv value in binary form */
    private static final byte[] CUSTOM_TLV_ULTIMATE_BIN = new byte[] {
        0x57, 0x68, 0x61, 0x74, 0x20, 0x64, 0x6f, 0x20, 0x79, 0x6f, 0x75, 0x20, 0x67, 0x65, 0x74,
        0x20, 0x77, 0x68, 0x65, 0x6e, 0x20, 0x79, 0x6f, 0x75, 0x20, 0x6d, 0x75, 0x6c, 0x74, 0x69,
        0x70, 0x6c, 0x79, 0x20, 0x36, 0x20, 0x62, 0x79, 0x20, 0x39, 0x3f
    };

    private static final Logger LOG = LoggerFactory.getLogger(LLDPTLVTest.class);

    /**
     * Test method for
     * {@link org.opendaylight.controller.liblldp.LLDPTLV#createCustomTLVValue(java.lang.String)}
     * .
     */
    @Test
    public void testCreateCustomTLVValue() {
        byte[] tlv = LLDPTLV.createCustomTLVValue(CUSTOM_TLV_ULTIMATE);

        byte[] expectedCustomTlv = Bytes.concat(new byte[] {
                // custom type (7b) + length (9b) = 16b = 2B  (skipped)
                // 0x7f, 24,
                // openflow OUI
                0x00, 0x26, (byte) 0xe1,
                // subtype
                0x00},
                // custom value
                CUSTOM_TLV_ULTIMATE_BIN);

        BaseEncoding be = BaseEncoding.base16().withSeparator(" ", 2).lowerCase();
        LOG.debug("expected: {}", be.encode(expectedCustomTlv));
        LOG.debug("actual  : {}", be.encode(tlv));
        Assert.assertArrayEquals(expectedCustomTlv, tlv);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.liblldp.LLDPTLV#getCustomString(byte[], int)}
     * .
     * @throws Exception
     */
    @Test
    public void testGetCustomString() throws Exception {
        byte[] inputCustomTlv = Bytes.concat(new byte[] {
                // custom type (7b) + length (9b) = 16b = 2B  (skipped)
                // 0x7f, 24,
                // openflow OUI
                0x00, 0x26, (byte) 0xe1,
                // subtype
                0x00},
                // custom value
                CUSTOM_TLV_ULTIMATE_BIN);

        String actual = LLDPTLV.getCustomString(inputCustomTlv, inputCustomTlv.length);
        LOG.debug("actual custom TLV value as string: {}", actual);
        Assert.assertEquals(CUSTOM_TLV_ULTIMATE, actual);
    }
}
