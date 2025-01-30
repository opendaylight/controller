/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * A class holding an equivalent of {@code Set<UnsignedLong>}. It is geared towards efficiently tracking ranges of
 * objects, similar to what a {@code RangeSet<UnsignedLong>} would do.
 *
 * <p>Unlike a {@code RangeSet}, though, this class takes advantage of knowing that an unsigned long is a discrete unit
 * and can be stored in a simple {@code long}.
 */
public abstract sealed class UnsignedLongSet permits ImmutableUnsignedLongSet, MutableUnsignedLongSet {
    /**
     * A single entry tracked in this set. It represents all discrete values in the range
     * {@code [lowerBits(), upperBits()]}.
     *
     * <p>The equivalent construct in {@code RangeSet<UnsignedLong>} terms is
     * {@code Range.closedOpen(UnsignedLong.fromLongBits(lowerBits), UnsignedLong.fromLongBits(upperBits + 1))}. We are
     * saving two objects with this specialization.
     */
    @NonNullByDefault
    public sealed interface Entry extends Comparable<Entry>, Immutable permits EntryImpl {

        long lowerBits();

        long upperBits();
    }

    /**
     * Internal implementation of {@link Entry}. Separated out to hide the default constructor, which does not perform
     * lower/upper sanity check.
     */
    // TODO: would it make sense to separate out
    record EntryImpl(long lowerBits, long upperBits) implements Entry {
        EntryImpl(final long longBits) {
            this(longBits, longBits);

        }

        @NonNull EntryImpl withLowerBits(final long newLowerBits) {
            return new EntryImpl(newLowerBits, upperBits);
        }

        @NonNull EntryImpl withUpperBits(final long newUpperBits) {
            return new EntryImpl(lowerBits, newUpperBits);
        }

        // These two methods provide the same serialization format as the one we've used to serialize
        // Range<UnsignedLong>
        static @NonNull EntryImpl readUnsigned(final DataInput in) throws IOException {
            final byte hdr = WritableObjects.readLongHeader(in);
            final long first = WritableObjects.readFirstLong(in, hdr);
            final long second = WritableObjects.readSecondLong(in, hdr) - 1;
            if (Long.compareUnsigned(first, second) > 0) {
                throw new IOException("Lower endpoint " + Long.toUnsignedString(first) + " is greater than upper "
                    + "endpoint " + Long.toUnsignedString(second));
            }

            return new EntryImpl(first, second);
        }

        void writeUnsigned(final @NonNull DataOutput out) throws IOException {
            WritableObjects.writeLongs(out, lowerBits, upperBits + 1);
        }

        @Override
        @SuppressWarnings("checkstyle:parameterName")
        public int compareTo(final Entry o) {
            return Long.compareUnsigned(lowerBits, o.lowerBits());
        }

        @Override
        public int hashCode() {
            return Long.hashCode(lowerBits) * 31 + Long.hashCode(upperBits);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof Entry other && lowerBits == other.lowerBits()
                && upperBits == other.upperBits();
        }

        @Override
        public String toString() {
            return "[" + Long.toUnsignedString(lowerBits) + ".." + Long.toUnsignedString(upperBits) + "]";
        }
    }

    // The idea is rather simple, we track a NavigableSet of range entries, ordered by their lower bound. This means
    // that for a contains() operation we just need the first headSet() entry. For insert operations we just update
    // either the lower bound or the upper bound of an existing entry. When we do, we also look at prev/next entry and
    // if they are contiguous with the updated entry, we adjust the entry once more and remove the prev/next entry.
    private final @NonNull NavigableSet<EntryImpl> ranges;

    UnsignedLongSet(final NavigableSet<EntryImpl> ranges) {
        this.ranges = requireNonNull(ranges);
    }

    /**
     * Check whether this set contains a value.
     *
     * @param longBits long value, interpreted as unsigned
     * @return {@code true} if this set contains the value
     */
    public final boolean contains(final long longBits) {
        final var head = ranges.floor(new EntryImpl(longBits));
        return head != null
            && Long.compareUnsigned(head.lowerBits, longBits) <= 0
            && Long.compareUnsigned(head.upperBits, longBits) >= 0;
    }

    /**
     * Returns {@code true} does not contain anything.
     *
     * @return {@code true} does not contain anything
     */
    public final boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final int rangeSize() {
        return ranges.size();
    }

    /**
     * Returns an immutable copy of this set.
     *
     * @return an immutable copy of this set
     */
    public abstract @NonNull ImmutableUnsignedLongSet immutableCopy();

    /**
     * Returns an mutable copy of this set.
     *
     * @return an mutable copy of this set
     */
    public final @NonNull MutableUnsignedLongSet mutableCopy() {
        return new MutableUnsignedLongSet(new TreeSet<>(ranges));
    }

    public final @NonNull NavigableSet<? extends Entry> ranges() {
        return Collections.unmodifiableNavigableSet(ranges);
    }

    final @NonNull NavigableSet<EntryImpl> trustedRanges() {
        return ranges;
    }

    @Override
    public final int hashCode() {
        return ranges.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj == this || obj instanceof UnsignedLongSet other && ranges.equals(other.ranges);
    }

    @Override
    public final String toString() {
        final var helper = MoreObjects.toStringHelper(this);

        final int size = ranges.size();
        switch (size) {
            case 0 -> {
                // no 'span' attribute
            }
            case 1 -> helper.add("span", ranges.first());
            default -> helper.add("span", new EntryImpl(ranges.first().lowerBits, ranges.last().upperBits));
        }

        return helper.add("size", size).toString();
    }
}
