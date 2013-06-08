
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.sal.packet.BitBufferHelper;

public class BitBufferHelperTest {

    @Test
    public void testGetByte() {
        byte[] data = { 100 };
        Assert.assertTrue(BitBufferHelper.getByte(data) == 100);
    }

    @Test
    public void testGetBits() throws Exception {
        byte[] data = { 10, 12, 14, 20, 55, 69, 82, 97, 109, 117, 127, -50 };
        byte[] bits;

        bits = BitBufferHelper.getBits(data, 88, 8); //BYTE extraOffsetBits = extranumBits = 0
        Assert.assertTrue(bits[0] == -50);

        bits = BitBufferHelper.getBits(data, 8, 16); //Short
        Assert.assertTrue(bits[0] == 12);
        Assert.assertTrue(bits[1] == 14);

        bits = BitBufferHelper.getBits(data, 32, 32); //Int
        Assert.assertTrue(bits[0] == 55);
        Assert.assertTrue(bits[1] == 69);
        Assert.assertTrue(bits[2] == 82);
        Assert.assertTrue(bits[3] == 97);

        bits = BitBufferHelper.getBits(data, 16, 48); //Long
        Assert.assertTrue(bits[0] == 14);
        Assert.assertTrue(bits[1] == 20);
        Assert.assertTrue(bits[2] == 55);
        Assert.assertTrue(bits[3] == 69);
        Assert.assertTrue(bits[4] == 82);
        Assert.assertTrue(bits[5] == 97);

        bits = BitBufferHelper.getBits(data, 40, 7); //BYTE extraOffsetBits = extranumBits != 0
        Assert.assertTrue(bits[0] == 34);

        bits = BitBufferHelper.getBits(data, 8, 13); //Short
        Assert.assertTrue(bits[0] == 1);
        Assert.assertTrue(bits[1] == -127);

        bits = BitBufferHelper.getBits(data, 32, 28); //Int
        Assert.assertTrue(bits[0] == 3);
        Assert.assertTrue(bits[1] == 116);
        Assert.assertTrue(bits[2] == 85);
        Assert.assertTrue(bits[3] == 38);

        bits = BitBufferHelper.getBits(data, 16, 41); //Long
        Assert.assertTrue(bits[0] == 0);
        Assert.assertTrue(bits[1] == 28);
        Assert.assertTrue(bits[2] == 40);
        Assert.assertTrue(bits[3] == 110);
        Assert.assertTrue(bits[4] == -118);
        Assert.assertTrue(bits[5] == -92);

        bits = BitBufferHelper.getBits(data, 3, 7); //BYTE extraOffsetBits != 0; extranumBits == 0
        Assert.assertTrue(bits[0] == 40);

        bits = BitBufferHelper.getBits(data, 13, 16); //Short
        Assert.assertTrue(bits[0] == -127);
        Assert.assertTrue(bits[1] == -62);

        bits = BitBufferHelper.getBits(data, 5, 32); //Int
        Assert.assertTrue(bits[0] == 65);
        Assert.assertTrue(bits[1] == -127);
        Assert.assertTrue(bits[2] == -62);
        Assert.assertTrue(bits[3] == -122);

        bits = BitBufferHelper.getBits(data, 23, 48); //Long
        Assert.assertTrue(bits[0] == 10);
        Assert.assertTrue(bits[1] == 27);
        Assert.assertTrue(bits[2] == -94);
        Assert.assertTrue(bits[3] == -87);
        Assert.assertTrue(bits[4] == 48);
        Assert.assertTrue(bits[5] == -74);

        bits = BitBufferHelper.getBits(data, 66, 9); //BYTE extraOffsetBits != 0; extranumBits != 0
        Assert.assertTrue(bits[0] == 1);
        Assert.assertTrue(bits[1] == 107);

        bits = BitBufferHelper.getBits(data, 13, 15); //Short
        Assert.assertTrue(bits[0] == 64);
        Assert.assertTrue(bits[1] == -31);

        bits = BitBufferHelper.getBits(data, 5, 29); //Int
        Assert.assertTrue(bits[0] == 8);
        Assert.assertTrue(bits[1] == 48);
        Assert.assertTrue(bits[2] == 56);
        Assert.assertTrue(bits[3] == 80);

        bits = BitBufferHelper.getBits(data, 31, 43); //Long
        Assert.assertTrue(bits[0] == 0);
        Assert.assertTrue(bits[1] == -35);
        Assert.assertTrue(bits[2] == 21);
        Assert.assertTrue(bits[3] == 73);
        Assert.assertTrue(bits[4] == -123);
        Assert.assertTrue(bits[5] == -75);

        bits = BitBufferHelper.getBits(data, 4, 12); //Short
        Assert.assertTrue(bits[0] == 10);
        Assert.assertTrue(bits[1] == 12);

        byte[] data1 = { 0, 8 };
        bits = BitBufferHelper.getBits(data1, 7, 9); //Short
        Assert.assertTrue(bits[0] == 0);
        Assert.assertTrue(bits[1] == 8);

        byte[] data2 = { 2, 8 };
        bits = BitBufferHelper.getBits(data2, 0, 7); //Short
        Assert.assertTrue(bits[0] == 1);

        bits = BitBufferHelper.getBits(data2, 7, 9); //Short
        Assert.assertTrue(bits[0] == 0);
        Assert.assertTrue(bits[1] == 8);
    }

