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
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * A class holding an equivalent of {@code Set<UnsignedLong>}. It is geared towards efficiently tracking ranges of
 * objects, similar to what a {@code RangeSet<UnsignedLong>} would do.
 *
 * <p>Unlike a {@code RangeSet}, though, this class takes advantage of knowing that an unsigned long is a discrete unit
 * and can be stored in a simple {@code long}.
 */
@NonNullByDefault
public abstract sealed class UnsignedLongSet permits ImmutableUnsignedLongSet, MutableUnsignedLongSet {
    /**
     * A single entry tracked in this set. It represents all discrete values in the range
     * {@code [lowerBits(), upperBits()]}.
     *
     * <p>The equivalent construct in {@code RangeSet<UnsignedLong>} terms is
     * {@code Range.closedOpen(UnsignedLong.fromLongBits(lowerBits), UnsignedLong.fromLongBits(upperBits + 1))}.
     *
     * <p>We are saving two objects with this specialization and are more expressive by using closed range.
     */
    public sealed interface Entry extends Immutable permits EntryImpl {

        long lowerBits();

        long upperBits();
    }

    // Internal access and common implementation details.
    sealed interface EntryImpl extends Comparable<EntryImpl>, Entry {
        @Override
        @SuppressWarnings("checkstyle:parameterName")
        default int compareTo(final EntryImpl o) {
            // Tailored to our algorithm: this is all we need to do to attain correct NavigableMap organization.
            // The rest of the magic is what we do with the TreeMap, i.e. how we merge entries when expanding.
            return Long.compareUnsigned(lowerBits(), o.lowerBits());
        }

        default EntryN withLowerBits(final long newLowerBits) {
            return new EntryN(newLowerBits, upperBits());
        }

        default EntryN withUpperBits(final long newUpperBits) {
            return new EntryN(lowerBits(), newUpperBits);
        }

        default int hashCodeImpl() {
            return Long.hashCode(lowerBits()) * 31 + Long.hashCode(upperBits());
        }

        default boolean equalsImpl(final @Nullable Object obj) {
            return obj == this || obj instanceof Entry other
                && lowerBits() == other.lowerBits() && upperBits() == other.upperBits();
        }

        default String toStringImpl() {
            return "[" + Long.toUnsignedString(lowerBits()) + ".." + Long.toUnsignedString(upperBits()) + "]";
        }
    }

    /**
     * A {@code [lowerBits..lowerBits]} entry.
     *
     * @param lowerBits lower and upper bound
     */
    record Entry1(long lowerBits) implements EntryImpl {
        @Override
        public long upperBits() {
            return lowerBits;
        }

        @Override
        public int hashCode() {
            return hashCodeImpl();
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            return equalsImpl(obj);
        }

        @Override
        public String toString() {
            return toStringImpl();
        }
    }

    /**
     * A {@code [lowerBits..upperBits]} entry.
     *
     * @param lowerBits lower bound
     * @param upperBits upper bound
     */
    record EntryN(long lowerBits, long upperBits) implements EntryImpl {
        @Override
        public int hashCode() {
            return hashCodeImpl();
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            return equalsImpl(obj);
        }

        @Override
        public String toString() {
            return toStringImpl();
        }
    }

    // The idea is rather simple, we track a NavigableSet of range entries, ordered by their lower bound. This means
    // that for a contains() operation we just need the first headSet() entry. For insert operations we just update
    // either the lower bound or the upper bound of an existing entry. When we do, we also look at prev/next entry and
    // if they are contiguous with the updated entry, we adjust the entry once more and remove the prev/next entry.
    private final NavigableSet<EntryImpl> ranges;

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
        final var head = ranges.floor(new Entry1(longBits));
        return head != null
            && Long.compareUnsigned(head.lowerBits(), longBits) <= 0
            && Long.compareUnsigned(head.upperBits(), longBits) >= 0;
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
    public abstract ImmutableUnsignedLongSet immutableCopy();

    /**
     * Returns an mutable copy of this set.
     *
     * @return an mutable copy of this set
     */
    public final MutableUnsignedLongSet mutableCopy() {
        return new MutableUnsignedLongSet(new TreeSet<>(ranges));
    }

    public final NavigableSet<? extends Entry> ranges() {
        return Collections.unmodifiableNavigableSet(ranges);
    }

    final NavigableSet<EntryImpl> trustedRanges() {
        return ranges;
    }

    @Override
    public final int hashCode() {
        return ranges.hashCode();
    }

    @Override
    public final boolean equals(final @Nullable Object obj) {
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
            default -> helper.add("span", new EntryN(ranges.first().lowerBits(), ranges.last().upperBits()));
        }

        return helper.add("size", size).toString();
    }
}
