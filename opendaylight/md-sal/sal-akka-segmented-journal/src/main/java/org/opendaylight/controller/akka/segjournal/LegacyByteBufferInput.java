package org.opendaylight.controller.akka.segjournal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LegacyByteBufferInput extends InputStream{
    protected ByteBuffer niobuffer;
    protected byte[] buffer;
    protected int position;
    protected int capacity;
    protected int limit;
    protected long total;
    protected char[] chars = new char[32];
    /* Default byte order is BIG_ENDIAN to be compatible to the base class */
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    public LegacyByteBufferInput(ByteBuffer buffer) {
        setBuffer(buffer);
    }

    public void setBuffer(ByteBuffer buffer) {
        if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
        niobuffer = buffer;
        position = buffer.position();
        limit = buffer.limit();
        capacity = buffer.capacity();
        byteOrder = buffer.order();
        total = 0;
    }

    public int readVarInt(boolean optimizePositive) {
        niobuffer.position(position);
        if (require(1) < 5) return readInt_slow(optimizePositive);
        position++;
        int b = niobuffer.get();
        int result = b & 0x7F;
        if ((b & 0x80) != 0) {
            position++;
            b = niobuffer.get();
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                position++;
                b = niobuffer.get();
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    position++;
                    b = niobuffer.get();
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        position++;
                        b = niobuffer.get();
                        result |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
    }

    final protected int require (int required) throws IllegalStateException {
        int remaining = limit - position;
        if (remaining >= required) {
            return remaining;
        }
        if (required > capacity) {
            throw new IllegalStateException("Buffer too small: capacity: " + capacity + ", required: " + required);
        }

        int count;
        // TODO: cant fill the buffer
//        if (remaining > 0) {
//            count = fill(niobuffer, limit, capacity - limit);
//            if (count == -1) throw new IllegalStateException("Buffer underflow.");
//            remaining += count;
//            if (remaining >= required) {
//                limit += count;
//                return remaining;
//            }
//        }

        // Compact. Position after compaction can be non-zero
        niobuffer.position(position);
        niobuffer.compact();
        total += position;
        position = 0;

        // TODO: cant fill the buffer
//        while (true) {
//            count = fill(niobuffer, remaining, capacity - remaining);
//            if (count == -1) {
//                if (remaining >= required) break;
//                throw new IllegalStateException("Buffer underflow.");
//            }
//            remaining += count;
//            if (remaining >= required) break; // Enough has been read.
//        }
        limit = remaining;
        niobuffer.position(0);
        return remaining;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
