
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;
		
import org.junit.Assert;
import org.junit.Test;
	
public class HexEncodeTest {
		
	@Test
	public void testbytesToHexString() {
		byte[] bytes1 = {(byte)0x01, (byte)0x02, (byte)0x03};		
		String str1 = HexEncode.bytesToHexString(bytes1);
		Assert.assertTrue(str1.equals("010203"));
	
		byte[] bytes2 = {(byte)0x11, (byte)0x22, (byte)0x33};		
		String str2 = HexEncode.bytesToHexString(bytes2);
		Assert.assertFalse(str2.equals("010203"));

	}

	@Test
	public void testLongToHexString() {
		long value1 = 12345678L;
		String str1 = HexEncode.longToHexString(value1);
		Assert.assertTrue(str1.equals("00:00:00:00:00:bc:61:4e"));
		
		long value2 = 98765432L;
		String str2 = HexEncode.longToHexString(value2);
		Assert.assertFalse(str2.equals("00:44:33:22:11:bc:61:4e"));

	}
	
	@Test
	public void testBytesFromHexString() {
		String byteStr1 = "00:11:22:33:44:55";
		byte byteArray1[] = new byte[(byteStr1.length() + 1)/3];
		byteArray1 = HexEncode.bytesFromHexString(byteStr1);
		
		Assert.assertTrue(byteArray1[0] == (byte)0x0);
		Assert.assertTrue(byteArray1[1] == (byte)0x11);
		Assert.assertTrue(byteArray1[2] == (byte)0x22);
		Assert.assertTrue(byteArray1[3] == (byte)0x33);
		Assert.assertTrue(byteArray1[4] == (byte)0x44);
		Assert.assertTrue(byteArray1[5] == (byte)0x55);
		
		String byteStr2 = "00:11:22:33:44:55";
		byte byteArray2[] = new byte[(byteStr2.length() + 1)/3];
		byteArray2 = HexEncode.bytesFromHexString(byteStr2);
		
		Assert.assertFalse(byteArray2[0] == (byte)0x55);
		Assert.assertFalse(byteArray2[1] == (byte)0x44);
		Assert.assertFalse(byteArray2[2] == (byte)0x33);
		Assert.assertFalse(byteArray2[3] == (byte)0x22);
		Assert.assertFalse(byteArray2[4] == (byte)0x11);
		Assert.assertFalse(byteArray2[5] == (byte)0x0);

	}
			
}



