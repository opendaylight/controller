
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class containing the common utility functions needed
 * for operating on networking data structures
 *
 *
 *
 */
public abstract class NetUtils {
    /**
     * Constant holding the number of bits in a byte
     */
    public static final int NumBitsInAByte = 8;

    /**
     * Converts a 4 bytes array into an integer number
     *
     * @param ba	the 4 bytes long byte array
     * @return	    the integer number
     */
    public static int byteArray4ToInt(byte[] ba) {
        if (ba == null || ba.length != 4)
            return 0;
        return (int) ((0xff & ba[0]) << 24 | (0xff & ba[1]) << 16
                | (0xff & ba[2]) << 8 | (0xff & ba[3]));
    }

    /**
     * Converts an integer number into a 4 bytes array
     *
     * @param i the integer number
     * @return  the byte array
     */
    public static byte[] intToByteArray4(int i) {
        return new byte[] { (byte) ((i >> 24) & 0xff),
                (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff),
                (byte) (i & 0xff) };
    }

    /**
     * Converts an IP address passed as integer value into the
     * respective InetAddress object
     *
     * @param address	the IP address in integer form
     * @return			the IP address in InetAddress form
     */
    public static InetAddress getInetAddress(int address) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByAddress(NetUtils.intToByteArray4(address));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * Return the InetAddress Network Mask given the length of the prefix bit mask.
     * The prefix bit mask indicates the contiguous leading bits that are NOT masked out.
     * Example: A prefix bit mask length of 8 will give an InetAddress Network Mask of 255.0.0.0
     *
     * @param prefixMaskLength	integer representing the length of the prefix network mask
     * @param isV6				boolean representing the IP version of the returned address
     * @return
     */
    public static InetAddress getInetNetworkMask(int prefixMaskLength,
            boolean isV6) {
        if (prefixMaskLength < 0 || (!isV6 && prefixMaskLength > 32)
                || (isV6 && prefixMaskLength > 128)) {
            return null;
        }
        byte v4Address[] = { 0, 0, 0, 0 };
        byte v6Address[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte address[] = (isV6) ? v6Address : v4Address;
        int numBytes = prefixMaskLength / 8;
        int numBits = prefixMaskLength % 8;
        int i = 0;
        for (; i < numBytes; i++) {
            address[i] = (byte) 0xff;
        }
        if (numBits > 0) {
            int rem = 0;
            for (int j = 0; j < numBits; j++) {
                rem |= 1 << (7 - j);
            }
            address[i] = (byte) rem;
        }

        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the number of contiguous bits belonging to the subnet, that have to be masked out
     * Example: A prefix network byte mask of ff.ff.ff.00 will give a subnet mask length of 8,
     * while ff.00.00.00 will return a subnet mask length of 24.
     * If the passed prefixMask object is null, 0 is returned
     *
     * @param prefixMask	the prefix mask as byte array
     * @return				the length of the prefix network mask
     */
    public static int getSubnetMaskLength(byte[] prefixMask) {
        int maskLength = 0;
        if (prefixMask != null) {
            // Create bit mask
            int intMask = 0;
            int numBytes = prefixMask.length;
            for (int i = 0; i < numBytes; i++) {
                intMask |= ((int) prefixMask[i] & 0xff) << (8 * (numBytes - 1 - i));
            }

            int bit = 1;
            while (((intMask & bit) == 0) && (maskLength <= (numBytes * 8))) {
                maskLength += 1;
                bit = bit << 1;
            }
        }
        return maskLength;
    }

    /**
     * Returns the number of contiguous bits belonging to the subnet, that have to be masked out
     * Example: A prefix network byte mask of ff.ff.ff.00 will give a subnet mask length of 8,
     * while ff.00.00.00 will return a subnet mask length of 24
     * If the passed prefixMask object is null, 0 is returned
     *
     * @param prefixMask	the prefix mask as InetAddress
     * @return				the length of the prefix network mask
     */
    public static int getSubnetMaskLength(InetAddress prefixMask) {
        return (prefixMask == null) ? 0 : NetUtils
                .getSubnetMaskLength(prefixMask.getAddress());
    }

    /**
     * Given an IP address and a prefix network mask length, it returns
     * the equivalent subnet prefix IP address
     * Example: for ip = "172.28.30.254" and maskLen = 25 it will return "172.28.30.128"
     *
     * @param ip		the IP address in InetAddress form
     * @param maskLen	the length of the prefix network mask
     * @return			the subnet prefix IP address in InetAddress form
     */
    public static InetAddress getSubnetPrefix(InetAddress ip, int maskLen) {
        int bytes = maskLen / 8;
        int bits = maskLen % 8;
        byte modifiedByte;
        byte[] sn = ip.getAddress();
        if (bits > 0) {
            modifiedByte = (byte) (sn[bytes] >> (8 - bits));
            sn[bytes] = (byte) (modifiedByte << (8 - bits));
            bytes++;
        }
        for (; bytes < sn.length; bytes++) {
            sn[bytes] = (byte) (0);
        }
        try {
            return InetAddress.getByAddress(sn);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Checks if the test address and mask conflicts with
     * the filter address and mask
     *
     * For example:
     * testAddress: 172.28.2.23 testMask: 255.255.255.0
     * filtAddress: 172.28.1.10 testMask: 255.255.255.0
     * conflict
     *
     * testAddress: 172.28.2.23 testMask: 255.255.255.0
     * filtAddress: 172.28.1.10 testMask: 255.255.0.0
     * do not conflict
     *
     * Null parameters are permitted
     *
     * @param testAddress
     * @param filterAddress
     * @param testMask
     * @param filterMask
     * @return
     */
    public static boolean inetAddressConflict(InetAddress testAddress,
            InetAddress filterAddress, InetAddress testMask,
            InetAddress filterMask) {
        // Sanity check
        if ((testAddress == null) || (filterAddress == null)) {
            return false;
        }

        // Presence check
        if (isAny(testAddress) || isAny(filterAddress)) {
            return false;
        }

        // Derive the masks length. A null mask means a full mask
        int testMaskLen = (testMask != null) ? NetUtils
                .getSubnetMaskLength(testMask.getAddress())
                : (testAddress instanceof Inet6Address) ? 128 : 32;
        int filterMaskLen = (filterMask != null) ? NetUtils
                .getSubnetMaskLength(filterMask.getAddress())
                : (filterAddress instanceof Inet6Address) ? 128 : 32;

        // Mask length check. Test mask has to be more generic than filter one
        if (testMaskLen < filterMaskLen) {
            return true;
        }

        // Subnet Prefix on filter mask length must be the same
        InetAddress prefix1 = getSubnetPrefix(testAddress, filterMaskLen);
        InetAddress prefix2 = getSubnetPrefix(filterAddress, filterMaskLen);
        return (!prefix1.equals(prefix2));
    }

    /**
     * Returns true if the passed MAC address is all zero
     *
     * @param mac	the byte array representing the MAC address
     * @return		true if all MAC bytes are zero
     */
    public static boolean isZeroMAC(byte[] mac) {
        for (short i = 0; i < 6; i++) {
            if (mac[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the passed InetAddress contains all zero
     *
     * @param ip	the IP address to test
     * @return		true if the address is all zero
     */
    public static boolean isAny(InetAddress ip) {
        for (byte b : ip.getAddress()) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean fieldsConflict(int field1, int field2) {
        if ((field1 == 0) || (field2 == 0) || (field1 == field2)) {
            return false;
        }
        return true;
    }

    public static InetAddress parseInetAddress(String addressString) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return address;
    }

    /**
     * Checks if the passed IP v4 address in string form is valid
     * The address may specify a mask at the end as "/MM"
     *
     * @param cidr the v4 address as A.B.C.D/MM
     * @return
     */
    public static boolean isIPv4AddressValid(String cidr) {
        if (cidr == null)
            return false;

        String values[] = cidr.split("/");
        Pattern ipv4Pattern = Pattern
                .compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
        Matcher mm = ipv4Pattern.matcher(values[0]);
        if (!mm.matches()) {
            return false;
        }
        if (values.length >= 2) {
            int prefix = Integer.valueOf(values[1]);
            if ((prefix < 0) || (prefix > 32)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the passed IP v6 address in string form is valid
     * The address may specify a mask at the end as "/MMM"
     *
     * @param cidr the v6 address as A::1/MMM
     * @return
     */
    public static boolean isIPv6AddressValid(String cidr) {
        if (cidr == null)
            return false;

        String values[] = cidr.split("/");
        try {
            //when given an IP address, InetAddress.getByName validates the ip address
            InetAddress addr = InetAddress.getByName(values[0]);
            if (!(addr instanceof Inet6Address)) {
                return false;
            }
        } catch (UnknownHostException ex) {
            return false;
        }

        if (values.length >= 2) {
            int prefix = Integer.valueOf(values[1]);
            if ((prefix < 0) || (prefix > 128)) {
                return false;
            }
        }
        return true;
    }
    
    /*
     * Following utilities are useful when you need to 
     * compare or bit shift java primitive type variable
     * which are inerently signed
     */
    /**
     * Returns the unsigned value of the passed byte variable
     * 
     * @param b	the byte value
     * @return the int variable containing the unsigned byte value
     */
    public static int getUnsignedByte(byte b) {
		return (b > 0)? (int)b : ((int)b & 0x7F | 0x80);
	}
	
    /**
     * Return the unsigned value of the passed short variable
     * 
     * @param s the short value
     * @return the int variable containing the unsigned short value
     */
	public static int getUnsignedShort(short s) {
		return (s > 0)? (int)s : ((int)s & 0x7FFF | 0x8000);
	}
}
