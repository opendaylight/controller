
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
@Deprecated
public class HexEncode {
    /**
     * This method converts byte array into String format without ":" inserted.
     *
     * @param bytes
     *            The byte array to convert to string
     * @return The hexadecimal representation of the byte array. If bytes is
     *         null, "null" string is returned
     */
    public static String bytesToHexString(byte[] bytes) {

        if (bytes == null) {
            return "null";
        }

        String ret = "";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                ret += ":";
            }
            short u8byte = (short) (bytes[i] & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1) {
                buf.append("0");
            }
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
            if ((i & 0x01) == 1) {
                buf.append(":");
            }
        }
        for (int j = 0; j < arr.length; j++) {
            buf.append(arr[j]);
            if ((((i + j) & 0x01) == 1) && (j < (arr.length - 1))) {
                buf.append(":");
            }
        }
        return buf.toString();
    }


    public static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
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
        if (bytes == null) {
            return "null";
        }
        String ret = "";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                buf.append(":");
            }
            short u8byte = (short) (bytes[i] & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1) {
                buf.append("0");
            }
            buf.append(tmp);
        }
        ret = buf.toString();
        return ret;
    }
}
