/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link DataInput} backed by a set of Journal frames.
 */
final class GatheringDataInput implements DataInput {
    // Note: this iterator must not include Manifest Frames stemming from the fact we crossed to a different journal
    //       segment. While a segment is prefix with the manifest, this is done due to the need for each segment being
    //       its own thing, without the need to perform snapshots or other state tracking.
    private final Iterator<ByteBuffer> frameIt;

    private ByteBuffer current = null;
    private ByteBuffer tmp;

    GatheringDataInput(final Iterator<ByteBuffer> frameIt) {
        this.frameIt = requireNonNull(frameIt);
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        if (b.length != 0) {
            readByteRange(b, 0, b.length);
        }
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len != 0) {
            readByteRange(b, off, len);
        }
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        // FIXME: this does not include CRC32 calculation, which we should be doing as a matter of course for each
        //        frame -- and then when reach its end should compare the CRC and raise an error if it fails.
        int skipped = 0;
        for (int toSkip = n; toSkip > 0;) {
            final var local = currentOrNull();
            if (local == null) {
                break;
            }

            final var remaining = local.remaining();
            if (remaining > toSkip) {
                // Partial move within this frame
                local.position(local.position() + toSkip);
                skipped += toSkip;
                break;
            }

            // Full exhaustion of this frame
            toSkip -= remaining;
            skipped += remaining;
        }
        return skipped;
    }

    @Override
    public boolean readBoolean() throws IOException {
        final var b = readByte();
        return switch (b) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IOException("Unexpected boolean value " + b);
        };
    }

    @Override
    public byte readByte() throws IOException {
        try {
            return currentOrThrow().get();
        } catch (BufferUnderflowException e) {
            throw toEOF(e);
        }
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    @Override
    public short readShort() throws IOException {
        return ensureRemaining(Short.BYTES).getShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(readShort());
    }

    @Override
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    public int readInt() throws IOException {
        return ensureRemaining(Integer.BYTES).getInt();
    }

    @Override
    public long readLong() throws IOException {
        return ensureRemaining(Long.BYTES).getLong();
    }

    @Override
    public float readFloat() throws IOException {
        return ensureRemaining(Float.BYTES).getFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return ensureRemaining(Double.BYTES).getDouble();
    }

    @Override
    public String readLine() throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    private ByteBuffer currentOrNull() {
        var local = current;
        if (local == null) {
            if (frameIt.hasNext()) {
                current = local = frameIt.next();
            }
        }
        return local;
    }

    private ByteBuffer currentOrThrow() throws EOFException {
        var local = current;
        if (local == null) {
            try {
                current = local = frameIt.next();
            } catch (NoSuchElementException e) {
                throw toEOF(e);
            }
        }
        return local;
    }

    // Returns a ByteBuffer containing at least count bytes, assuming the caller will consume them.
    // IFF a buffer can satisfy the request, we just return that buffer
    // IFF the bytes are carried in multiple frames, gather exactly count bytes into temporary buffer (reused)
    // count is expected to be Long.BYTES or less
    private ByteBuffer ensureRemaining(final int count) throws IOException {
        final var local = currentOrThrow();
        final var remaining = local.remaining();
        if (remaining > count) {
            // Superset, with at least one additional byte -- just keep the current frame
            return local;
        }
        if (remaining == count) {
            // Exact size match: drop reference to the current frame and return it -- once the user has read the bytes
            // it will become empty and thus is no longer relevant to us.
            current = null;
            return local;
        }

        // Deal with conjuring a buffer with exactly count bytes.
        return gatherBytes(current, remaining, count);
    }

    // The ugly part of ensureRemaining(), dealing with shifting just enough bytes into a temporary array. With our
    // sizing (see
    private ByteBuffer gatherBytes(final ByteBuffer start, final int startLen, final int count) throws IOException {
        var toBuffer = tmp;
        if (toBuffer == null) {
            tmp = toBuffer = ByteBuffer.allocate(Long.BYTES);
        }

        final var array = toBuffer.array();
        start.get(array, 0, startLen);
        current = null;

        var off = startLen;
        var toRead = count - startLen;
        while (toRead > 0) {
            final var local = currentOrThrow();
            final var remaining = local.remaining();
            if (remaining > toRead) {
                local.get(array, off, toRead);
                break;
            }

            local.get(array, off, remaining);
            current = null;
            off += remaining;
            toRead -= remaining;
        }

        toBuffer.limit(count);
        toBuffer.position(0);
        return toBuffer;
    }

    private void readByteRange(final byte[] bytes, final int off, final int len) throws IOException {
        int start = off;
        int toRead = len;
        do {
            final var local = currentOrThrow();
            final var remaining = local.remaining();
            if (remaining > toRead) {
                local.get(bytes, start, toRead);
                return;
            }

            local.get(bytes, start, remaining);
            current = null;
            toRead -= remaining;
            start += remaining;
        } while (toRead > 0);
    }

    private static EOFException toEOF(final Exception cause) {
        final var ret = new EOFException(cause.getMessage());
        ret.initCause(cause);
        return ret;
    }
}
