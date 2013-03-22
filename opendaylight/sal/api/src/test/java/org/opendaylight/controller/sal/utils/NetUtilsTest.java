
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.sal.utils.NetUtils;

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
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address) == 31);

        byte address1[] = { (byte) 255, 0, 0, 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address1) == 24);

        byte address2[] = { (byte) 255, (byte) 255, (byte) 248, 0 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address2) == 11);

        byte address4[] = { (byte) 255, (byte) 255, (byte) 255, (byte) 254 };
        Assert.assertTrue(NetUtils.getSubnetMaskLength(address4) == 1);
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
}