    // [01101100][01100000]
    //     [01100011]
    @Test
    public void testGetBytes() throws Exception {
        byte data[] = { 108, 96, 125, -112, 5, 6, 108, 8, 9, 10, 11, 12, 13,
                14, 15, 16, 17, 18, 19, 20, 21, 22 };
        byte[] x;

        Assert.assertTrue(BitBufferHelper.getBits(data, 0, 8)[0] == 108);
        Assert.assertTrue(BitBufferHelper.getBits(data, 8, 8)[0] == 96);

        x = BitBufferHelper.getBits(data, 0, 10);
        Assert.assertTrue(x[0] == 1);
        Assert.assertTrue(x[1] == -79);

        x = BitBufferHelper.getBits(data, 3, 8);
        Assert.assertTrue(x[0] == 99);
        //Assert.assertTrue(x[1] == 97);

    }

    @Test
    public void testMSBMask() {
        int numBits = 1; //MSB
        int mask = BitBufferHelper.getMSBMask(numBits);
        Assert.assertTrue(mask == 128);

        numBits = 8;
        mask = BitBufferHelper.getMSBMask(numBits);
        Assert.assertTrue(mask == 255);

        numBits = 2;
        mask = BitBufferHelper.getMSBMask(numBits);
        Assert.assertTrue(mask == 192);
    }

    @Test
    public void testLSBMask() {
        int numBits = 1; //LSB
        int mask = BitBufferHelper.getLSBMask(numBits);
        Assert.assertTrue(mask == 1);

        numBits = 3;
        mask = BitBufferHelper.getLSBMask(numBits);
        Assert.assertTrue(mask == 7);

        numBits = 8;
        mask = BitBufferHelper.getLSBMask(numBits);
        Assert.assertTrue(mask == 255);
    }

