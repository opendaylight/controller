/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MyOutput {
    protected int position;
    protected int capacity;
    protected byte[] buffer;

    public MyOutput(int bufferSize) {
        this.capacity = bufferSize;
        buffer = new byte[bufferSize];
    }

    @SuppressFBWarnings("ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT")
    public int writeVarInt(int value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = (value << 1) ^ (value >> 31);
        }
        if (value >>> 7 == 0) {
            buffer[position++] = (byte)value;
            return 1;
        }
        if (value >>> 14 == 0) {
            buffer[position++] = (byte)((value & 0x7F) | 0x80);
            buffer[position++] = (byte)(value >>> 7);
            return 2;
        }
        if (value >>> 21 == 0) {
            buffer[position++] = (byte)((value & 0x7F) | 0x80);
            buffer[position++] = (byte)(value >>> 7 | 0x80);
            buffer[position++] = (byte)(value >>> 14);
            return 3;
        }
        if (value >>> 28 == 0) {
            buffer[position++] = (byte)((value & 0x7F) | 0x80);
            buffer[position++] = (byte)(value >>> 7 | 0x80);
            buffer[position++] = (byte)(value >>> 14 | 0x80);
            buffer[position++] = (byte)(value >>> 21);
            return 4;
        }
        buffer[position++] = (byte)((value & 0x7F) | 0x80);
        buffer[position++] = (byte)(value >>> 7 | 0x80);
        buffer[position++] = (byte)(value >>> 14 | 0x80);
        buffer[position++] = (byte)(value >>> 21 | 0x80);
        buffer[position++] = (byte)(value >>> 28);
        return 5;
    }

    public void writeString(String value) {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }
        int charCount = value.length();
        if (charCount == 0) {
            writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
            return;
        }
        // Detect ASCII.
        boolean ascii = false;
        if (charCount > 1 && charCount < 64) {
            ascii = true;
            for (int i = 0; i < charCount; i++) {
                int charAt = value.charAt(i);
                if (charAt > 127) {
                    ascii = false;
                    break;
                }
            }
        }
        if (ascii) {
            if (capacity - position < charCount) {
                writeAscii_slow(value, charCount);
            }
            else {
                value.getBytes(0, charCount, buffer, position);
                position += charCount;
            }
            buffer[position - 1] |= 0x80;
        } else {
            writeUtf8Length(charCount + 1);
            int charIndex = 0;
            if (capacity - position >= charCount) {
                // Try to write 8 bit chars.
                byte[] bytes = this.buffer;
                int positions = this.position;
                for (; charIndex < charCount; charIndex++) {
                    int charAt = value.charAt(charIndex);
                    if (charAt > 127) {
                        break;
                    }
                    bytes[positions++] = (byte)charAt;
                }
                this.position = positions;
            }
            if (charIndex < charCount) {
                writeString_slow(value, charCount, charIndex);
            }
        }
    }

    public void writeByte(int value) {
        buffer[position++] = (byte)value;
    }

    private void writeAscii_slow(String value, int charCount) {
        int charIndex = 0;
        byte[] bytes = this.buffer;
        int charsToWrite = Math.min(charCount, capacity - position);
        while (charIndex < charCount) {
            value.getBytes(charIndex, charIndex + charsToWrite, bytes, position);
            charIndex += charsToWrite;
            position += charsToWrite;
            charsToWrite = Math.min(charCount - charIndex, capacity);
            bytes = this.buffer;
        }
    }

    @SuppressFBWarnings("ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT")
    private void writeUtf8Length(int value) {
        if (value >>> 6 == 0) {
            buffer[position++] = (byte)(value | 0x80); // Set bit 8.
        } else if (value >>> 13 == 0) {
            byte[] bytes = this.buffer;
            bytes[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
            bytes[position++] = (byte)(value >>> 6);
        } else if (value >>> 20 == 0) {
            byte[] bytes = this.buffer;
            bytes[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
            bytes[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
            bytes[position++] = (byte)(value >>> 13);
        } else if (value >>> 27 == 0) {
            byte[] bytes = this.buffer;
            bytes[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
            bytes[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
            bytes[position++] = (byte)((value >>> 13) | 0x80); // Set bit 8.
            bytes[position++] = (byte)(value >>> 20);
        } else {
            byte[] bytes = this.buffer;
            bytes[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
            bytes[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
            bytes[position++] = (byte)((value >>> 13) | 0x80); // Set bit 8.
            bytes[position++] = (byte)((value >>> 20) | 0x80); // Set bit 8.
            bytes[position++] = (byte)(value >>> 27);
        }
    }

    private void writeString_slow(CharSequence value, int charCount, int charIndex) {
        for (; charIndex < charCount; charIndex++) {
            int charAt = value.charAt(charIndex);
            if (charAt <= 0x007F) {
                buffer[position++] = (byte)charAt;
            } else if (charAt > 0x07FF) {
                buffer[position++] = (byte)(0xE0 | charAt >> 12 & 0x0F);
                buffer[position++] = (byte)(0x80 | charAt >> 6 & 0x3F);
                buffer[position++] = (byte)(0x80 | charAt & 0x3F);
            } else {
                buffer[position++] = (byte)(0xC0 | charAt >> 6 & 0x1F);
                buffer[position++] = (byte)(0x80 | charAt & 0x3F);
            }
        }
    }

    public byte[] toBytes() {
        byte[] newBuffer = new byte[position];
        System.arraycopy(buffer, 0, newBuffer, 0, position);
        return newBuffer;
    }
}
