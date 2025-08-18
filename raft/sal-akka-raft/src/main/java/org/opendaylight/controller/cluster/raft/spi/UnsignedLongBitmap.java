/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * A more efficient equivalent of {@code ImmutableMap<UnsignedLong, Boolean>}.
 */
public abstract sealed class UnsignedLongBitmap implements Immutable {
    // We have four bits available when using WritableObject.writeLong() to write the entry key. Historically we have
    // not taken advantage of so they were all set to zero and the actual entry value is encoded as a separate byte.
    //
    // We allocate the two least significant bits to store the entry value inline with the entry key. The bottom-most
    // bit, bit 4, is used as an indicator: when set to 1, the entry value is present in bit 5.
    //
    // flag bit 4 == 0001 0000
    private static final int HAVE_VALUE = 0x10;
    // flag bit 5 == 0010 0000
    private static final int VALUE_TRUE = 0x20;

    @VisibleForTesting
    static final class Regular extends UnsignedLongBitmap {
        private static final @NonNull UnsignedLongBitmap EMPTY = new Regular(new long[0], new boolean[0]);

        private final long[] keys;
        private final boolean[] values;

        Regular(final long[] keys, final boolean[] values) {
            this.keys = requireNonNull(keys);
            this.values = requireNonNull(values);
            verify(keys.length == values.length);
        }

        @Override
        public boolean isEmpty() {
            return keys.length == 0;
        }

        @Override
        public int size() {
            return keys.length;
        }

        @Override
        void writeEntriesTo(final DataOutput out) throws IOException {
            for (int i = 0; i < keys.length; ++i) {
                writeEntry(out, keys[i], values[i]);
            }
        }

        @Override
        StringBuilder appendEntries(final StringBuilder sb) {
            final int last = keys.length - 1;
            for (int i = 0; i < last; ++i) {
                appendEntry(sb, keys[i], values[i]).append(", ");
            }
            return appendEntry(sb, keys[last], values[last]);
        }

        @Override
        void putEntries(final HashMap<UnsignedLong, Boolean> ret) {
            for (int i = 0; i < keys.length; ++i) {
                ret.put(UnsignedLong.fromLongBits(keys[i]), values[i]);
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(keys) ^ Arrays.hashCode(values);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof Regular other && Arrays.equals(keys, other.keys)
                && Arrays.equals(values, other.values);
        }
    }

    private static final class Singleton extends UnsignedLongBitmap {
        private final long key;
        private final boolean value;

        Singleton(final long key, final boolean value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        void writeEntriesTo(final DataOutput out) throws IOException {
            writeEntry(out, key, value);
        }

        @Override
        StringBuilder appendEntries(final StringBuilder sb) {
            return sb.append(Long.toUnsignedString(key)).append('=').append(value);
        }

        @Override
        void putEntries(final HashMap<UnsignedLong, Boolean> ret) {
            ret.put(UnsignedLong.fromLongBits(key), value);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(key) ^ Boolean.hashCode(value);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof Singleton other && key == other.key && value == other.value;
        }
    }

    private UnsignedLongBitmap() {
        // Hidden on purpose
    }

    public static @NonNull UnsignedLongBitmap of() {
        return Regular.EMPTY;
    }

    public static @NonNull UnsignedLongBitmap of(final long keyBits, final boolean value) {
        return new Singleton(keyBits, value);
    }

    public static @NonNull UnsignedLongBitmap copyOf(final Map<UnsignedLong, Boolean> map) {
        final int size = map.size();
        return switch (size) {
            case 0 -> of();
            case 1 -> {
                final var entry = map.entrySet().iterator().next();
                yield of(entry.getKey().longValue(), entry.getValue());
            }
            default -> {
                final var entries = new ArrayList<>(map.entrySet());
                entries.sort(Comparator.comparing(Entry::getKey));

                final var keys = new long[size];
                final var values = new boolean[size];

                int idx = 0;
                for (var e : entries) {
                    keys[idx] = e.getKey().longValue();
                    values[idx] = e.getValue();
                    ++idx;
                }

                yield new Regular(keys, values);
            }
        };
    }

    public abstract boolean isEmpty();

    public abstract int size();

    public final @NonNull HashMap<UnsignedLong, Boolean> mutableCopy() {
        final int size = size();
        if (size == 0) {
            return new HashMap<>();
        }

        final var ret = HashMap.<UnsignedLong, Boolean>newHashMap(size);
        putEntries(ret);
        return ret;
    }

    public static @NonNull UnsignedLongBitmap readFrom(final @NonNull DataInput in, final int size) throws IOException {
        return switch (size) {
            case 0 -> of();
            case 1 -> {
                final var header = WritableObjects.readLongHeader(in);
                yield new Singleton(WritableObjects.readLongBody(in, header), readValue(in, header));
            }
            default -> {
                final var keys = new long[size];
                final var values = new boolean[size];
                for (int i = 0; i < size; ++i) {
                    final var header = WritableObjects.readLongHeader(in);
                    keys[i] = WritableObjects.readLongBody(in, header);
                    values[i] = readValue(in, header);
                }

                // There should be no duplicates and the IDs need to be increasing
                long prevKey = keys[0];
                for (int i = 1; i < size; ++i) {
                    final long key = keys[i];
                    if (Long.compareUnsigned(prevKey, key) >= 0) {
                        throw new IOException("Key " + Long.toUnsignedString(key) + " may not be used after key "
                            + Long.toUnsignedString(prevKey));
                    }
                    prevKey = key;
                }

                yield new Regular(keys, values);
            }
        };
    }

    private static boolean readValue(final @NonNull DataInput in, final byte header) throws IOException {
        return (header & HAVE_VALUE) != 0
            // - compact: boolean embedded in header flags
            ? (header & VALUE_TRUE) != 0
            // - legacy: boolean is a separate byte
            : in.readBoolean();
    }

    public void writeEntriesTo(final @NonNull DataOutput out, final int size) throws IOException {
        if (size != size()) {
            throw new IOException("Mismatched size: expected " + size() + ", got " + size);
        }
        writeEntriesTo(out);
    }

    abstract void writeEntriesTo(@NonNull DataOutput out) throws IOException;

    abstract StringBuilder appendEntries(StringBuilder sb);

    abstract void putEntries(HashMap<UnsignedLong, Boolean> ret);

    /**
     * {@inheritDoc}
     *
     * <p>Implementations of this method return a deterministic value.
     */
    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public final String toString() {
        return isEmpty() ? "{}" : appendEntries(new StringBuilder().append('{')).append('}').toString();
    }

    private static StringBuilder appendEntry(final StringBuilder sb, final long key, final boolean value) {
        return sb.append(Long.toUnsignedString(key)).append('=').append(value);
    }

    private static void writeEntry(final @NonNull DataOutput out, final long key, final boolean value)
            throws IOException {
        // New-style entry format: the value is encoded in the header
        WritableObjects.writeLong(out, key, value ? HAVE_VALUE | VALUE_TRUE : HAVE_VALUE);
    }
}
