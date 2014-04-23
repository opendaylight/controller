/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class NetUtilsTest {

    @Test
    public void testByteArrayMethods() {
        int ip = 8888;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        ip = 0xffffffff;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        ip = 0;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        ip = 0x1fffffff;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        ip = 0xfffffff;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        ip = 0xf000ffff;
        Assert.assertTrue(NetUtils
                .byteArray4ToInt(NetUtils.intToByteArray4(ip)) == ip);

        byte ba[] = { (byte) 0xf, (byte) 0xf, (byte) 0xf, (byte) 0xff };
        Assert.assertTrue(Arrays.equals(ba, NetUtils.intToByteArray4(NetUtils
                .byteArray4ToInt(ba))));

        byte ba1[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };
        Assert.assertTrue(Arrays.equals(ba1, NetUtils.intToByteArray4(NetUtils
                .byteArray4ToInt(ba1))));

        byte ba2[] = { (byte) 255, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(Arrays.equals(ba2, NetUtils.intToByteArray4(NetUtils
                .byteArray4ToInt(ba2))));

        byte ba3[] = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(Arrays.equals(ba3, NetUtils.intToByteArray4(NetUtils
                .byteArray4ToInt(ba3))));

        byte ba4[] = { (byte) 255, (byte) 128, (byte) 0, (byte) 0 };
        Assert.assertTrue(Arrays.equals(ba4, NetUtils.intToByteArray4(NetUtils
                .byteArray4ToInt(ba4))));
    }

    @Test
    public void testByteArrayMethodsForLong() {
        // Test of longToByteArray6 method.
        byte ba[] = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
            (byte) 0x55, (byte) 0x66
        };
        long mac = 0x112233445566L;
        Assert.assertTrue(Arrays.equals(ba, NetUtils.longToByteArray6(mac)));

        byte ba1[] = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff
        };
        long mac1 = 0xffffffffffffL;
        Assert.assertTrue(Arrays.equals(ba1, NetUtils.longToByteArray6(mac1)));

        byte ba2[] = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00
        };
        long mac2 = 0x000000000000L;
        Assert.assertTrue(Arrays.equals(ba2, NetUtils.longToByteArray6(mac2)));

        byte ba3[] = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
            (byte) 0x00, (byte) 0x00
        };
        long mac3 = 0xffffff000000L;
        Assert.assertTrue(Arrays.equals(ba3, NetUtils.longToByteArray6(mac3)));

        byte ba4[] = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff,
            (byte) 0xff, (byte) 0xff
        };
        long mac4 = 0x000000ffffffL;
        Assert.assertTrue(Arrays.equals(ba4, NetUtils.longToByteArray6(mac4)));

        // Convert a long number to a byte array,
        // and revert it to the long number again.
        Assert.assertTrue(NetUtils
                .byteArray6ToLong(NetUtils.longToByteArray6(mac)) == mac);

        Assert.assertTrue(NetUtils
                .byteArray6ToLong(NetUtils.longToByteArray6(mac1)) == mac1);

        Assert.assertTrue(NetUtils
                .byteArray6ToLong(NetUtils.longToByteArray6(mac2)) == mac2);

        Assert.assertTrue(NetUtils
                .byteArray6ToLong(NetUtils.longToByteArray6(mac3)) == mac3);

        Assert.assertTrue(NetUtils
                .byteArray6ToLong(NetUtils.longToByteArray6(mac4)) == mac4);

        // Convert a byte array to a long nubmer,
        // and revert it to the byte array again.
        Assert.assertTrue(Arrays.equals(ba,
                    NetUtils.longToByteArray6(NetUtils.byteArray6ToLong(ba))));

        Assert.assertTrue(Arrays.equals(ba1,
                    NetUtils.longToByteArray6(NetUtils.byteArray6ToLong(ba1))));

        Assert.assertTrue(Arrays.equals(ba2,
                    NetUtils.longToByteArray6(NetUtils.byteArray6ToLong(ba2))));

        Assert.assertTrue(Arrays.equals(ba3,
                    NetUtils.longToByteArray6(NetUtils.byteArray6ToLong(ba3))));

        Assert.assertTrue(Arrays.equals(ba4,
                    NetUtils.longToByteArray6(NetUtils.byteArray6ToLong(ba4))));

        // Test of paramter validation of byteArray6ToLong method.
        byte array5[] = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44
        };
        Assert.assertEquals(0, NetUtils.byteArray6ToLong(array5));

        byte array7[] = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
            (byte) 0x55, (byte) 0x66, (byte) 0x77
        };
        Assert.assertEquals(0, NetUtils.byteArray6ToLong(array7));

        byte arrayNull[] = null;
        Assert.assertEquals(0, NetUtils.byteArray6ToLong(arrayNull));
    }

    @Test
    public void testInetMethods() throws UnknownHostException {
        int ip = 0xfffffff0;
        InetAddress inet = InetAddress.getByName("255.255.255.240");
        Assert.assertTrue(inet.equals(NetUtils.getInetAddress(ip)));

        ip = 0;
        inet = InetAddress.getByName("0.0.0.0");
        Assert.assertTrue(inet.equals(NetUtils.getInetAddress(ip)));

        ip = 0x9ffff09;
        inet = InetAddress.getByName("9.255.255.9");
        Assert.assertTrue(inet.equals(NetUtils.getInetAddress(ip)));
    }

    @Test
    public void testMasksV4() throws UnknownHostException {

        InetAddress mask = InetAddress.getByName("128.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(1, false)));

        mask = InetAddress.getByName("192.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(2, false)));

        mask = InetAddress.getByName("224.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(3, false)));

        mask = InetAddress.getByName("240.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(4, false)));

        mask = InetAddress.getByName("248.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(5, false)));

        mask = InetAddress.getByName("252.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(6, false)));

        mask = InetAddress.getByName("254.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(7, false)));

        mask = InetAddress.getByName("255.0.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(8, false)));

        mask = InetAddress.getByName("255.128.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(9, false)));

        mask = InetAddress.getByName("255.192.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(10, false)));

        mask = InetAddress.getByName("255.224.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(11, false)));

        mask = InetAddress.getByName("255.240.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(12, false)));

        mask = InetAddress.getByName("255.248.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(13, false)));

        mask = InetAddress.getByName("255.252.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(14, false)));

        mask = InetAddress.getByName("255.254.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(15, false)));

        mask = InetAddress.getByName("255.255.0.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(16, false)));

        mask = InetAddress.getByName("255.255.128.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(17, false)));

        mask = InetAddress.getByName("255.255.192.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(18, false)));

        mask = InetAddress.getByName("255.255.224.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(19, false)));

        mask = InetAddress.getByName("255.255.240.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(20, false)));

        mask = InetAddress.getByName("255.255.248.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(21, false)));

        mask = InetAddress.getByName("255.255.252.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(22, false)));

        mask = InetAddress.getByName("255.255.254.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(23, false)));

        mask = InetAddress.getByName("255.255.255.0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(24, false)));

        mask = InetAddress.getByName("255.255.255.128");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(25, false)));

        mask = InetAddress.getByName("255.255.255.192");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(26, false)));

        mask = InetAddress.getByName("255.255.255.224");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(27, false)));

        mask = InetAddress.getByName("255.255.255.240");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(28, false)));

        mask = InetAddress.getByName("255.255.255.248");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(29, false)));

        mask = InetAddress.getByName("255.255.255.252");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(30, false)));

        mask = InetAddress.getByName("255.255.255.254");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(31, false)));

        mask = InetAddress.getByName("255.255.255.255");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(32, false)));
    }

    @Test
    public void testMasksV6() throws UnknownHostException {

        InetAddress mask = InetAddress.getByName("ff00::0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(8, true)));

        mask = InetAddress.getByName("8000::0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(1, true)));

        mask = InetAddress.getByName("f800::0");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(5, true)));

        mask = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe");
        Assert.assertTrue(mask.equals(NetUtils.getInetNetworkMask(127, true)));
    }

    @Test
    public void testGetSubnetLen() {

        byte address[] = { (byte) 128, (byte) 0, (byte) 0, 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address) == 1);

        byte address1[] = { (byte) 255, 0, 0, 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address1) == 8);

        byte address2[] = { (byte) 255, (byte) 255, (byte) 248, 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address2) == 21);

        byte address4[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 254 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address4) == 31);

        byte address5[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address5) == 128);

        byte address6[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 254 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address6) == 127);

        byte address7[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address7) == 64);

        byte address8[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 254, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address8) == 63);

        byte address9[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 128,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address9) == 49);

        byte address10[] = { (byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address10) == 1);

        byte address11[] = { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address11) == 0);
    }

    @Test
    public void testGetSubnetPrefix() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("192.128.64.252");
        int maskLen = 25;
        Assert.assertTrue(NetUtils.getSubnetPrefix(ip, maskLen).equals(
                InetAddress.getByName("192.128.64.128")));
    }

    @Test
    public void testIsIPv6Valid() throws UnknownHostException {
        Assert.assertTrue(NetUtils
                .isIPv6AddressValid("fe80:0000:0000:0000:0204:61ff:fe9d:f156")); //normal ipv6
        Assert.assertTrue(NetUtils
                .isIPv6AddressValid("fe80:0:0:0:204:61ff:fe9d:f156")); //no leading zeroes
        Assert.assertTrue(NetUtils
                .isIPv6AddressValid("fe80::204:61ff:fe9d:f156")); //zeroes to ::
        Assert
                .assertTrue(NetUtils
                        .isIPv6AddressValid("fe80:0000:0000:0000:0204:61ff:254.157.241.86")); // ipv4 ending
        Assert.assertTrue(NetUtils
                .isIPv6AddressValid("fe80:0:0:0:0204:61ff:254.157.241.86")); // no leading zeroes, ipv4 end
        Assert.assertTrue(NetUtils
                .isIPv6AddressValid("fe80::204:61ff:254.157.241.86")); // zeroes ::, no leading zeroes

        Assert.assertTrue(NetUtils.isIPv6AddressValid("2001::")); //link-local prefix
        Assert.assertTrue(NetUtils.isIPv6AddressValid("::1")); //localhost
        Assert.assertTrue(NetUtils.isIPv6AddressValid("fe80::")); //global-unicast
        Assert.assertFalse(NetUtils.isIPv6AddressValid("abcd")); //not valid
        Assert.assertFalse(NetUtils.isIPv6AddressValid("1")); //not valid
        Assert.assertFalse(NetUtils
                .isIPv6AddressValid("fe80:0:0:0:204:61ff:fe9d")); //not valid, too short
        Assert.assertFalse(NetUtils
                .isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d")); //not valid
        Assert.assertFalse(NetUtils.isIPv6AddressValid("192.168.1.1")); //not valid,ipv4
        Assert
                .assertFalse(NetUtils
                        .isIPv6AddressValid("2001:0000:1234:0000:10001:C1C0:ABCD:0876")); //not valid, extra number
        Assert
                .assertFalse(NetUtils
                        .isIPv6AddressValid("20010:0000:1234:0000:10001:C1C0:ABCD:0876")); //not valid, extra number

        Assert
                .assertTrue(NetUtils
                        .isIPv6AddressValid("2001:0DB8:0000:CD30:0000:0000:0000:0000/60")); //full with mask
        Assert.assertTrue(NetUtils.isIPv6AddressValid("2001:0DB8:0:CD30::/64")); //shortened with mask
        Assert.assertTrue(NetUtils.isIPv6AddressValid("2001:0DB8:0:CD30::/0")); //0 subnet with mask
        Assert.assertTrue(NetUtils.isIPv6AddressValid("::1/128")); //localhost 128 mask

        Assert.assertFalse(NetUtils.isIPv6AddressValid("124.15.6.89/60")); //invalid, ip with mask
        Assert
                .assertFalse(NetUtils
                        .isIPv6AddressValid("2001:0DB8:0000:CD30:0000:0000:0000:0000/130")); //invalid, mask >128
        Assert
                .assertFalse(NetUtils
                        .isIPv6AddressValid("2001:0DB8:0:CD30::/-5")); //invalid, mask < 0
        Assert.assertFalse(NetUtils
                .isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d/64")); //not valid ip, valid netmask
        Assert.assertFalse(NetUtils
                .isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d/-1")); //not valid both

    }

    @Test
    public void testInetAddressConflict() throws UnknownHostException {

        // test a ipv4 testAddress in the same subnet as the filter
        // the method should return false as there is no conflict
        Assert.assertFalse(NetUtils.inetAddressConflict(
                InetAddress.getByName("9.9.1.1"),
                InetAddress.getByName("9.9.1.0"), null,
                InetAddress.getByName("255.255.255.0")));

        // test a ipv4 testAddress not in the same subnet as the filter
        // the method should return true as there is a conflict
        Assert.assertTrue(NetUtils.inetAddressConflict(
                InetAddress.getByName("9.9.2.1"),
                InetAddress.getByName("9.9.1.0"), null,
                InetAddress.getByName("255.255.255.0")));

        // test a ipv4 testAddress more generic than the filter
        // the method should return true as there is a conflict
        Assert.assertTrue(NetUtils.inetAddressConflict(
                InetAddress.getByName("9.9.1.1"),
                InetAddress.getByName("9.9.1.0"),
                InetAddress.getByName("255.255.0.0"),
                InetAddress.getByName("255.255.255.0")));

        // test a ipv4 testAddress less generic than the filter and in the same
        // subnet as the filter
        // the method should return false as there is no conflict
        Assert.assertFalse(NetUtils.inetAddressConflict(
                InetAddress.getByName("9.9.1.0"),
                InetAddress.getByName("9.9.0.0"),
                InetAddress.getByName("255.255.255.0"),
                InetAddress.getByName("255.255.0.0")));

        // test a ipv4 testAddress less generic than the filter and not in the
        // same subnet as the filter
        // the method should return true as there is a conflict
        Assert.assertTrue(NetUtils.inetAddressConflict(
                InetAddress.getByName("9.8.1.0"),
                InetAddress.getByName("9.9.0.0"),
                InetAddress.getByName("255.255.255.0"),
                InetAddress.getByName("255.255.0.0")));

    }

    @Test
    public void testIPAddressValidity() {
        Assert.assertFalse(NetUtils.isIPAddressValid(null));
        Assert.assertFalse(NetUtils.isIPAddressValid("abc"));
        Assert.assertFalse(NetUtils.isIPAddressValid("1.1.1"));
        Assert.assertFalse(NetUtils.isIPAddressValid("1.1.1.1/49"));

        Assert.assertTrue(NetUtils.isIPAddressValid("1.1.1.1"));
        Assert.assertTrue(NetUtils.isIPAddressValid("1.1.1.1/32"));
        Assert.assertTrue(NetUtils
                .isIPAddressValid("2001:420:281:1004:407a:57f4:4d15:c355"));
    }

    @Test
    public void testGetUnsignedByte() {
        Assert.assertEquals(0,   NetUtils.getUnsignedByte((byte) 0x00));
        Assert.assertEquals(1,   NetUtils.getUnsignedByte((byte) 0x01));
        Assert.assertEquals(127, NetUtils.getUnsignedByte((byte) 0x7f));

        Assert.assertEquals(128, NetUtils.getUnsignedByte((byte) 0x80));
        Assert.assertEquals(255, NetUtils.getUnsignedByte((byte) 0xff));
    }

    @Test
    public void testGetUnsignedShort() {
        Assert.assertEquals(0,     NetUtils.getUnsignedShort((short) 0x0000));
        Assert.assertEquals(1,     NetUtils.getUnsignedShort((short) 0x0001));
        Assert.assertEquals(32767, NetUtils.getUnsignedShort((short) 0x7fff));

        Assert.assertEquals(32768, NetUtils.getUnsignedShort((short) 0x8000));
        Assert.assertEquals(65535, NetUtils.getUnsignedShort((short) 0xffff));
    }

    @Test
    public void testMulticastMACAddr() {
        byte[] empty = new byte[0];
        Assert.assertFalse(NetUtils.isUnicastMACAddr(empty));
        Assert.assertFalse(NetUtils.isMulticastMACAddr(empty));

        byte[] bcast = {
            (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff, (byte)0xff,
        };
        Assert.assertFalse(NetUtils.isUnicastMACAddr(bcast));
        Assert.assertFalse(NetUtils.isMulticastMACAddr(bcast));

        byte[] firstOctet = {
            (byte)0x00, (byte)0x20, (byte)0x80, (byte)0xfe,
        };
        for (int len = 1; len <= 10; len++) {
            byte[] ba = new byte[len];
            boolean valid = (len == 6);
            for (byte first: firstOctet) {
                ba[0] = first;
                Assert.assertFalse(NetUtils.isMulticastMACAddr(ba));
                Assert.assertEquals(valid, NetUtils.isUnicastMACAddr(ba));

                ba[0] |= (byte)0x01;
                Assert.assertEquals(valid, NetUtils.isMulticastMACAddr(ba));
                Assert.assertFalse(NetUtils.isUnicastMACAddr(ba));
            }
        }
    }
}
