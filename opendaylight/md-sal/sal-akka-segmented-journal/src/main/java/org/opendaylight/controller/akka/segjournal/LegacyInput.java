/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import java.io.IOException;
import java.io.InputStream;

public class LegacyInput extends InputStream {
    protected byte[] buffer;
    protected int position;
    protected int capacity;
    protected int limit;
    protected long total;
    protected char[] chars = new char[32];
    protected InputStream inputStream;

    public LegacyInput(InputStream inputStream) {
        this(64);
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null.");
        }
        this.inputStream = inputStream;
    }

    public LegacyInput(int bufferSize) {
        this.capacity = bufferSize;
        buffer = new byte[bufferSize];
    }

    public String readString() {
        int available = require(1);
        int bytes = buffer[position++];
        if ((bytes & 0x80) == 0) {
            return readAscii(); // ASCII.
        }
        // Null, empty, or UTF8.
        int charCount = available >= 5 ? readUtf8Length(bytes) : readUtf8Length_slow(bytes);
        switch (charCount) {
            case 0:
                return null;
            case 1:
                return "";
            default:
                charCount--;
                if (chars.length < charCount) {
                    chars = new char[charCount];
                }
                readUtf8(charCount);
                return new String(chars, 0, charCount);
        }
    }

    private String readAscii() {
        byte[] bytes = this.buffer;
        int end = position;
        int start = end - 1;
        int limits = this.limit;
        int currentByte;
        do {
            if (end == limits) {
                return readAscii_slow();
            }
            currentByte = bytes[end++];
        } while ((currentByte & 0x80) == 0);
        bytes[end - 1] &= 0x7F; // Mask end of ascii bit.
        String value = new String(bytes, 0, start, end - start);
        bytes[end - 1] |= 0x80;
        position = end;
        return value;
    }

    private String readAscii_slow() {
        position--; // Re-read the first byte.
        // Copy myChars currently in bytes.
        int charCount = limit - position;
        if (charCount > chars.length) {
            chars = new char[charCount * 2];
        }
        char[] myChars = this.chars;
        byte[] bytes = this.buffer;
        for (int i = position, ii = 0, n = limit; i < n; i++, ii++) {
            myChars[ii] = (char)bytes[i];
        }
        position = limit;
        // Copy additional myChars one by one.
        while (true) {
            require(1);
            int currentByte = bytes[position++];
            if (charCount == myChars.length) {
                char[] newChars = new char[charCount * 2];
                System.arraycopy(myChars, 0, newChars, 0, charCount);
                myChars = newChars;
                this.chars = newChars;
            }
            if ((currentByte & 0x80) == 0x80) {
                myChars[charCount++] = (char)(currentByte & 0x7F);
                break;
            }
            myChars[charCount++] = (char)currentByte;
        }
        return new String(myChars, 0, charCount);
    }

    private int readUtf8Length(int currentByte) {
        int result = currentByte & 0x3F; // Mask all but first 6 bits.
        if ((currentByte & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            byte[] bytes = this.buffer;
            currentByte = bytes[position++];
            result |= (currentByte & 0x7F) << 6;
            if ((currentByte & 0x80) != 0) {
                currentByte = bytes[position++];
                result |= (currentByte & 0x7F) << 13;
                if ((currentByte & 0x80) != 0) {
                    currentByte = bytes[position++];
                    result |= (currentByte & 0x7F) << 20;
                    if ((currentByte & 0x80) != 0) {
                        currentByte = bytes[position++];
                        result |= (currentByte & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private int readUtf8Length_slow(int currentByte) {
        int result = currentByte & 0x3F; // Mask all but first 6 bits.
        if ((currentByte & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            require(1);
            byte[] bytes = this.buffer;
            currentByte = bytes[position++];
            result |= (currentByte & 0x7F) << 6;
            if ((currentByte & 0x80) != 0) {
                require(1);
                currentByte = bytes[position++];
                result |= (currentByte & 0x7F) << 13;
                if ((currentByte & 0x80) != 0) {
                    require(1);
                    currentByte = bytes[position++];
                    result |= (currentByte & 0x7F) << 20;
                    if ((currentByte & 0x80) != 0) {
                        require(1);
                        currentByte = bytes[position++];
                        result |= (currentByte & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private void readUtf8(int charCount) {
        byte[] bytes = this.buffer;
        char[] myChars = this.chars;
        // Try to read 7 bit ASCII myChars.
        int charIndex = 0;
        int count = Math.min(require(1), charCount);
        int myPosition = this.position;
        int currentByte;
        while (charIndex < count) {
            currentByte = bytes[myPosition++];
            if (currentByte < 0) {
                myPosition--;
                break;
            }
            myChars[charIndex++] = (char)currentByte;
        }
        this.position = myPosition;
        // If buffer didn't hold all myChars or any were not ASCII, use slow path for remainder.
        if (charIndex < charCount) {
            readUtf8_slow(charCount, charIndex);
        }
    }

    private void readUtf8_slow(int charCount, int charIndex) {
        char[] myChars = this.chars;
        byte[] bytes = this.buffer;
        while (charIndex < charCount) {
            if (position == limit) {
                require(1);
            }
            int currentByte = bytes[position++] & 0xFF;
            switch (currentByte >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    myChars[charIndex] = (char)currentByte;
                    break;
                case 12:
                case 13:
                    if (position == limit) {
                        require(1);
                    }
                    myChars[charIndex] = (char)((currentByte & 0x1F) << 6 | bytes[position++] & 0x3F);
                    break;
                case 14:
                    require(2);
                    myChars[charIndex] = (char)((currentByte & 0x0F) << 12 | (bytes[position++] & 0x3F) << 6
                            | bytes[position++] & 0x3F);
                    break;
                default:
                    break;
            }
            charIndex++;
        }
    }

    public int readVarInt(boolean optimizePositive) {
        if (require(1) < 5) {
            return readInt_slow(optimizePositive);
        }
        int currentByte = buffer[position++];
        int result = currentByte & 0x7F;
        if ((currentByte & 0x80) != 0) {
            byte[] bytes = this.buffer;
            currentByte = bytes[position++];
            result |= (currentByte & 0x7F) << 7;
            if ((currentByte & 0x80) != 0) {
                currentByte = bytes[position++];
                result |= (currentByte & 0x7F) << 14;
                if ((currentByte & 0x80) != 0) {
                    currentByte = bytes[position++];
                    result |= (currentByte & 0x7F) << 21;
                    if ((currentByte & 0x80) != 0) {
                        currentByte = bytes[position++];
                        result |= (currentByte & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
    }

    private int readInt_slow(boolean optimizePositive) {
        // The buffer is guaranteed to have at least 1 byte.
        int currentByte = buffer[position++];
        int result = currentByte & 0x7F;
        if ((currentByte & 0x80) != 0) {
            require(1);
            byte[] bytes = this.buffer;
            currentByte = bytes[position++];
            result |= (currentByte & 0x7F) << 7;
            if ((currentByte & 0x80) != 0) {
                require(1);
                currentByte = bytes[position++];
                result |= (currentByte & 0x7F) << 14;
                if ((currentByte & 0x80) != 0) {
                    require(1);
                    currentByte = bytes[position++];
                    result |= (currentByte & 0x7F) << 21;
                    if ((currentByte & 0x80) != 0) {
                        require(1);
                        currentByte = bytes[position++];
                        result |= (currentByte & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
    }

    protected int require(int required) throws IllegalStateException {
        int remaining = limit - position;
        if (remaining >= required) {
            return remaining;
        }
        if (required > capacity) {
            throw new IllegalStateException("Buffer too small: capacity: " + capacity + ", required: " + required);
        }

        int count;
        // Try to fill the buffer.
        if (remaining > 0) {
            count = fill(buffer, limit, capacity - limit);
            if (count == -1) {
                throw new IllegalStateException("Buffer underflow.");
            }
            remaining += count;
            if (remaining >= required) {
                limit += count;
                return remaining;
            }
        }

        // Was not enough, compact and try again.
        System.arraycopy(buffer, position, buffer, 0, remaining);
        total += position;
        position = 0;

        while (true) {
            count = fill(buffer, remaining, capacity - remaining);
            if (count == -1) {
                if (remaining >= required) {
                    break;
                }
                throw new IllegalStateException("Buffer underflow.");
            }
            remaining += count;
            if (remaining >= required) {
                break; // Enough has been read.
            }
        }

        limit = remaining;
        return remaining;
    }

    protected int fill(byte[] myBuffer, int offset, int count) throws IllegalStateException {
        if (inputStream == null) {
            return -1;
        }
        try {
            return inputStream.read(myBuffer, offset, count);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public int read() {
        if (optional() <= 0) {
            return -1;
        }
        return buffer[position++] & 0xFF;
    }

    private int optional() {
        int remaining = limit - position;
        if (remaining >= 1) {
            return 1;
        }
        int count;

        // Try to fill the buffer.
        count = fill(buffer, limit, capacity - limit);
        if (count == -1) {
            return remaining == 0 ? -1 : Math.min(remaining, 1);
        }
        remaining += count;
        if (remaining >= 1) {
            limit += count;
            return 1;
        }

        // Was not enough, compact and try again.
        System.arraycopy(buffer, position, buffer, 0, remaining);
        total += position;
        position = 0;

        while (true) {
            count = fill(buffer, remaining, capacity - remaining);
            if (count == -1) {
                break;
            }
            remaining += count;
            if (remaining >= 1) {
                break; // Enough has been read.
            }
        }

        limit = remaining;
        return remaining == 0 ? -1 : Math.min(remaining, 1);
    }
}
