/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Mutable;

@Beta
public final class MutableUnsignedLongSet extends UnsignedLongSet implements Mutable {
    MutableUnsignedLongSet(final TreeSet<Entry> ranges) {
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
        addOne(trustedRanges(), longBits);
    }

    public void addAll(final UnsignedLongSet other) {
        final var ranges = trustedRanges();
        for (var range : other.trustedRanges()) {
            if (range.lowerBits == range.upperBits) {
                addOne(ranges, range.lowerBits);
            } else {
                addRange(ranges, range);
            }
        }
    }

    private static void addOne(final NavigableSet<Entry> ranges, final long longBits) {
        final var range = Entry.of(longBits);
        // We need Iterator.remove() to perform efficient merge below
        final var headIt = ranges.headSet(range, true).descendingIterator();
        if (headIt.hasNext()) {
            final var head = headIt.next();
            if (Long.compareUnsigned(head.upperBits, longBits) >= 0) {
                // Already contained, this is a no-op
                return;
            }

            // Merge into head entry if possible
            if (head.upperBits + 1 == longBits) {
                head.upperBits = longBits;

                // Potentially merge head entry and tail entry
                final var tail = ranges.higher(range);
                if (tail != null) {
                    if (tail.lowerBits - 1 == longBits) {
                        // Expand tail, remove head
                        tail.lowerBits = head.lowerBits;
                        headIt.remove();
                    }
                }
                return;
            }
        }

        final var tail = ranges.higher(range);
        if (tail != null) {
            // Merge into tail entry if possible
            if (tail.lowerBits - 1 == longBits) {
                tail.lowerBits = longBits;
                return;
            }
        }

        // No luck, store a new entry
        ranges.add(range);
    }

    private static void addRange(final NavigableSet<Entry> ranges, final Entry range) {
        // If the start of the range is already covered by an existing range, we can expand that
        final var lower = ranges.floor(range);
        if (lower != null && Long.compareUnsigned(lower.upperBits + 1, range.lowerBits) >= 0) {
            expandLower(ranges, lower, range.upperBits);
            return;
        }

        // If the end of the range is already covered by an existing range, we can expand that
        final var upper = ranges.floor(Entry.of(range.upperBits));
        if (upper != null && Long.compareUnsigned(upper.lowerBits - 1, range.upperBits) <= 0) {
            // Quick check: if we did not find a lower range at all, we might be expanding the entire span, in which
            // case upper needs to become the first entry
            if (lower == null) {
                ranges.headSet(upper, false).clear();
            }

            expandUpper(ranges, upper, range.lowerBits, range.upperBits);
            return;
        }

        // No luck, insert
        ranges.add(range.copy());
    }

    private static void expandLower(final NavigableSet<Entry> ranges, final Entry entry, final long upperBits) {
        if (Long.compareUnsigned(entry.upperBits, upperBits) >= 0) {
            // Already contained, this is a no-op
            return;
        }

        // Acquire any ranges after floor and clean them up
        final var tailIt = ranges.tailSet(entry, false).iterator();
        final long nextLower = upperBits + 1;
        while (tailIt.hasNext()) {
            final var tail = tailIt.next();
            if (Long.compareUnsigned(tail.lowerBits, nextLower) > 0) {
                // There is gap, nothing more to cleanup
                break;
            }

            // We can merge this entry into floor...
            tailIt.remove();

            if (Long.compareUnsigned(tail.upperBits, nextLower) >= 0) {
                // ... but we need to expand floor accordingly and after that we are done
                entry.upperBits = tail.upperBits;
                return;
            }
        }

        // Expand floor to include this range and we are done
        entry.upperBits = upperBits;
    }

    private static void expandUpper(final NavigableSet<Entry> ranges, final Entry entry, final long lowerBits,
            final long upperBits) {
        if (Long.compareUnsigned(entry.upperBits, upperBits) >= 0) {
            // Upper end is already covered
            entry.lowerBits = lowerBits;
            return;
        }

        // We are expanding the entry's upper boundary, we need to check if we need to coalesce following entries
        long newUpper = upperBits;
        final var tailIt = ranges.tailSet(entry, false).iterator();
        if (tailIt.hasNext()) {
            final var tail = tailIt.next();
            if (Long.compareUnsigned(tail.lowerBits, newUpper + 1) <= 0) {
                tailIt.remove();
                newUpper = tail.upperBits;
            }
        }

        entry.lowerBits = lowerBits;
        entry.upperBits = newUpper;
    }

    public ImmutableRangeSet<UnsignedLong> toRangeSet() {
        return ImmutableRangeSet.copyOf(Collections2.transform(trustedRanges(), Entry::toUnsigned));
    }
}
