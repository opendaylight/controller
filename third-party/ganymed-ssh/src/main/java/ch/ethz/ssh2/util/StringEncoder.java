/*
 * Copyright (c) 2006-2011 Christian Plattner.
 * All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.util;

import java.io.UnsupportedEncodingException;

/**
 * @author Christian Plattner
 * @version $Id: StringEncoder.java 43 2011-06-21 18:34:06Z dkocher@sudo.ch $
 */
public class StringEncoder
{
	public static byte[] GetBytes(String data)
	{
        try {
            return data.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

	public static String GetString(byte[] data)
	{
		return GetString(data, 0, data.length);
	}

	public static String GetString(byte[] data, int off, int len)
	{
        try {
            return new String(data, off, len, "UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