    @Test
    public void testToByteArray() {
        short sh = Short.MAX_VALUE;
        byte[] data_sh = new byte[Byte.SIZE / 8];
        data_sh = BitBufferHelper.toByteArray(sh);
        Assert.assertTrue(data_sh[0] == 127);
        Assert.assertTrue(data_sh[1] == -1);

        short sh2 = Short.MIN_VALUE;
        byte[] data_sh2 = new byte[Byte.SIZE / 8];
        data_sh2 = BitBufferHelper.toByteArray(sh2);
        Assert.assertTrue(data_sh2[0] == -128);
        Assert.assertTrue(data_sh2[1] == 0);

        short sh3 = 16384;
        byte[] data_sh3 = new byte[Byte.SIZE / 8];
        data_sh3 = BitBufferHelper.toByteArray(sh3);
        Assert.assertTrue(data_sh3[0] == 64);
        Assert.assertTrue(data_sh3[1] == 0);

        short sh4 = 146; //TCP headerlenflags - startoffset = 103
        byte[] data_sh4 = new byte[Byte.SIZE / 8];
        data_sh4 = BitBufferHelper.toByteArray(sh4);
        Assert.assertTrue(data_sh4[0] == 0);
        Assert.assertTrue(data_sh4[1] == -110);

        short sh4_2 = 5000; //IPv4 Offset - startOffset = 51 (to 63)
        byte[] data_sh4_2 = new byte[Byte.SIZE / 8];
        data_sh4_2 = BitBufferHelper.toByteArray(sh4_2);
        Assert.assertTrue(data_sh4_2[0] == 19);
        Assert.assertTrue(data_sh4_2[1] == -120);

        short sh4_3 = 5312; //numEndRestBits < numBitstoShiftBy
        byte[] data_sh4_3 = new byte[Byte.SIZE / 8];
        data_sh4_3 = BitBufferHelper.toByteArray(sh4_3);
        Assert.assertTrue(data_sh4_3[0] == 20);
        Assert.assertTrue(data_sh4_3[1] == -64);

        int Int = Integer.MAX_VALUE;
        byte[] data_Int = new byte[Integer.SIZE / 8];
        data_Int = BitBufferHelper.toByteArray(Int);
        Assert.assertTrue(data_Int[0] == 127);
        Assert.assertTrue(data_Int[1] == -1);
        Assert.assertTrue(data_Int[2] == -1);
        Assert.assertTrue(data_Int[3] == -1);

        int Int2 = Integer.MIN_VALUE;
        byte[] data_Int2 = new byte[Integer.SIZE / 8];
        data_Int2 = BitBufferHelper.toByteArray(Int2);
        Assert.assertTrue(data_Int2[0] == -128);
        Assert.assertTrue(data_Int2[1] == 0);
        Assert.assertTrue(data_Int2[2] == 0);
        Assert.assertTrue(data_Int2[3] == 0);

        int Int3 = 1077952576;
        byte[] data_Int3 = new byte[Integer.SIZE / 8];
        data_Int3 = BitBufferHelper.toByteArray(Int3);
        Assert.assertTrue(data_Int3[0] == 64);
        Assert.assertTrue(data_Int3[1] == 64);
        Assert.assertTrue(data_Int3[2] == 64);
        Assert.assertTrue(data_Int3[3] == 64);

        long Lng = Long.MAX_VALUE;
        byte[] data_lng = new byte[Long.SIZE / 8];
        data_lng = BitBufferHelper.toByteArray(Lng);
        Assert.assertTrue(data_lng[0] == 127);
        Assert.assertTrue(data_lng[1] == -1);
        Assert.assertTrue(data_lng[2] == -1);
        Assert.assertTrue(data_lng[3] == -1);
        Assert.assertTrue(data_lng[4] == -1);
        Assert.assertTrue(data_lng[5] == -1);
        Assert.assertTrue(data_lng[6] == -1);
        Assert.assertTrue(data_lng[7] == -1);

        long Lng2 = Long.MIN_VALUE;
        byte[] data_lng2 = new byte[Long.SIZE / 8];
        data_lng2 = BitBufferHelper.toByteArray(Lng2);
        Assert.assertTrue(data_lng2[0] == -128);
        Assert.assertTrue(data_lng2[1] == 0);
        Assert.assertTrue(data_lng2[2] == 0);
        Assert.assertTrue(data_lng2[3] == 0);
        Assert.assertTrue(data_lng2[4] == 0);
        Assert.assertTrue(data_lng2[5] == 0);
        Assert.assertTrue(data_lng2[6] == 0);
        Assert.assertTrue(data_lng2[7] == 0);

        byte B = Byte.MAX_VALUE;
        byte[] data_B = new byte[Byte.SIZE / 8];
        data_B = BitBufferHelper.toByteArray(B);
        Assert.assertTrue(data_B[0] == 127);

        byte B1 = Byte.MIN_VALUE;
        byte[] data_B1 = new byte[Byte.SIZE / 8];
        data_B1 = BitBufferHelper.toByteArray(B1);
        Assert.assertTrue(data_B1[0] == -128);

        byte B2 = 64;
        byte[] data_B2 = new byte[Byte.SIZE / 8];
        data_B2 = BitBufferHelper.toByteArray(B2);
        Assert.assertTrue(data_B2[0] == 64);

        byte B3 = 32;
        byte[] data_B3 = new byte[Byte.SIZE / 8];
        data_B3 = BitBufferHelper.toByteArray(B3);
        Assert.assertTrue(data_B3[0] == 32);

    }

