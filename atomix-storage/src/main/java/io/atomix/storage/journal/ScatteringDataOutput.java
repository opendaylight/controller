/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * A {@link DataOutput} which transparently allocates frames in the Journal.
 */
final class ScatteringDataOutput implements DataOutput {
    // FIXME: frame allocations are on a 32-byte aligned offset (perhaps smaller? how much book keeping do we have?)
    private static final class OutputFrame {
        // FIXME: add state:
        //        - entry ID
        //        - frame counter
        // FIXME: handle to JournalWriter (and its innards?)
        private final long frameAvail = 0;

        void writeByte(final int b) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeShort(final int s) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeChar(final int c) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeInt(final int i) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeLong(final long l) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeFloat(final float f) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void writeDouble(final double d) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        int writeBytes(final byte[] bytes, final int off, final int len) throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void nextFrame() throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        void close() throws IOException {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }
    }

    // Default maximum segment size, 256KiB
    private static final int MAX_FRAME_SIZE = 1 << 18;

    // FIXME: add state:
    //        - entry ID
    //        - frame counter

    private final OutputFrame frame;

    ScatteringDataOutput(final OutputFrame frame) {
        this.frame = requireNonNull(frame);
    }

    @Override
    public void write(final int b) throws IOException {
        writeByte(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        if (b.length != 0) {
            writeByteRange(b, 0, b.length);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len != 0) {
            writeByteRange(b, off, len);
        }
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        ensureAvail(Byte.BYTES).writeByte(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        ensureAvail(Short.BYTES).writeShort(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        ensureAvail(Character.BYTES).writeChar(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        ensureAvail(Integer.BYTES).writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        ensureAvail(Long.BYTES).writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        ensureAvail(Float.BYTES).writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        ensureAvail(Double.BYTES).writeDouble(v);
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        // FIXME: move ensureAvail() out of hot loop
        for (int i = 0, len = s.length(); i < len ; i++) {
            ensureAvail(Byte.BYTES).writeByte(s.charAt(i));
        }
    }

    @Override
    public void writeChars(final String s) throws IOException {
        // FIXME: move ensureAvail() out of hot loop
        for (int i = 0, len = s.length(); i < len ; i++) {
            ensureAvail(Character.BYTES).writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private OutputFrame ensureAvail(final long requiredBytes) throws IOException {
        final var local = frame;
        if (local.frameAvail < requiredBytes) {
            local.nextFrame();
        }
        return local;
    }

    // Called to write at least one byte
    private void writeByteRange(final byte[] bytes, final int off, final int len) throws IOException {
        final var local = frame;
        int from = off;
        int remaining = len;
        while (true) {
            final int written = local.writeBytes(bytes, from, remaining);
            remaining -= written;
            if (remaining == 0) {
                // all done
                return;
            }

            from += written;
            local.nextFrame();
        }
    }
}
