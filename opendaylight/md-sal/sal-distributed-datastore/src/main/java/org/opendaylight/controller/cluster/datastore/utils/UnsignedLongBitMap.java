/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

@Beta
public final class UnsignedLongBitMap implements Immutable {
    // FIXME: this should be a record once we have JDK17+
    private static final class KeyValue implements Map.Entry<UnsignedLong, Boolean>, Comparable<KeyValue>, Immutable {
        final long keyBits;
        final boolean value;

        KeyValue(final long keyBits, final boolean value) {
            this.keyBits = keyBits;
            this.value = value;
        }

        KeyValue(final Entry<UnsignedLong, Boolean> entry) {
            this(entry.getKey().longValue(), entry.getValue());
        }

        @Override
        public UnsignedLong getKey() {
            return UnsignedLong.fromLongBits(keyBits);
        }

        @Override
        public Boolean getValue() {
            return value;
        }

        @Override
        public Boolean setValue(final Boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("checkstyle:parameterName")
        public int compareTo(final KeyValue o) {
            return Long.compareUnsigned(keyBits, o.keyBits);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(keyBits) ^ Boolean.hashCode(value);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyValue)) {
                return false;
            }
            final var other = (KeyValue) obj;
            return keyBits == other.keyBits && value == other.value;
        }

        @Override
        public String toString() {
            return Long.toUnsignedString(keyBits) + "=" + value;
        }
    }

    private static final @NonNull UnsignedLongBitMap EMPTY = new UnsignedLongBitMap(ImmutableList.of());

    private final ImmutableList<KeyValue> entries;

    private UnsignedLongBitMap(final ImmutableList<KeyValue> entries) {
        this.entries = requireNonNull(entries);
    }

    public static @NonNull UnsignedLongBitMap of() {
        return EMPTY;
    }

    public static @NonNull UnsignedLongBitMap copyOf(final Map<UnsignedLong, Boolean> map) {
        final int size = map.size();
        final ImmutableList<KeyValue> entries;
        switch (size) {
            case 0:
                return of();
            case 1:
                entries = ImmutableList.of(new KeyValue(map.entrySet().iterator().next()));
                break;
            default:
                entries = map.entrySet().stream()
                    .map(KeyValue::new)
                    .sorted()
                    .collect(ImmutableList.toImmutableList());
        }

        return new UnsignedLongBitMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public @NonNull HashMap<UnsignedLong, Boolean> mutableCopy() {
        final int size = size();
        switch (size) {
            case 0:
                return new HashMap<>();
            default:
                final var ret = Maps.<UnsignedLong, Boolean>newHashMapWithExpectedSize(size);
                for (var entry : entries) {
                    ret.put(entry.getKey(), entry.getValue());
                }
                return ret;
        }
    }

    public static UnsignedLongBitMap readFrom(final DataInput in, final int size) throws IOException {
        if (size == 0) {
            return of();
        }

        final var tmp = new ArrayList<KeyValue>(size);
        for (int i = 0; i < size; ++i) {
            tmp.add(new KeyValue(WritableObjects.readLong(in), in.readBoolean()));
        }
        final var entries = ImmutableList.sortedCopyOf(tmp);

        // FIXME: validate there are no duplicates

        return new UnsignedLongBitMap(entries);
    }

    public void writeEntriesTo(final @NonNull DataOutput out, final int size) throws IOException {
        if (size != size()) {
            throw new IOException("Mismatched size: expected " + size() + ", got " + size);
        }
        for (var entry : entries) {
            WritableObjects.writeLong(out, entry.keyBits);
            out.writeBoolean(entry.value);
        }
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this
            || obj instanceof UnsignedLongBitMap && entries.equals(((UnsignedLongBitMap) obj).entries);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        final var sb = new StringBuilder().append('{');
        final var it = entries.iterator();
        for (;;) {
            sb.append(it.next());

            if (it.hasNext()) {
                sb.append(", ");
            } else {
                return sb.append('}').toString();
            }
        }
    }
}
