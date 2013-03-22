
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 *
 */
package org.opendaylight.controller.sal.packet;

import java.util.Arrays;

import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * BitBufferHelper class that provides utility methods to
 * - fetch specific bits from a serialized stream of bits
 * - convert bits to primitive data type - like short, int, long
 * - store bits in specified location in stream of bits
 * - convert primitive data types to stream of bits
 *
 *
 */
public abstract class BitBufferHelper {

    public static long ByteMask = 0xFF;

    // Getters
    // data: array where data are stored
    // startOffset: bit from where to start reading
    // numBits: number of bits to read
    // All this function return an exception if overflow or underflow

    /**
     * Returns the first byte from the byte array
     * @param byte[] data
     * @return byte value
     */
    public static byte getByte(byte[] data) {
        if ((data.length * NetUtils.NumBitsInAByte) > Byte.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (data[0]);
    }

    /**
     * Returns the short value for the byte array passed.
     * Size of byte array is restricted to Short.SIZE
     * @param byte[] data
     * @return short value
     */
    public static short getShort(byte[] data) {
        if (data.length > Short.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (short) toNumber(data);
    }

    /**
     * Returns the int value for the byte array passed.
     * Size of byte array is restricted to Integer.SIZE
     * @param byte[] data
     * @return int - the integer value of byte array
     */
    public static int getInt(byte[] data) {
        if (data.length > Integer.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (int) toNumber(data);
    }

    /**
     * Returns the long value for the byte array passed.
     * Size of byte array is restricted to Long.SIZE
     * @param byte[] data
     * @return long - the integer value of byte array
     */
    public static long getLong(byte[] data) {
        if (data.length > Long.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (long) toNumber(data);
    }

    /**
     * Returns the short value for the last numBits of the byte array passed.
     * Size of numBits is restricted to Short.SIZE
     * @param byte[] data
     * @param int - numBits
     * @return short - the short value of byte array
     * @throws Exception
     */
    public static short getShort(byte[] data, int numBits) throws Exception {
        if (numBits > Short.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int startOffset = data.length * NetUtils.NumBitsInAByte - numBits;
        return (short) toNumber(BitBufferHelper.getBits(data, startOffset,
                numBits), numBits);
    }

    /**
     * Returns the int value for the last numBits of the byte array passed.
     * Size of numBits is restricted to Integer.SIZE
     * @param byte[] data
     * @param int - numBits
     * @return int - the integer value of byte array
     * @throws Exception
     */
    public static int getInt(byte[] data, int numBits) throws Exception {
        if (numBits > Integer.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requiested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int startOffset = data.length * NetUtils.NumBitsInAByte - numBits;
        return (int) toNumber(BitBufferHelper.getBits(data, startOffset,
                numBits), numBits);
    }

    /**
     * Returns the long value for the last numBits of the byte array passed.
     * Size of numBits is restricted to Long.SIZE
     * @param byte[] data
     * @param int - numBits
     * @return long - the integer value of byte array
     * @throws Exception
     */

    public static long getLong(byte[] data, int numBits) throws Exception {
        if (numBits > Long.SIZE) {
            try {
                throw new Exception(
                        "Container is too small for the number of requested bits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (numBits > data.length * NetUtils.NumBitsInAByte) {
            try {
                throw new Exception(
                        "Trying to read more bits than contained in the data buffer");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int startOffset = data.length * NetUtils.NumBitsInAByte - numBits;
        return toNumber(BitBufferHelper.getBits(data, startOffset, numBits),
                numBits);
    }

    /**
     * Reads the specified number of bits from the passed byte array
     * starting to read from the specified offset
     * The bits read are stored in a byte array which size is dictated
     * by the number of bits to be stored.
     * The bits are stored in the byte array LSB aligned.
     *
     * Ex.
     * Read 7 bits at offset 10
     * 0         9 10     16 17
     * 0101000010 | 0000101 | 1111001010010101011
     * will be returned as {0,0,0,0,0,1,0,1}
     *
     * @param byte[] data
     * @param int startOffset - offset to start fetching bits from data from
     * @param int numBits - number of bits to be fetched from data
     * @return byte [] - LSB aligned bits
     * @throws Exception
     */
    public static byte[] getBits(byte[] data, int startOffset, int numBits)
            throws Exception {

        int startByteOffset = 0;
        int valfromcurr, valfromnext;
        int extranumBits = numBits % NetUtils.NumBitsInAByte;
        int extraOffsetBits = startOffset % NetUtils.NumBitsInAByte;
        int numBytes = (numBits % NetUtils.NumBitsInAByte != 0) ? 1 + numBits
                / NetUtils.NumBitsInAByte : numBits / NetUtils.NumBitsInAByte;
        byte[] shiftedBytes = new byte[numBytes];
        startByteOffset = startOffset / NetUtils.NumBitsInAByte;
        byte[] bytes = new byte[numBytes];
        if (numBits == 0)
            return bytes;

        checkExceptions(data, startOffset, numBits);

        if (extraOffsetBits == 0) {
            if (extranumBits == 0) {
                System.arraycopy(data, startByteOffset, bytes, 0, numBytes);
                return bytes;
            } else {
                System.arraycopy(data, startByteOffset, bytes, 0, numBytes - 1);
                bytes[numBytes - 1] = (byte) ((int) data[startByteOffset
                        + numBytes - 1] & getMSBMask(extranumBits));
            }
        } else {
            int i;
            for (i = 0; i < numBits / NetUtils.NumBitsInAByte; i++) {
                // Reading Numbytes starting from offset
                valfromcurr = (data[startByteOffset + i])
                        & getLSBMask(NetUtils.NumBitsInAByte - extraOffsetBits);
                valfromnext = (data[startByteOffset + i + 1])
                        & getMSBMask(extraOffsetBits);
                bytes[i] = (byte) (valfromcurr << (extraOffsetBits) | (valfromnext >> (NetUtils.NumBitsInAByte - extraOffsetBits)));
            }
            // Now adding the rest of the bits if any
            if (extranumBits != 0) {
                if (extranumBits < (NetUtils.NumBitsInAByte - extraOffsetBits)) {
                    valfromnext = (byte) (data[startByteOffset + i + 1] & ((getMSBMask(extranumBits)) >> extraOffsetBits));
                    bytes[i] = (byte) (valfromnext << extraOffsetBits);
                } else if (extranumBits == (NetUtils.NumBitsInAByte - extraOffsetBits)) {
                    valfromcurr = (data[startByteOffset + i])
                            & getLSBMask(NetUtils.NumBitsInAByte
                                    - extraOffsetBits);
                    bytes[i] = (byte) (valfromcurr << extraOffsetBits);
                } else {
                    valfromcurr = (data[startByteOffset + i])
                            & getLSBMask(NetUtils.NumBitsInAByte
                                    - extraOffsetBits);
                    valfromnext = (data[startByteOffset + i + 1])
                            & (getMSBMask(extranumBits
                                    - (NetUtils.NumBitsInAByte - extraOffsetBits)));
                    bytes[i] = (byte) (valfromcurr << (extraOffsetBits) | (valfromnext >> (NetUtils.NumBitsInAByte - extraOffsetBits)));
                }

            }
        }
        // Aligns the bits to LSB
        shiftedBytes = shiftBitsToLSB(bytes, numBits);
        return shiftedBytes;
    }

    // Setters
    // data: array where data will be stored
    // input: the data that need to be stored in the data array
    // startOffset: bit from where to start writing
    // numBits: number of bits to read

    /**
     * Bits are expected to be stored in the input byte array from LSB
     * @param byte[] - data to set the input byte
     * @param byte - input byte to be inserted
     * @param startOffset - offset of data[] to start inserting byte from
     * @param numBits - number of bits of input to be inserted into data[]
     * @return void
     * @throws Exception
     */
    public static void setByte(byte[] data, byte input, int startOffset,
            int numBits) throws Exception {
        byte[] inputByteArray = new byte[1];
        Arrays.fill(inputByteArray, 0, 1, input);
        setBytes(data, inputByteArray, startOffset, numBits);
    }

    /**
     * Bits are expected to be stored in the input byte array from LSB
     * @param byte[] - data to set the input byte
     * @param byte[] - input bytes to be inserted
     * @param startOffset - offset of data[] to start inserting byte from
     * @param numBits - number of bits of input to be inserted into data[]
     * @return void
     * @throws Exception
     */
    public static void setBytes(byte[] data, byte[] input, int startOffset,
            int numBits) throws Exception {
        checkExceptions(data, startOffset, numBits);
        insertBits(data, input, startOffset, numBits);
    }

    /**
     * Returns numBits 1's in the MSB position
     * @param numBits
     * @return
     */
    public static int getMSBMask(int numBits) {
        int mask = 0;
        for (int i = 0; i < numBits; i++) {
            mask = mask | (1 << (7 - i));
        }
        return mask;
    }

    /**
     * Returns numBits 1's in the LSB position
     * @param numBits
     * @return
     */
    public static int getLSBMask(int numBits) {
        int mask = 0;
        for (int i = 0; i < numBits; i++) {
            mask = mask | (1 << i);
        }
        return mask;
    }

    /**
     * Returns the numerical value of the byte array passed
     * @param byte[] - array
     * @return long - numerical value of byte array passed
     */
    static public long toNumber(byte[] array) {
        long ret = 0;
        long length = array.length;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = array[i];
            if (value < 0)
                value += 256;
            ret = ret
                    | (long) ((long) value << ((length - i - 1) * NetUtils.NumBitsInAByte));
        }
        return ret;
    }

    /**
     * Returns the numerical value of the last numBits (LSB bits)
     * of the byte array passed
     * @param byte[] - array
     * @param int - numBits
     * @return long - numerical value of byte array passed
     */
    static public long toNumber(byte[] array, int numBits) {
        int length = numBits / NetUtils.NumBitsInAByte;
        int bitsRest = numBits % NetUtils.NumBitsInAByte;
        int startOffset = array.length - length;
        long ret = 0;
        int value = 0;

        value = array[startOffset - 1] & getLSBMask(bitsRest);
        value = (array[startOffset - 1] < 0) ? (array[startOffset - 1] + 256)
                : array[startOffset - 1];
        ret = ret
                | (value << ((array.length - startOffset) * NetUtils.NumBitsInAByte));

        for (int i = startOffset; i < array.length; i++) {
            value = array[i];
            if (value < 0)
                value += 256;
            ret = ret
                    | (long) ((long) value << ((array.length - i - 1) * NetUtils.NumBitsInAByte));
        }

        return ret;
    }

    /**
     * Accepts a number as input and returns its value in byte form
     * in LSB aligned form
     * example: 	input = 5000 [1001110001000]
     * 	bytes = 19, -120 [00010011] [10001000]
     * @param Number
     * @return byte[]
     *
     */

    public static byte[] toByteArray(Number input) {
        Class<? extends Number> dataType = input.getClass();
        short size = 0;
        long Lvalue = input.longValue();

        if (dataType == Byte.class || dataType == byte.class)
            size = Byte.SIZE;
        else if (dataType == Short.class || dataType == short.class)
            size = Short.SIZE;
        else if (dataType == Integer.class || dataType == int.class)
            size = Integer.SIZE;
        else if (dataType == Long.class || dataType == long.class)
            size = Long.SIZE;
        else
            throw new IllegalArgumentException(
                    "Parameter must one of the following: Short/Int/Long\n");

        int length = size / NetUtils.NumBitsInAByte;
        byte bytes[] = new byte[length];

        /*Getting the bytes from input value*/
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((Lvalue >> (NetUtils.NumBitsInAByte * (length
                    - i - 1))) & ByteMask);
        }
        return bytes;
    }

    /**
     * Accepts a number as input and returns its value in byte form
     * in MSB aligned form
     * example: input = 5000 [1001110001000]
     * 		bytes = -114, 64 [10011100] [01000000]
     * @param Number input
     * @param int numBits - the number of bits to be returned
     * @return byte[]
     *
     */
    public static byte[] toByteArray(Number input, int numBits) {
        Class<? extends Number> dataType = input.getClass();
        short size = 0;
        long Lvalue = input.longValue();

        if (dataType == Short.class) {
            size = Short.SIZE;
        } else if (dataType == Integer.class) {
            size = Integer.SIZE;
        } else if (dataType == Long.class) {
            size = Long.SIZE;
        } else {
            throw new IllegalArgumentException(
                    "Parameter must one of the following: Short/Int/Long\n");
        }

        int length = size / NetUtils.NumBitsInAByte;
        byte bytes[] = new byte[length];
        byte[] inputbytes = new byte[length];
        byte shiftedBytes[];

        //Getting the bytes from input value
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((Lvalue >> (NetUtils.NumBitsInAByte * (length
                    - i - 1))) & ByteMask);
        }

        if ((bytes[0] == 0 && dataType == Long.class)
                || (bytes[0] == 0 && dataType == Integer.class)) {
            int index = 0;
            for (index = 0; index < length; ++index) {
                if (bytes[index] != 0) {
                    bytes[0] = bytes[index];
                    break;
                }
            }
            System.arraycopy(bytes, index, inputbytes, 0, length - index);
            Arrays.fill(bytes, length - index + 1, length - 1, (byte) 0);
        } else {
            System.arraycopy(bytes, 0, inputbytes, 0, length);
        }

        shiftedBytes = shiftBitsToMSB(inputbytes, numBits);

        return shiftedBytes;
    }

    /**
     * Takes an LSB aligned byte array and returned the LSB numBits in a MSB aligned byte array
     *
     * @param inputbytes
     * @param numBits
     * @return
     */
    /**
     * It aligns the last numBits bits to the head of the byte array
     * following them with numBits % 8 zero bits.
     *
     * Example:
     * For inputbytes = [00000111][01110001] and numBits = 12 it returns:
     *     shiftedBytes = [01110111][00010000]
     *
     * @param byte[] inputBytes
     * @param int numBits - number of bits to be left aligned
     * @return byte[]
     */
    public static byte[] shiftBitsToMSB(byte[] inputBytes, int numBits) {
        int numBitstoShiftBy = 0, leadZeroesMSB = 8, numEndRestBits = 0;
        int size = inputBytes.length;
        byte[] shiftedBytes = new byte[size];
        int i;

        for (i = 0; i < Byte.SIZE; i++) {
            if (((byte) (inputBytes[0] & getMSBMask(i + 1))) != 0) {
                leadZeroesMSB = i;
                break;
            }
        }

        if (numBits % NetUtils.NumBitsInAByte == 0)
            numBitstoShiftBy = 0;
        else
            numBitstoShiftBy = ((NetUtils.NumBitsInAByte - (numBits % NetUtils.NumBitsInAByte)) < leadZeroesMSB) ? (NetUtils.NumBitsInAByte - (numBits % NetUtils.NumBitsInAByte))
                    : leadZeroesMSB;

        if (numBitstoShiftBy == 0)
            return inputBytes;

        if (numBits < NetUtils.NumBitsInAByte) { //inputbytes.length = 1 OR Read less than a byte
            shiftedBytes[0] = (byte) ((inputBytes[0] & getLSBMask(numBits)) << numBitstoShiftBy);
        } else {
            numEndRestBits = NetUtils.NumBitsInAByte
                    - (inputBytes.length * NetUtils.NumBitsInAByte - numBits - numBitstoShiftBy); //# of bits to read from last byte
            for (i = 0; i < (size - 1); i++) {
                if ((i + 1) == (size - 1)) {
                    if (numEndRestBits > numBitstoShiftBy) {
                        shiftedBytes[i] = (byte) ((inputBytes[i] << numBitstoShiftBy) | ((inputBytes[i + 1] & getMSBMask(numBitstoShiftBy)) >> (numEndRestBits - numBitstoShiftBy)));
                        shiftedBytes[i + 1] = (byte) ((inputBytes[i + 1] & getLSBMask(numEndRestBits
                                - numBitstoShiftBy)) << numBitstoShiftBy);
                    } else
                        shiftedBytes[i] = (byte) ((inputBytes[i] << numBitstoShiftBy) | ((inputBytes[i + 1] & getMSBMask(numEndRestBits)) >> (NetUtils.NumBitsInAByte - numEndRestBits)));
                }
                shiftedBytes[i] = (byte) ((inputBytes[i] << numBitstoShiftBy) | (inputBytes[i + 1] & getMSBMask(numBitstoShiftBy)) >> (NetUtils.NumBitsInAByte - numBitstoShiftBy));
            }

        }
        return shiftedBytes;
    }

    /**
     * It aligns the first numBits bits to the right end of the byte array
     * preceding them with numBits % 8 zero bits.
     *
     * Example:
     * For inputbytes = [01110111][00010000] and numBits = 12 it returns:
     *     shiftedBytes = [00000111][01110001]
     *
     * @param byte[] inputBytes
     * @param int numBits - number of bits to be right aligned
     * @return byte[]
     */
    public static byte[] shiftBitsToLSB(byte[] inputBytes, int numBits) {
        int numBytes = inputBytes.length;
        int numBitstoShift = numBits % NetUtils.NumBitsInAByte;
        byte[] shiftedBytes = new byte[numBytes];
        int inputLsb = 0, inputMsb = 0;

        if (numBitstoShift == 0)
            return inputBytes;

        for (int i = 1; i < numBytes; i++) {
            inputLsb = inputBytes[i - 1]
                    & getLSBMask(NetUtils.NumBitsInAByte - numBitstoShift);
            inputLsb = (inputLsb < 0) ? (inputLsb + 256) : inputLsb;
            inputMsb = inputBytes[i] & getMSBMask(numBitstoShift);
            inputMsb = (inputBytes[i] < 0) ? (inputBytes[i] + 256)
                    : inputBytes[i];
            shiftedBytes[i] = (byte) ((inputLsb << numBitstoShift) | (inputMsb >> (NetUtils.NumBitsInAByte - numBitstoShift)));
        }
        inputMsb = inputBytes[0] & (getMSBMask(numBitstoShift));
        inputMsb = (inputMsb < 0) ? (inputMsb + 256) : inputMsb;
        shiftedBytes[0] = (byte) (inputMsb >> (NetUtils.NumBitsInAByte - numBitstoShift));
        return shiftedBytes;
    }

    /**
     * Insert in the data buffer at position dictated by the offset the number
     * of bits specified from the input data byte array.
     * The input byte array has the bits stored starting from the LSB
     *
     * @param byte[] data
     * @param byte[] inputdata
     * @param int startOffset
     * @param int numBits
     * @return void
     */
    public static void insertBits(byte[] data, byte[] inputdataLSB,
            int startOffset, int numBits) {
        byte[] inputdata = shiftBitsToMSB(inputdataLSB, numBits); // Align to MSB the passed byte array
        int numBytes = numBits / NetUtils.NumBitsInAByte;
        int startByteOffset = startOffset / NetUtils.NumBitsInAByte;
        int extraOffsetBits = startOffset % NetUtils.NumBitsInAByte;
        int extranumBits = numBits % NetUtils.NumBitsInAByte;
        int RestBits = numBits % NetUtils.NumBitsInAByte;
        int InputMSBbits = 0, InputLSBbits = 0;
        int i;

        if (numBits == 0)
            return;

        if (extraOffsetBits == 0) {
            if (extranumBits == 0) {
                numBytes = numBits / NetUtils.NumBitsInAByte;
                System.arraycopy(inputdata, 0, data, startByteOffset, numBytes);
            } else {
                System.arraycopy(inputdata, 0, data, startByteOffset, numBytes);
                data[startByteOffset + numBytes] = (byte) (data[startByteOffset
                        + numBytes] | (inputdata[numBytes] & getMSBMask(extranumBits)));
            }
        } else {
            for (i = 0; i < numBytes; i++) {
                if (i != 0)
                    InputLSBbits = (inputdata[i - 1] & getLSBMask(extraOffsetBits));
                InputMSBbits = (byte) (inputdata[i] & (getMSBMask(NetUtils.NumBitsInAByte
                        - extraOffsetBits)));
                InputMSBbits = (InputMSBbits >= 0) ? InputMSBbits
                        : InputMSBbits + 256;
                data[startByteOffset + i] = (byte) (data[startByteOffset + i]
                        | (InputLSBbits << (NetUtils.NumBitsInAByte - extraOffsetBits)) | (InputMSBbits >> extraOffsetBits));
                InputMSBbits = InputLSBbits = 0;
            }
            if (RestBits < (NetUtils.NumBitsInAByte - extraOffsetBits)) {
                if (numBytes != 0)
                    InputLSBbits = (inputdata[i - 1] & getLSBMask(extraOffsetBits));
                InputMSBbits = (byte) (inputdata[i] & (getMSBMask(RestBits)));
                InputMSBbits = (InputMSBbits >= 0) ? InputMSBbits
                        : InputMSBbits + 256;
                data[startByteOffset + i] = (byte) ((data[startByteOffset + i])
                        | (InputLSBbits << (NetUtils.NumBitsInAByte - extraOffsetBits)) | (InputMSBbits >> extraOffsetBits));
            } else if (RestBits == (NetUtils.NumBitsInAByte - extraOffsetBits)) {
                if (numBytes != 0)
                    InputLSBbits = (inputdata[i - 1] & getLSBMask(extraOffsetBits));
                InputMSBbits = (byte) (inputdata[i] & (getMSBMask(NetUtils.NumBitsInAByte
                        - extraOffsetBits)));
                InputMSBbits = (InputMSBbits >= 0) ? InputMSBbits
                        : InputMSBbits + 256;
                data[startByteOffset + i] = (byte) (data[startByteOffset + i]
                        | (InputLSBbits << (NetUtils.NumBitsInAByte - extraOffsetBits)) | (InputMSBbits >> extraOffsetBits));
            } else {
                if (numBytes != 0)
                    InputLSBbits = (inputdata[i - 1] & getLSBMask(extraOffsetBits));
                InputMSBbits = (byte) (inputdata[i] & (getMSBMask(NetUtils.NumBitsInAByte
                        - extraOffsetBits)));
                InputMSBbits = (InputMSBbits >= 0) ? InputMSBbits
                        : InputMSBbits + 256;
                data[startByteOffset + i] = (byte) (data[startByteOffset + i]
                        | (InputLSBbits << (NetUtils.NumBitsInAByte - extraOffsetBits)) | (InputMSBbits >> extraOffsetBits));

                InputLSBbits = (inputdata[i] & (getLSBMask(RestBits
                        - (NetUtils.NumBitsInAByte - extraOffsetBits)) << (NetUtils.NumBitsInAByte - RestBits)));
                data[startByteOffset + i + 1] = (byte) (data[startByteOffset
                        + i + 1] | (InputLSBbits << (NetUtils.NumBitsInAByte - extraOffsetBits)));
            }
        }
    }

    /**
     * Checks for overflow and underflow exceptions
     * @param data
     * @param startOffset
     * @param numBits
     * @throws Exception
     */
    public static void checkExceptions(byte[] data, int startOffset, int numBits)
            throws Exception {
        int endOffsetByte;
        int startByteOffset;
        endOffsetByte = startOffset
                / NetUtils.NumBitsInAByte
                + numBits
                / NetUtils.NumBitsInAByte
                + ((numBits % NetUtils.NumBitsInAByte != 0) ? 1 : ((startOffset
                        % NetUtils.NumBitsInAByte != 0) ? 1 : 0));
        startByteOffset = startOffset / NetUtils.NumBitsInAByte;

        if (data == null) {
            throw new Exception("data[] is null\n");
        }

        if ((startOffset < 0) || (startByteOffset >= data.length)
                || (endOffsetByte > data.length) || (numBits < 0)
                || (numBits > NetUtils.NumBitsInAByte * data.length)) {
            throw new Exception(
                    "Illegal arguement/out of bound exception - data.length = "
                            + data.length + " startOffset = " + startOffset
                            + " numBits " + numBits);
        }
    }
}
