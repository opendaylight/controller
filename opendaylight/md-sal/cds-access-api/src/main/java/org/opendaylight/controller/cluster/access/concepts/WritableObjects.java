/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

//FIXME: this really should go into yangtools/common/concepts.
public final class WritableObjects {
    private WritableObjects() {
        throw new UnsupportedOperationException();
    }

    /**
     * Write a long value into a {@link DataOutput}, compressing potential zero bytes. This method is useful for
     * serializing counters and similar, which have a wide range, but typically do not use it. The value provided is
     * treated as unsigned.
     *
     * This methods writes the number of trailing non-zero in the value. It then writes the minimum required bytes
     * to reconstruct the value by left-padding zeroes. Inverse operation is performed by {@link #readLong(DataInput)}.
     *
     * @param out Output
     * @param value long value to write
     */
    public static void writeLong(final DataOutput out, final long value) throws IOException {
        final int bytes = valueBytes(value);
        out.writeByte(bytes);
        writeValue(out, value, bytes);
    }

    public static long readLong(final DataInput in) throws IOException {
        return readValue(in, in.readByte());
    }

    private static long readValue(final DataInput in, final int flags) throws IOException {
        int bytes = flags & 0xf;
        if (bytes < 8) {
            if (bytes > 0) {
                long value = 0;
                if (bytes >= 4) {
                    bytes -= 4;
                    value = (in.readInt() & 0xFFFFFFFFL) << (bytes * Byte.SIZE);
                }
                if (bytes >= 2) {
                    bytes -= 2;
                    value |= in.readUnsignedShort() << (bytes * Byte.SIZE);
                }
                if (bytes > 0) {
                    value |= in.readUnsignedByte();
                }
                return value;
            } else {
                return 0;
            }
        } else {
            return in.readLong();
        }
    }

    private static void writeValue(final DataOutput out, final long value, final int bytes) throws IOException {
        if (bytes < 8) {
            int left = bytes;
            if (left >= 4) {
                left -= 4;
                out.writeInt((int)(value >>> (left * Byte.SIZE)));
            }
            if (left >= 2) {
                left -= 2;
                out.writeShort((int)(value >>> (left * Byte.SIZE)));
            }
            if (left > 0) {
                out.writeByte((int)(value & 0xFF));
            }
        } else {
            out.writeLong(value);
        }
    }

    private static int valueBytes(final long value) {
        // This is a binary search for the first match. Note that we need to mask bits from the most significant one
        if ((value & 0xFFFFFFFF00000000L) != 0) {
            if ((value & 0xFFFF000000000000L) != 0) {
                return (value & 0xFF00000000000000L) != 0 ? 8 : 7;
            } else {
                return (value & 0xFF0000000000L) != 0 ? 6 : 5;
            }
        } else if ((value & 0xFFFFFFFFL) != 0) {
            if ((value & 0xFFFF0000L) != 0) {
                return (value & 0xFF000000L) != 0 ? 4 : 3;
            } else {
                return (value & 0xFF00L) != 0 ? 2 : 1;
            }
        } else {
            return 0;
        }
    }
}
