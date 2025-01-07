/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSortedSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.slf4j.LoggerFactory;

@Beta
public final class ImmutableUnsignedLongSet extends UnsignedLongSet implements Immutable, WritableObject {
    // TreeSet has a large per-entry overhead (40-64 bytes as of OpenJDK 21), so we prefer allocating
    // an ImmutableSortedSet, which is backed by an array of up to this many elements. For larger sizes we opt to pay
    // the TreeMap overhead so as not to create huge objects on the heap.
    private static final int DEFAULT_MAX_ARRAY = 4096;
    private static final String PROP_MAX_ARRAY =
        "org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet.max-array";
    private static final int MAX_ARRAY;

    static {
        final var logger = LoggerFactory.getLogger(ImmutableUnsignedLongSet.class);
        final int value = Integer.getInteger(PROP_MAX_ARRAY, DEFAULT_MAX_ARRAY);
        if (value < 0) {
            logger.warn("Ignoring invalid {} value {}", PROP_MAX_ARRAY, value);
            MAX_ARRAY = DEFAULT_MAX_ARRAY;
        } else {
            MAX_ARRAY = value;
        }
        logger.debug("Using ImmutableSortedSet for up to {} elements", MAX_ARRAY);
    }

    private static final @NonNull ImmutableUnsignedLongSet EMPTY =
        new ImmutableUnsignedLongSet(ImmutableSortedSet.of());

    private ImmutableUnsignedLongSet(final NavigableSet<Entry> ranges) {
        super(ranges);
    }

    static @NonNull ImmutableUnsignedLongSet copyOf(final MutableUnsignedLongSet mutable) {
        final var size = mutable.rangeSize();
        if (size == 0) {
            return of();
        }
        return new ImmutableUnsignedLongSet(size <= MAX_ARRAY ? ImmutableSortedSet.copyOfSorted(mutable.trustedRanges())
            : new TreeSet<>(mutable.trustedRanges()));
    }

    public static @NonNull ImmutableUnsignedLongSet of() {
        return EMPTY;
    }

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return this;
    }

    public static @NonNull ImmutableUnsignedLongSet readFrom(final DataInput in) throws IOException {
        return readFrom(in, in.readInt());
    }

    public static @NonNull ImmutableUnsignedLongSet readFrom(final DataInput in, final int size) throws IOException {
        if (size == 0) {
            return EMPTY;
        }

        final NavigableSet<Entry> ranges;
        if (size <= MAX_ARRAY) {
            final var entries = new Entry[size];
            for (int i = 0; i < size; ++i) {
                entries[i] = Entry.readUnsigned(in);
            }
            ranges = ImmutableSortedSet.copyOf(entries);
        } else {
            ranges = new TreeSet<>();
            for (int i = 0; i < size; ++i) {
                ranges.add(Entry.readUnsigned(in));
            }
        }
        return new ImmutableUnsignedLongSet(ranges);
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeInt(rangeSize());
        writeRanges(out);
    }

    public void writeRangesTo(final @NonNull DataOutput out, final int size) throws IOException {
        final int rangeSize = rangeSize();
        if (size != rangeSize) {
            throw new IOException("Mismatched size: expected " + rangeSize + ", got " + size);
        }
        writeRanges(out);
    }

    private void writeRanges(final @NonNull DataOutput out) throws IOException {
        for (var range : trustedRanges()) {
            range.writeUnsigned(out);
        }
    }
}