    @Test
    public void testToByteArrayVariable() {
        int len = 9;
        byte[] data_sh;
        data_sh = BitBufferHelper.toByteArray(511, len);
        Assert.assertTrue(data_sh[0] == (byte) 255);
        Assert.assertTrue(data_sh[1] == (byte) 128);

        data_sh = BitBufferHelper.toByteArray((int) 511, len);
        Assert.assertTrue(data_sh[0] == (byte) 255);
        Assert.assertTrue(data_sh[1] == (byte) 128);

        data_sh = BitBufferHelper.toByteArray((long) 511, len);
        Assert.assertTrue(data_sh[0] == (byte) 255);
        Assert.assertTrue(data_sh[1] == (byte) 128);
    }

    @Test
    public void testToInt() {
        byte data[] = { 1 };
        Assert.assertTrue(BitBufferHelper.toNumber(data) == 1);

        byte data2[] = { 1, 1 };
        Assert.assertTrue(BitBufferHelper.toNumber(data2) == 257);

        byte data3[] = { 1, 1, 1 };
        Assert.assertTrue(BitBufferHelper.toNumber(data3) == 65793);
    }

    @Test
    public void testToLongGetter() {
        byte data[] = { 1, 1 };
        Assert.assertTrue(BitBufferHelper.getLong(data) == 257L);
    }

    @Test
    public void testSetByte() throws Exception {
        byte input;
        byte[] data = new byte[20];

        input = 125;
        BitBufferHelper.setByte(data, input, 0, Byte.SIZE);
        Assert.assertTrue(data[0] == 125);

        input = 109;
        BitBufferHelper.setByte(data, input, 152, Byte.SIZE);
        Assert.assertTrue(data[19] == 109);
    }

    @Test
    public void testSetBytes() throws Exception {
        byte[] input = { 0, 1 };
        byte[] data = { 6, 0 };

        BitBufferHelper.setBytes(data, input, 7, 9);
        Assert.assertTrue(data[0] == 6);
        Assert.assertTrue(data[1] == 1);
    }

