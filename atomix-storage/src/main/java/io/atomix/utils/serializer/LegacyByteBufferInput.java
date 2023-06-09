/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import java.io.InputStream;
import java.nio.ByteBuffer;

/*
 * This class is used to deserialize segmented journals created before Kryo was removed from atomix-storage in
 * CONTROLLER-2072. It contains methods form Kryo to read int and String values which are compatible with format
 * of written data.
 */
final class LegacyByteBufferInput extends InputStream {
    private ByteBuffer niobuffer;
    private int position;
    private int capacity;
    private int limit;
    private char[] chars = new char[32];

    public LegacyByteBufferInput(ByteBuffer buffer) {
        setBuffer(buffer);
    }

    public void setBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null.");
        }
        niobuffer = buffer;
        position = buffer.position();
        limit = buffer.limit();
        capacity = buffer.capacity();
    }

    public String readString() {
        niobuffer.position(position);
        int available = require(1);
        position++;
        int myByte = niobuffer.get();
        if ((myByte & 0x80) == 0) {
            return readAscii(); // ASCII.
        }
        // Null, empty, or UTF8.
        int charCount = available >= 5 ? readUtf8Length(myByte) : readUtf8Length_slow(myByte);
        if (charCount == 0) {
            return null;
        }
        if (charCount == 1) {
            return "";
        }
        charCount--;
        if (chars.length < charCount) {
            chars = new char[charCount];
        }
        readUtf8(charCount);
        return new String(chars, 0, charCount);
    }

    private int readUtf8Length(int myByte) {
        int result = myByte & 0x3F; // Mask all but first 6 bits.
        if ((myByte & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            position++;
            myByte = niobuffer.get();
            result |= (myByte & 0x7F) << 6;
            if ((myByte & 0x80) != 0) {
                position++;
                myByte = niobuffer.get();
                result |= (myByte & 0x7F) << 13;
                if ((myByte & 0x80) != 0) {
                    position++;
                    myByte = niobuffer.get();
                    result |= (myByte & 0x7F) << 20;
                    if ((myByte & 0x80) != 0) {
                        position++;
                        myByte = niobuffer.get();
                        result |= (myByte & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private int readUtf8Length_slow(int myByte) {
        int result = myByte & 0x3F; // Mask all but first 6 bits.
        if ((myByte & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            require(1);
            position++;
            myByte = niobuffer.get();
            result |= (myByte & 0x7F) << 6;
            if ((myByte & 0x80) != 0) {
                require(1);
                position++;
                myByte = niobuffer.get();
                result |= (myByte & 0x7F) << 13;
                if ((myByte & 0x80) != 0) {
                    require(1);
                    position++;
                    myByte = niobuffer.get();
                    result |= (myByte & 0x7F) << 20;
                    if ((myByte & 0x80) != 0) {
                        require(1);
                        position++;
                        myByte = niobuffer.get();
                        result |= (myByte & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private void readUtf8(int charCount) {
        char[] myChars = this.chars;
        // Try to read 7 bit ASCII chars.
        int charIndex = 0;
        int count = Math.min(require(1), charCount);
        int myPosition = this.position;
        int myByte;
        while (charIndex < count) {
            myPosition++;
            myByte = niobuffer.get();
            if (myByte < 0) {
                myPosition--;
                break;
            }
            myChars[charIndex++] = (char)myByte;
        }
        this.position = myPosition;
        // If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
        if (charIndex < charCount) {
            niobuffer.position(myPosition);
            readUtf8_slow(charCount, charIndex);
        }
    }

    private void readUtf8_slow(int charCount, int charIndex) {
        char[] myChars = this.chars;
        while (charIndex < charCount) {
            if (position == limit) {
                require(1);
            }
            position++;
            int myByte = niobuffer.get() & 0xFF;
            switch (myByte >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    myChars[charIndex] = (char)myByte;
                    break;
                case 12:
                case 13:
                    if (position == limit) {
                        require(1);
                    }
                    position++;
                    myChars[charIndex] = (char)((myByte & 0x1F) << 6 | niobuffer.get() & 0x3F);
                    break;
                case 14:
                    require(2);
                    position += 2;
                    int b2 = niobuffer.get();
                    int b3 = niobuffer.get();
                    myChars[charIndex] = (char)((myByte & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F);
                    break;
                default:
                    break;
            }
            charIndex++;
        }
    }

    private String readAscii() {
        int end = position;
        int start = end - 1;
        int myLimit = this.limit;
        int myByte;
        do {
            if (end == myLimit) {
                return readAscii_slow();
            }
            end++;
            myByte = niobuffer.get();
        } while ((myByte & 0x80) == 0);
        niobuffer.put(end - 1, (byte)(niobuffer.get(end - 1) & 0x7F)); // Mask end of ascii bit.
        byte[] tmp = new byte[end - start];
        niobuffer.position(start);
        niobuffer.get(tmp);
        //String value = new String(tmp, 0, 0, end - start);
        niobuffer.put(end - 1, (byte)(niobuffer.get(end - 1) | 0x80));
        position = end;
        niobuffer.position(position);
        return new String(tmp, 0, 0, end - start);
    }

    private String readAscii_slow() {
        position--; // Re-read the first byte.
        // Copy chars currently in buffer.
        int charCount = limit - position;
        if (charCount > chars.length) {
            chars = new char[charCount * 2];
        }
        char[] myChars = this.chars;
        for (int i = position, ii = 0, n = limit; i < n; i++, ii++) {
            myChars[ii] = (char)niobuffer.get(i);
        }
        position = limit;
        // Copy additional chars one by one.
        while (true) {
            require(1);
            position++;
            int myByte = niobuffer.get();
            if (charCount == myChars.length) {
                char[] newChars = new char[charCount * 2];
                System.arraycopy(myChars, 0, newChars, 0, charCount);
                myChars = newChars;
                this.chars = newChars;
            }
            if ((myByte & 0x80) == 0x80) {
                myChars[charCount++] = (char)(myByte & 0x7F);
                break;
            }
            myChars[charCount++] = (char)myByte;
        }
        return new String(myChars, 0, charCount);
    }

    public int readVarInt(boolean optimizePositive) {
        niobuffer.position(position);
        if (require(1) < 5) {
            return readInt_slow(optimizePositive);
        }
        position++;
        int myByte = niobuffer.get();
        int result = myByte & 0x7F;
        if ((myByte & 0x80) != 0) {
            position++;
            myByte = niobuffer.get();
            result |= (myByte & 0x7F) << 7;
            if ((myByte & 0x80) != 0) {
                position++;
                myByte = niobuffer.get();
                result |= (myByte & 0x7F) << 14;
                if ((myByte & 0x80) != 0) {
                    position++;
                    myByte = niobuffer.get();
                    result |= (myByte & 0x7F) << 21;
                    if ((myByte & 0x80) != 0) {
                        position++;
                        myByte = niobuffer.get();
                        result |= (myByte & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
    }

    private int readInt_slow(boolean optimizePositive) {
        // The buffer is guaranteed to have at least 1 byte.
        position++;
        int myByte = niobuffer.get();
        int result = myByte & 0x7F;
        if ((myByte & 0x80) != 0) {
            require(1);
            position++;
            myByte = niobuffer.get();
            result |= (myByte & 0x7F) << 7;
            if ((myByte & 0x80) != 0) {
                require(1);
                position++;
                myByte = niobuffer.get();
                result |= (myByte & 0x7F) << 14;
                if ((myByte & 0x80) != 0) {
                    require(1);
                    position++;
                    myByte = niobuffer.get();
                    result |= (myByte & 0x7F) << 21;
                    if ((myByte & 0x80) != 0) {
                        require(1);
                        position++;
                        myByte = niobuffer.get();
                        result |= (myByte & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
    }

    private int require(int required) throws IllegalStateException {
        int remaining = limit - position;
        if (remaining >= required) {
            return remaining;
        }
        if (required > capacity) {
            throw new IllegalStateException("Buffer too small: capacity: " + capacity + ", required: " + required);
        }
        return -1;
    }

    @Override
    public int read() {
        int remaining = limit - position;
        if (remaining <= 0) {
            return -1;
        }
        niobuffer.position(position);
        position++;
        return niobuffer.get() & 0xFF;
    }
}
