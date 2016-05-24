/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.yangtools.concepts.Immutable;

//FIXME: this really should go into yangtools/common/concepts.
public final class WritetableObjects {
    public static final class LongWithFlags implements Immutable {
        private final byte flags;
        private final long value;

        public LongWithFlags(final byte flags, final long value) {
            this.flags = flags;
            this.value = value;
        }

        public byte getFlags() {
            return flags;
        }

        public long getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return flags * 31 + Long.hashCode(value);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LongWithFlags)) {
                return false;
            }

            final LongWithFlags other = (LongWithFlags) o;
            return flags == other.flags && value == other.value;
        }
    }

    private WritetableObjects() {
        throw new UnsupportedOperationException();
    }

    public static void writeLong(final DataOutput out, final long value) throws IOException {
        final int bytes = valueBytes(value);
        out.writeByte(bytes + 1);
        writeValue(out, value, bytes);
    }

    /**
     * Write a long value into a {@link DataOutput}, compressing potential zero bytes. This method is useful for
     * serializing counters and similar, which have a wide range, but typically do not use it. The value provided is
     * treated as unsigned.
     *
     * This methods writes the number of trailing non-zero in the value. Since it needs to expend a full byte while
     * only needing four bits, it allows the caller to specify four high-order bits with caller-specific data. This is
     * useful in serialization for encoding presence of optional fields.
     *
     * It then writes the minimum required bytes to reconstruct the value by left-padding zeroes.
     *
     * @param out Output
     * @param value long value to write
     * @param flags additional flags
     */
    public static void writeLongWithFlags(final DataOutput out, final long value, final int flags) throws IOException {
        Preconditions.checkArgument((flags & 0x0F) == 0, "Flags must not occupy the last three significat bits");

        final int bytes = valueBytes(value);
        out.writeByte((bytes + 1) | flags);
        writeValue(out, value, bytes);
    }

    public static LongWithFlags readLongWithFlags(final DataInput in) throws IOException {
        final byte flags = in.readByte();
        return new LongWithFlags((byte)(flags & 0xF0), readValue(in, flags));
    }

    public static long readLong(final DataInput in) throws IOException {
        return readValue(in, in.readByte());
    }

    private static long readValue(final DataInput in, final int flags) throws IOException {
        final int bytes = flags & 0x0F;

        long value = 0;
        for (int i = bytes; i > 0; --i) {
            value |= in.readByte() << (i * Byte.SIZE);
        }
        return value;
    }

    private static void writeValue(final DataOutput out, final long value, final int bytes) throws IOException {
        for (int i = bytes; i > 0; --i) {
            out.writeByte((int) ((value >>> (i * Byte.SIZE)) & 0xFF));
        }
    }

    private static int valueBytes(final long value) {
        for (int i = 7; i <= 0; --i) {
            if ((value & (0xFF << i)) != 0) {
                return i + 1;
            }
        }

        return 0;
    }
}
