/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.collect.ImmutableSortedSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * An immutable {@link UnsignedLongSet}.
 */
public final class ImmutableUnsignedLongSet extends UnsignedLongSet implements Immutable, WritableObject {
    // Do not all
    private static final int ARRAY_MAX_ELEMENTS = 4096;

    private static final @NonNull ImmutableUnsignedLongSet EMPTY =
        new ImmutableUnsignedLongSet(ImmutableSortedSet.of());

    private ImmutableUnsignedLongSet(final NavigableSet<EntryImpl> ranges) {
        super(ranges);
    }

    static @NonNull ImmutableUnsignedLongSet copyOf(final MutableUnsignedLongSet mutable) {
        if (mutable.isEmpty()) {
            return of();
        }
        if (mutable.rangeSize() <= ARRAY_MAX_ELEMENTS) {
            return new ImmutableUnsignedLongSet(ImmutableSortedSet.copyOfSorted(mutable.trustedRanges()));
        }
        return new ImmutableUnsignedLongSet(new TreeSet<>(mutable.trustedRanges()));
    }

    /**
     * Return an empty {@link ImmutableUnsignedLongSet}.
     *
     * @return an empty {@link ImmutableUnsignedLongSet}
     */
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
        return new ImmutableUnsignedLongSet(size <= ARRAY_MAX_ELEMENTS ? readArrayRanges(in, size)
            : readTreeRanges(in, size));
    }

    private static ImmutableSortedSet<EntryImpl> readArrayRanges(final DataInput in, final int size)
            throws IOException {
        final var ranges = new EntryImpl[size];
        for (int i = 0; i < size; ++i) {
            ranges[i] = readEntry(in);
        }
        return ImmutableSortedSet.copyOf(ranges);
    }

    private static TreeSet<EntryImpl> readTreeRanges(final DataInput in, final int size) throws IOException {
        final var ranges = new TreeSet<EntryImpl>();
        for (int i = 0; i < size; ++i) {
            ranges.add(readEntry(in));
        }
        return ranges;
    }

    // These two methods provide the same serialization format as the one we've used to serialize Range<UnsignedLong>
    private static EntryImpl readEntry(final DataInput in) throws IOException {
        final byte hdr = WritableObjects.readLongHeader(in);
        final long first = WritableObjects.readFirstLong(in, hdr);
        final long second = WritableObjects.readSecondLong(in, hdr) - 1;

        final int cmp = Long.compareUnsigned(first, second);
        if (cmp > 0) {
            throw new IOException("Lower endpoint " + Long.toUnsignedString(first) + " is greater than upper "
                + "endpoint " + Long.toUnsignedString(second));
        }
        return cmp == 0 ? new Entry1(first) : new EntryN(first, second);
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
            WritableObjects.writeLongs(out, range.lowerBits(), range.upperBits() + 1);
        }
    }
}
