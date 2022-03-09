/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
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
@Beta
public abstract class UnsignedLongBitmap implements Immutable {
    @VisibleForTesting
    static final class Regular extends UnsignedLongBitmap {
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
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Regular)) {
                return false;
            }
            final var other = (Regular) obj;
            return Arrays.equals(keys, other.keys) && Arrays.equals(values, other.values);
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
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Singleton)) {
                return false;
            }
            final var other = (Singleton) obj;
            return key == other.key && value == other.value;
        }
    }

    private static final @NonNull UnsignedLongBitmap EMPTY = new Regular(new long[0], new boolean[0]);

    private UnsignedLongBitmap() {
        // Hidden on purpose
    }

    public static @NonNull UnsignedLongBitmap of() {
        return EMPTY;
    }

    public static @NonNull UnsignedLongBitmap of(final long keyBits, final boolean value) {
        return new Singleton(keyBits, value);
    }

    public static @NonNull UnsignedLongBitmap copyOf(final Map<UnsignedLong, Boolean> map) {
        final int size = map.size();
        switch (size) {
            case 0:
                return of();
            case 1:
                final var entry = map.entrySet().iterator().next();
                return of(entry.getKey().longValue(), entry.getValue());
            default:
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

                return new Regular(keys, values);
        }
    }

    public abstract boolean isEmpty();

    public abstract int size();

    public final @NonNull HashMap<UnsignedLong, Boolean> mutableCopy() {
        final int size = size();
        switch (size) {
            case 0:
                return new HashMap<>();
            default:
                final var ret = Maps.<UnsignedLong, Boolean>newHashMapWithExpectedSize(size);
                putEntries(ret);
                return ret;
        }
    }

    public static @NonNull UnsignedLongBitmap readFrom(final @NonNull DataInput in, final int size) throws IOException {
        switch (size) {
            case 0:
                return of();
            case 1:
                return new Singleton(WritableObjects.readLong(in), in.readBoolean());
            default:
                final var keys = new long[size];
                final var values = new boolean[size];
                for (int i = 0; i < size; ++i) {
                    keys[i] = WritableObjects.readLong(in);
                    values[i] = in.readBoolean();
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

                return new Regular(keys, values);
        }
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
     * <p>
     * Implementations of this method return a deterministic value.
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
        // FIXME: This serialization format is what we inherited. We could do better by storing the boolean in
        //        writeLong()'s flags. On the other had, we could also be writing longs by twos, which might be
        //        benefitial.
        WritableObjects.writeLong(out, key);
        out.writeBoolean(value);
    }
}
