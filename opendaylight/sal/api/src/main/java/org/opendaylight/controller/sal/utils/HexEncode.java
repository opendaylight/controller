
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.math.BigInteger;

/**
 * The class provides methods to convert hex encode strings
 *
 *
 */
public class HexEncode {
	/**
	 * This method converts byte array into String format without ":" inserted.
	 */
    public static String bytesToHexString(byte[] bytes) {
        int i;
        String ret = "";
        String tmp;
        StringBuffer buf = new StringBuffer();
        for (i = 0; i < bytes.length; i++) {
            if (i > 0)
                ret += ":";
            short u8byte = (short) ((short) bytes[i] & 0xff);
            tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1)
                buf.append("0");
            buf.append(tmp);
        }
        ret = buf.toString();
        return ret;
    }

    public static String longToHexString(long val) {
        char arr[] = Long.toHexString(val).toCharArray();
        StringBuffer buf = new StringBuffer();
        // prepend the right number of leading zeros
        int i = 0;
        for (; i < (16 - arr.length); i++) {
            buf.append("0");
            if ((i & 0x01) == 1)
                buf.append(":");
        }
        for (int j = 0; j < arr.length; j++) {
            buf.append(arr[j]);
            if ((((i + j) & 0x01) == 1) && (j < (arr.length - 1)))
                buf.append(":");
        }
        return buf.toString();
    }

    public static byte[] bytesFromHexString(String values) {
        String[] octets = values.split(":");
        byte[] ret = new byte[octets.length];
        int i;

        for (i = 0; i < octets.length; i++)
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        return ret;
    }

    public static long stringToLong(String values) {
        long value = new BigInteger(values.replaceAll(":", ""), 16).longValue();
        return value;
    }

	/**
	 * This method converts byte array into HexString format with ":" inserted.
	 */
    public static String bytesToHexStringFormat(byte[] bytes) {
        int i;
        String ret = "";
        String tmp;
        StringBuffer buf = new StringBuffer();
        for (i = 0; i < bytes.length; i++) {
            if (i > 0)
                buf.append(":");
            short u8byte = (short) ((short) bytes[i] & 0xff);
            tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1)
                buf.append("0");
            buf.append(tmp);
        }
        ret = buf.toString();
        return ret;
    }
}