    //@Test
    //INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
    // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]*/
    public void testInsertBits() throws Exception {
        //CASE 1: startOffset%8 == 0 && numBits%8 == 0
        byte inputdata[] = { 75, 110, 107, 80, 10, 12, 35, 100, 125, 65 };
        int startOffset = 0;
        int numBits = 8;

        byte data1[] = new byte[2];
        startOffset = 0;
        numBits = 16;
        BitBufferHelper.insertBits(data1, inputdata, startOffset, numBits);
        Assert.assertTrue(data1[0] == 75);
        Assert.assertTrue(data1[1] == 110);

        byte data2[] = new byte[4];
        startOffset = 0;
        numBits = 32;
        BitBufferHelper.insertBits(data2, inputdata, startOffset, numBits);
        Assert.assertTrue(data2[0] == 75);
        Assert.assertTrue(data2[1] == 110);
        Assert.assertTrue(data2[2] == 107);
        Assert.assertTrue(data2[3] == 80);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		// OUTPUT: [01001011] [01101000] = {75, 104}
        byte data10[] = new byte[2];
        startOffset = 0;
        numBits = 13;
        BitBufferHelper.insertBits(data10, inputdata, startOffset, numBits);
        Assert.assertTrue(data10[0] == 75);
        Assert.assertTrue(data10[1] == 104);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		// OUTPUT: [01001000] = {72}
        byte data11[] = new byte[4];
        startOffset = 8;
        numBits = 6;
        BitBufferHelper.insertBits(data11, inputdata, startOffset, numBits);
        Assert.assertTrue(data11[1] == 72);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [01001011] [01101110] [01101000] = {75, 110, 105}
        byte data12[] = new byte[4];
        startOffset = 0;
        numBits = 23;
        BitBufferHelper.insertBits(data12, inputdata, startOffset, numBits);
        Assert.assertTrue(data12[0] == 75);
        Assert.assertTrue(data12[1] == 110);
        Assert.assertTrue(data12[2] == 106);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [01001011] [01101110] [01100000] = {75, 110, 96}
        byte data13[] = new byte[4];
        startOffset = 8;
        numBits = 20;
        BitBufferHelper.insertBits(data13, inputdata, startOffset, numBits);
        Assert.assertTrue(data13[1] == 75);
        Assert.assertTrue(data13[2] == 110);
        Assert.assertTrue(data13[3] == 96);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [01001011] [01101110] [01101011] [10100000]= {75, 110, 107, 80}
        byte data14[] = new byte[4];
        startOffset = 0;
        numBits = 30;
        BitBufferHelper.insertBits(data14, inputdata, startOffset, numBits);
        Assert.assertTrue(data14[0] == 75);
        Assert.assertTrue(data14[1] == 110);
        Assert.assertTrue(data14[2] == 107);
        Assert.assertTrue(data14[3] == 80);

        //CASE 3: startOffset%8 != 0, numBits%8 = 0
        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [10100000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00001001] [11000000] = {72, 96}
        byte data16[] = new byte[5];
        startOffset = 3;
        numBits = 8;
        BitBufferHelper.insertBits(data16, inputdata, startOffset, numBits);
        Assert.assertTrue(data16[0] == 9);
        Assert.assertTrue(data16[1] == 96);
        Assert.assertTrue(data16[2] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [00000100] [1011 0110] [1110 0000] = {4, -54, -96}

        startOffset = 3;
        numBits = 16;
        byte data17[] = new byte[5];
        BitBufferHelper.insertBits(data17, inputdata, startOffset, numBits);
        Assert.assertTrue(data17[0] == 9);
        Assert.assertTrue(data17[1] == 109);
        Assert.assertTrue(data17[2] == -64);
        Assert.assertTrue(data17[3] == 0);

        // INPUT: {79, 110, 111}
        // = [01001111] [01101110] [01101111]
        //OUTPUT: [0000 1001] [1110 1101] [110 00000] = {9, -19, -64}
        byte data18[] = new byte[5];
        byte inputdata3[] = { 79, 110, 111 };
        startOffset = 3;
        numBits = 16;
        BitBufferHelper.insertBits(data18, inputdata3, startOffset, numBits);
        Assert.assertTrue(data18[0] == 9);
        Assert.assertTrue(data18[1] == -19);
        Assert.assertTrue(data18[2] == -64);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 1001] [0110 1101] [1100 1101] [0110 1010] [0000 0001] = {9, 109, -51, 106, 0}

        startOffset = 3;
        numBits = 32;
        byte data19[] = new byte[5];
        BitBufferHelper.insertBits(data19, inputdata, startOffset, numBits);
        Assert.assertTrue(data19[0] == 9);
        Assert.assertTrue(data19[1] == 109);
        Assert.assertTrue(data19[2] == -51);
        Assert.assertTrue(data19[3] == 106);
        Assert.assertTrue(data19[4] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: data[4, 5, 6] = [0 010 0101] [1 011 0111] [0 000 0000] = {37, -73, 0}
        startOffset = 33;
        numBits = 16;
        byte data20[] = new byte[7];
        BitBufferHelper.insertBits(data20, inputdata, startOffset, numBits);
        Assert.assertTrue(data20[4] == 37);
        Assert.assertTrue(data20[5] == -73);
        Assert.assertTrue(data20[6] == 0);

        //CASE 4: extranumBits != 0 AND extraOffsetBits != 0
        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 1001] [0100 0000]  = {9, 96}
        startOffset = 3;
        numBits = 7;
        byte data21[] = new byte[7];
        BitBufferHelper.insertBits(data21, inputdata, startOffset, numBits);
        Assert.assertTrue(data21[0] == 9);
        Assert.assertTrue(data21[1] == 64);
        Assert.assertTrue(data21[2] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: data = [00000 010] [01011 011] [01110 000] = {37, -73, 0}
        startOffset = 5;
        numBits = 17;
        byte data22[] = new byte[7];
        BitBufferHelper.insertBits(data22, inputdata, startOffset, numBits);
        Assert.assertTrue(data22[0] == 2);
        Assert.assertTrue(data22[1] == 91);
        Assert.assertTrue(data22[2] == 112);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 1001] [0110 1101] [110 01101] [01 00000] = {9, 109, -51, 64}
        startOffset = 3;
        numBits = 23;
        byte data23[] = new byte[7];
        BitBufferHelper.insertBits(data23, inputdata, startOffset, numBits);
        Assert.assertTrue(data23[0] == 9);
        Assert.assertTrue(data23[1] == 109);
        Assert.assertTrue(data23[2] == -51);
        Assert.assertTrue(data23[3] == 64);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 1001] [0110 1101]  = {9, 109}
        startOffset = 3;
        numBits = 13;
        byte data24[] = new byte[7];
        BitBufferHelper.insertBits(data24, inputdata, startOffset, numBits);
        Assert.assertTrue(data24[0] == 9);
        Assert.assertTrue(data24[1] == 109);
        Assert.assertTrue(data24[2] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 0100] [1011 0110] [1110 0110]  = {4, -74, -26}
        startOffset = 4;
        numBits = 20;
        byte data25[] = new byte[7];
        BitBufferHelper.insertBits(data25, inputdata, startOffset, numBits);
        Assert.assertTrue(data25[0] == 4);
        Assert.assertTrue(data25[1] == -74);
        Assert.assertTrue(data25[2] == -26);
        Assert.assertTrue(data25[3] == -0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [0000 0010] [0101 1011]   = {0, 2, 91, 0}
        startOffset = 13;
        numBits = 11;
        byte data26[] = new byte[7];
        BitBufferHelper.insertBits(data26, inputdata, startOffset, numBits);
        Assert.assertTrue(data26[0] == 0);
        Assert.assertTrue(data26[1] == 2);
        Assert.assertTrue(data26[2] == 91);
        Assert.assertTrue(data26[3] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [000 01001] [011 01101] [110 0 0000]   = {9, 109, -64, 0}
        startOffset = 3;
        numBits = 17;
        byte data27[] = new byte[7];
        BitBufferHelper.insertBits(data27, inputdata, startOffset, numBits);
        Assert.assertTrue(data27[0] == 9);
        Assert.assertTrue(data27[1] == 109);
        Assert.assertTrue(data27[2] == -64);
        Assert.assertTrue(data27[3] == 0);

        // INPUT: {75, 110, 107, 80, 10, 12, 35, 100, 125, 65} =
        // [01001011] [01101110] [01101011] [01010000] [00001010] [00001100] [00100011] [01100100] [11111101] [01000001]		//OUTPUT: [00000000] [00000100] [10110110] [11100000]= {0, 4, -54, -96}
        // OUTPUT: [00 000000] [00 000000] [00 010010] [11 011011] [10 011010] [11 010100] [0000 0000] = {0, 0, 18, -37,-102,-44,0}
        startOffset = 18;
        numBits = 34;
        byte data28[] = new byte[7];
        BitBufferHelper.insertBits(data28, inputdata, startOffset, numBits);
        Assert.assertTrue(data28[0] == 0);
        Assert.assertTrue(data28[1] == 0);
        Assert.assertTrue(data28[2] == 18);
        Assert.assertTrue(data28[3] == -37);
        Assert.assertTrue(data28[4] == -102);
        Assert.assertTrue(data28[5] == -44);
        Assert.assertTrue(data28[6] == 0);

    }

    @Test
    public void testGetShort() throws Exception {
        byte data[] = new byte[2];
        data[0] = 7;
        data[1] = 8;
        int length = 9; // num bits
        Assert.assertTrue(BitBufferHelper.getShort(data, length) == 264);

        data[0] = 6;
        data[1] = 8;
        short result = BitBufferHelper.getShort(data, length);
        Assert.assertTrue(result == 8);

        data[0] = 8;
        data[1] = 47;
        result = BitBufferHelper.getShort(data, length);
        Assert.assertTrue(result == 47);

        //[0000 0001] [0001 0100] [0110 0100]
        byte[] data1 = new byte[2];
        data1[0] = 1;
        data1[1] = 20; //data1[2] = 100;
        length = 15;
        result = BitBufferHelper.getShort(data1, length);
        Assert.assertTrue(result == 276);

        byte[] data2 = new byte[2];
        data2[0] = 64;
        data2[1] = 99; //data2[2] = 100;
        length = 13;
        result = BitBufferHelper.getShort(data2, length);
        Assert.assertTrue(result == 99);

        byte[] data3 = { 100, 50 };
        result = BitBufferHelper.getShort(data3);
        Assert.assertTrue(result == 25650);
    }

    @Test
    public void testToIntVarLength() throws Exception {
        byte data[] = { (byte) 255, (byte) 128 };
        int length = 9; // num bits
        Assert.assertTrue(BitBufferHelper.getInt(data, length) == 384);

        byte data2[] = { 0, 8 };
        Assert.assertTrue(BitBufferHelper.getInt(data2, 9) == 8);

        byte data3[] = { 1, 1, 1 };
        Assert.assertTrue(BitBufferHelper.getInt(data3) == 65793);

        byte data4[] = { 1, 1, 1 };
        Assert.assertTrue(BitBufferHelper.getInt(data4) == 65793);

        byte data5[] = { 1, 1 };
        Assert.assertTrue(BitBufferHelper.getInt(data5) == 257);

    }

    @Test
    public void testShiftBitstoLSB() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        byte[] data2 = { 8, 9, 10 };
        byte[] shiftedBytes2 = BitBufferHelper.shiftBitsToLSB(data2, 11);

        Assert.assertTrue(shiftedBytes2[0] == 0);
        Assert.assertTrue(shiftedBytes2[1] == 64);
        Assert.assertTrue(shiftedBytes2[2] == 72);

        byte[] shiftedBytes = BitBufferHelper.shiftBitsToLSB(data, 49);

        Assert.assertTrue(shiftedBytes[0] == 0);
        Assert.assertTrue(shiftedBytes[1] == 2);
        Assert.assertTrue(shiftedBytes[2] == 4);
        Assert.assertTrue(shiftedBytes[3] == 6);
        Assert.assertTrue(shiftedBytes[4] == 8);
        Assert.assertTrue(shiftedBytes[5] == 10);
        Assert.assertTrue(shiftedBytes[6] == 12);
        Assert.assertTrue(shiftedBytes[7] == 14);
        Assert.assertTrue(shiftedBytes[8] == 16);
        Assert.assertTrue(shiftedBytes[9] == 18);

        byte[] data1 = { 1, 2, 3 };
        byte[] shiftedBytes1 = BitBufferHelper.shiftBitsToLSB(data1, 18);
        Assert.assertTrue(shiftedBytes1[0] == 0);
        Assert.assertTrue(shiftedBytes1[1] == 4);
        Assert.assertTrue(shiftedBytes1[2] == 8);

    }

    @Test
    public void testShiftBitstoLSBMSB() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };

        byte[] clone = BitBufferHelper.shiftBitsToMSB(BitBufferHelper
                .shiftBitsToLSB(data, 72), 72);

        Assert.assertTrue(clone[0] == 1);
        Assert.assertTrue(clone[1] == 2);
        Assert.assertTrue(clone[2] == 3);
        Assert.assertTrue(clone[3] == 4);
        Assert.assertTrue(clone[4] == 5);
        Assert.assertTrue(clone[5] == 6);
        Assert.assertTrue(clone[6] == 7);
        Assert.assertTrue(clone[7] == 8);
        Assert.assertTrue(clone[8] == 9);
        Assert.assertTrue(clone[9] == 0);
    }

}
