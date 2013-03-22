
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   EthernetAddressTest.java
 *
 * @brief  Unit Tests for EthernetAddress class
 *
 * Unit Tests for EthernetAddress class
 */
package org.opendaylight.controller.sal.packet.address;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;

public class EthernetAddressTest {
    @Test
    public void testNonValidConstructor() {
        EthernetAddress ea1;
        // Null input array
        try {
            ea1 = new EthernetAddress((byte[]) null);

            // Exception is expected if NOT raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        // Array too short
        try {
            ea1 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0 });

            // Exception is expected if NOT raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        // Array too long
        try {
            ea1 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
                    (byte) 0x0 });

            // Exception is expected if NOT raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }
    }

    @Test
    public void testEquality() {
        EthernetAddress ea1;
        EthernetAddress ea2;
        try {
            ea1 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1 });

            ea2 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1 });
            Assert.assertTrue(ea1.equals(ea2));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            ea1 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1 });

            ea2 = ea1.clone();
            Assert.assertTrue(ea1.equals(ea2));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        // Check for well knowns
        try {
            ea1 = EthernetAddress.BROADCASTMAC;
            ea2 = new EthernetAddress(new byte[] { (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff });
            Assert.assertTrue(ea1.equals(ea2));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testUnEquality() {
        EthernetAddress ea1;
        EthernetAddress ea2;
        try {
            ea1 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x2 });

            ea2 = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                    (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1 });
            Assert.assertTrue(!ea1.equals(ea2));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }
}
