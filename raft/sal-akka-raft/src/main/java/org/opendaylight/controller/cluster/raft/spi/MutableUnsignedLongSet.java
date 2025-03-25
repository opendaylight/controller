/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.primitives.UnsignedLong;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * A mutable {@link UnsignedLongSet}.
 */
public final class MutableUnsignedLongSet extends UnsignedLongSet implements Mutable {
    MutableUnsignedLongSet(final TreeSet<EntryImpl> ranges) {
        super(ranges);
    }

    public static @NonNull MutableUnsignedLongSet of() {
        return new MutableUnsignedLongSet(new TreeSet<>());
    }

    public static @NonNull MutableUnsignedLongSet of(final long... ulongs) {
        final var ret = MutableUnsignedLongSet.of();
        for (long longBits : ulongs) {
            ret.add(longBits);
        }
        return ret;
    }

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return ImmutableUnsignedLongSet.copyOf(this);
    }

    public void add(final long longBits) {
        addOne(trustedRanges(), new Entry1(longBits));
    }

    public void addAll(final UnsignedLongSet other) {
        final var ranges = trustedRanges();
        for (var range : other.trustedRanges()) {
            if (range.lowerBits() == range.upperBits()) {
                addOne(ranges, range);
            } else {
                addRange(ranges, range);
            }
        }
    }

    private static void addOne(final NavigableSet<EntryImpl> ranges, final EntryImpl range) {
        final long longBits = range.lowerBits();

        // We need Iterator.remove() to perform efficient merge below
        final var headIt = ranges.headSet(range, true).descendingIterator();
        if (headIt.hasNext()) {
            final var head = headIt.next();
            if (Long.compareUnsigned(head.upperBits(), longBits) >= 0) {
                // Already contained, this is a no-op
                return;
            }

            // Merge into head entry if possible
            if (head.upperBits() + 1 == longBits) {
                // We will be replacing head
                headIt.remove();

                // Potentially merge head entry and tail entry
                final var tailIt = ranges.tailSet(range, false).iterator();
                if (tailIt.hasNext()) {
                    final var tail = tailIt.next();
                    if (tail.lowerBits() - 1 == longBits) {
                        // Update tail.lowerBits to include contents of head
                        tailIt.remove();
                        ranges.add(tail.withLowerBits(head.lowerBits()));
                        return;
                    }
                }

                // Update head.upperBits
                ranges.add(head.withUpperBits(longBits));
                return;
            }
        }

        final var tailIt = ranges.tailSet(range, false).iterator();
        if (tailIt.hasNext()) {
            final var tail = tailIt.next();
            // Merge into tail entry if possible
            if (tail.lowerBits() - 1 == longBits) {
                // Update tail.lowerBits
                tailIt.remove();
                ranges.add(tail.withLowerBits(longBits));
                return;
            }
        }

        // No luck, store a new entry
        ranges.add(range);
    }

    private static void addRange(final NavigableSet<EntryImpl> ranges, final EntryImpl range) {
        // If the start of the range is already covered by an existing range, we can expand that
        final var headIt = ranges.headSet(range, true).descendingIterator();
        final boolean hasFloor = headIt.hasNext();
        if (hasFloor) {
            final var floor = headIt.next();
            if (Long.compareUnsigned(floor.upperBits(), range.upperBits()) < 0
                && Long.compareUnsigned(floor.upperBits() + 1, range.lowerBits()) >= 0) {
                headIt.remove();
                ranges.add(expandFloor(ranges, floor, range.upperBits()));
                return;
            }
        }

        // If the end of the range is already covered by an existing range, we can expand that
        final var tailIt = ranges.headSet(new Entry1(range.upperBits()), true).descendingIterator();
        if (tailIt.hasNext()) {
            final var upper = tailIt.next();
            tailIt.remove();

            // Quick check: if we did not find a lower range at all, we might be expanding the entire span, in which
            // case upper needs to become the first entry
            if (!hasFloor) {
                ranges.headSet(upper, false).clear();
            }

            ranges.add(expandCeiling(ranges, upper, range.lowerBits(), range.upperBits()));
            return;
        }

        // No luck, insert
        ranges.add(range);
    }

    private static @NonNull EntryN expandFloor(final NavigableSet<EntryImpl> ranges, final EntryImpl floor,
            final long upperBits) {
        // Acquire any ranges after floor and clean them up
        final var tailIt = ranges.tailSet(floor, false).iterator();
        final long nextLower = upperBits + 1;
        while (tailIt.hasNext()) {
            final var tail = tailIt.next();
            if (Long.compareUnsigned(tail.lowerBits(), nextLower) > 0) {
                // There is gap, nothing more to cleanup
                break;
            }

            // We can merge this entry into floor...
            tailIt.remove();

            if (Long.compareUnsigned(tail.upperBits(), nextLower) >= 0) {
                // ... but we need to expand floor accordingly and after that we are done
                return floor.withUpperBits(tail.upperBits());
            }
        }

        // Expand floor to include this range and we are done
        return floor.withUpperBits(upperBits);
    }

    private static @NonNull EntryN expandCeiling(final NavigableSet<EntryImpl> ranges, final EntryImpl ceiling,
            final long lowerBits, final long upperBits) {
        if (Long.compareUnsigned(ceiling.upperBits(), upperBits) >= 0) {
            // Upper end is already covered
            return ceiling.withLowerBits(lowerBits);
        }

        // We are expanding the entry's upper boundary, we need to check if we need to coalesce following entries
        long newUpper = upperBits;
        final var tailIt = ranges.tailSet(ceiling, false).iterator();
        if (tailIt.hasNext()) {
            final var tail = tailIt.next();
            if (Long.compareUnsigned(tail.lowerBits(), newUpper + 1) <= 0) {
                tailIt.remove();
                newUpper = tail.upperBits();
            }
        }

        return new EntryN(lowerBits, newUpper);
    }

    // Provides compatibility with RangeSet<UnsignedLong> using [lower, upper + 1)
    public ImmutableRangeSet<UnsignedLong> toRangeSet() {
        final var builder = ImmutableRangeSet.<UnsignedLong>builder();
        for (var entry : trustedRanges()) {
            builder.add(Range.closedOpen(
                UnsignedLong.fromLongBits(entry.lowerBits()), UnsignedLong.fromLongBits(entry.upperBits() + 1)));
        }
        return builder.build();
    }
}
