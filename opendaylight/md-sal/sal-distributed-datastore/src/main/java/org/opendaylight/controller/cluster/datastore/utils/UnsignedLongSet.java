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
import com.google.common.base.MoreObjects;
import com.google.common.collect.BoundType;
import com.google.common.collect.Collections2;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * A class holding an equivalent of {@code Set<UnsignedLong>}. It is geared towards efficiently tracking ranges of
 * objects, similar to what a {@link RangeSet} would do.
 *
 * <p>
 * Unlike a {@code RangeSet}, though, this class takes advantage of knowing that an unsigned long is a discrete unit
 * and can be stored in a simple {@code long}.
 *
 * @author Robert Varga
 */
abstract class UnsignedLongSet {
    @Beta
    @VisibleForTesting
    public static final class Entry implements Comparable<Entry>, Mutable {
        // Note: mutable to allow efficient merges.
        long lowerBits;
        long upperBits;

        private Entry(final long lowerBits, final long upperBits) {
            this.lowerBits = lowerBits;
            this.upperBits = upperBits;
        }

        static Entry of(final long longBits) {
            return of(longBits, longBits);
        }

        static Entry of(final long lowerBits, final long upperBits) {
            return new Entry(lowerBits, upperBits);
        }

        static Entry of(final Range<UnsignedLong> range) {
            verify(range.lowerBoundType() == BoundType.CLOSED && range.upperBoundType() == BoundType.OPEN,
                "Unexpected range %s", range);
            return of(range.lowerEndpoint().longValue(), range.upperEndpoint().longValue() - 1);
        }

        @VisibleForTesting
        public UnsignedLong lower() {
            return UnsignedLong.fromLongBits(lowerBits);
        }

        @VisibleForTesting
        public UnsignedLong upper() {
            return UnsignedLong.fromLongBits(upperBits);
        }

        boolean contains(final long longBits) {
            return Long.compareUnsigned(lowerBits, longBits) <= 0 && Long.compareUnsigned(upperBits, longBits) >= 0;
        }

        Entry copy() {
            return new Entry(lowerBits, upperBits);
        }

        // Provides compatibility with RangeSet<UnsignedLong> using [lower, upper + 1)
        Range<UnsignedLong> toUnsigned() {
            return Range.closedOpen(UnsignedLong.fromLongBits(lowerBits), UnsignedLong.fromLongBits(upperBits + 1));
        }

        // These two methods provide the same serialization format as the one we've used to serialize
        // Range<UnsignedLong>
        static @NonNull Entry readUnsigned(final DataInput in) throws IOException {
            final byte hdr = WritableObjects.readLongHeader(in);
            final long first = WritableObjects.readFirstLong(in, hdr);
            final long second = WritableObjects.readSecondLong(in, hdr) - 1;
            if (Long.compareUnsigned(first, second) > 0) {
                throw new IOException("Lower endpoint " + Long.toUnsignedString(first) + " is greater than upper "
                    + "endpoint " + Long.toUnsignedString(second));
            }

            return new Entry(first, second);
        }

        void writeUnsigned(final @NonNull DataOutput out) throws IOException {
            WritableObjects.writeLongs(out, lowerBits, upperBits + 1);
        }

        @Override
        @SuppressWarnings("checkstyle:parameterName")
        public int compareTo(final Entry o) {
            return Long.compareUnsigned(lowerBits, o.lowerBits);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(lowerBits) * 31 + Long.hashCode(upperBits);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            final var other = (Entry) obj;
            return lowerBits == other.lowerBits && upperBits == other.upperBits;
        }

        @Override
        public String toString() {
            return "[" + Long.toUnsignedString(lowerBits) + ".." + Long.toUnsignedString(upperBits) + "]";
        }
    }

    // The idea is rather simple, we track a TreeSet of range entries, ordered by their lower bound. This means that
    // for a contains() operation we just need the first headSet() entry. For insert operations we just update either
    // the lower bound or the upper bound of an existing entry. When we do, we also look at prev/next entry and if they
    // are contiguous with the updated entry, we adjust the entry once more and remove the prev/next entry.
    private final @NonNull TreeSet<Entry> ranges;

    UnsignedLongSet(final TreeSet<Entry> ranges) {
        this.ranges = requireNonNull(ranges);
    }

    final void addImpl(final long longBits) {
        final var range = Entry.of(longBits);

        final var headIt = headIter(range);
        if (headIt.hasNext()) {
            final var head = headIt.next();
            if (head.contains(longBits)) {
                return;
            }
            if (head.upperBits + 1 == longBits) {
                head.upperBits = longBits;
                final var tailSet = ranges.tailSet(range);
                if (!tailSet.isEmpty()) {
                    final var tail = tailSet.first();
                    if (tail.lowerBits - 1 == longBits) {
                        tail.lowerBits = head.lowerBits;
                        headIt.remove();
                    }
                }
                return;
            }
        }

        final var tailSet = ranges.tailSet(range);
        if (!tailSet.isEmpty()) {
            final var tail = tailSet.first();
            if (tail.lowerBits - 1 == longBits) {
                tail.lowerBits = longBits;
                return;
            }
        }

        ranges.add(range);
    }

    public final boolean contains(final long longBits) {
        final var headIt = headIter(Entry.of(longBits));
        return headIt.hasNext() && headIt.next().contains(longBits);
    }

    public final boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final int size() {
        return ranges.size();
    }

    public abstract @NonNull ImmutableUnsignedLongSet immutableCopy();

    public final @NonNull MutableUnsignedLongSet mutableCopy() {
        return new MutableUnsignedLongSet(copyRanges());
    }

    final @NonNull TreeSet<Entry> copyRanges() {
        return new TreeSet<>(Collections2.transform(ranges, Entry::copy));
    }

    public final @NonNull NavigableSet<Entry> ranges() {
        return Collections.unmodifiableNavigableSet(ranges);
    }

    final @NonNull NavigableSet<Entry> trustedRanges() {
        return ranges;
    }

    @Override
    public final int hashCode() {
        return ranges.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj == this || obj instanceof UnsignedLongSet && ranges.equals(((UnsignedLongSet) obj).ranges);
    }

    @Override
    public final String toString() {
        final var helper = MoreObjects.toStringHelper(this);

        final int size = ranges.size();
        switch (size) {
            case 0:
                break;
            case 1:
                helper.add("span", ranges.first());
                break;
            default:
                helper.add("span", Entry.of(ranges.first().lowerBits, ranges.last().upperBits));
        }

        return helper.add("size", size).toString();
    }

    private Iterator<Entry> headIter(final Entry range) {
        return ranges.headSet(range, true).descendingIterator();
    }
}
