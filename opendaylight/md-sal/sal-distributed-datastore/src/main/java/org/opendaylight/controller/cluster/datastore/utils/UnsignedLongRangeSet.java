/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.BoundType;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * Utility {@link RangeSet}-like class, specialized for holding {@link UnsignedLong}. It does not directly implement
 * the {@link RangeSet} interface, but allows converting to and from it. Internal implementation takes advantage of
 * knowing that {@link UnsignedLong} is a discrete type and that it can be stored in a long.
 *
 * @author Robert Varga
 */
@Beta
public final class UnsignedLongRangeSet implements Mutable {
    // Note: mutable to allow efficient merges
    private static final class LongRange implements Comparable<LongRange>, Mutable {
        long lowerBits;
        long upperBits;

        private LongRange(final long lowerBits, final long upperBits) {
            this.lowerBits = lowerBits;
            this.upperBits = upperBits;
        }

        static LongRange of(final long longBits) {
            return of(longBits, longBits);
        }

        static LongRange of(final long lowerBits, final long upperBits) {
            return new LongRange(lowerBits, upperBits);
        }

        static LongRange of(final Range<UnsignedLong> range) {
            verify(range.lowerBoundType() == BoundType.CLOSED && range.upperBoundType() == BoundType.OPEN,
                "Unexpected range %s", range);
            return of (range.lowerEndpoint().longValue(), range.upperEndpoint().longValue() - 1);
        }

        boolean contains(final long longBits) {
            return Long.compareUnsigned(lowerBits, longBits) <= 0 && Long.compareUnsigned(upperBits, longBits) >= 0;
        }

        LongRange copy() {
            return new LongRange(lowerBits, upperBits);
        }

        Range<UnsignedLong> toUnsigned() {
            return Range.closedOpen(UnsignedLong.fromLongBits(lowerBits), UnsignedLong.fromLongBits(upperBits + 1));
        }

        @Override
        public int compareTo(final LongRange o) {
            return Long.compareUnsigned(lowerBits, o.lowerBits);
        }

        @Override
        public String toString() {
            final String lowerStr = Long.toUnsignedString(lowerBits);
            return lowerBits == upperBits ? lowerStr : lowerStr + "-" + Long.toUnsignedString(upperBits);
        }
    }

    private final NavigableSet<LongRange> ranges;

    private UnsignedLongRangeSet(final TreeSet<LongRange> ranges) {
        this.ranges = requireNonNull(ranges);
    }

    public UnsignedLongRangeSet() {
        this(new TreeSet<>());
    }

    public void add(final long longBits) {
        final LongRange range = LongRange.of(longBits);

        final Iterator<LongRange> headIt = headIter(range);
        if (headIt.hasNext()) {
            final var head = headIt.next();
            if (head.contains(longBits)) {
                return;
            }
            if (head.upperBits + 1 == longBits) {
                head.upperBits = longBits;
                final var it = tailIter(range);
                if (it.hasNext()) {
                    final var tail = it.next();
                    if (tail.lowerBits -1 == longBits) {
                        tail.lowerBits = head.lowerBits;
                        headIt.remove();
                    }
                }
                return;
            }
        }

        final var tailIt = tailIter(range);
        if (tailIt.hasNext()) {
            final var tail = tailIt.next();
            if (tail.contains(longBits)) {
                return;
            }
            if (tail.lowerBits - 1 == longBits) {
                tail.lowerBits = longBits;
                return;
            }
        }

        ranges.add(range);
        return;
    }

    public void add(final UnsignedLong value) {
        add(value.longValue());
    }

    public boolean contains(final long longBits) {
        final var headIt = headIter(LongRange.of(longBits));
        return headIt.hasNext() && headIt.next().contains(longBits);
    }

    public boolean contains(final UnsignedLong value) {
        return contains(value.longValue());
    }

    public UnsignedLongRangeSet copy() {
        return new UnsignedLongRangeSet(new TreeSet<>(Collections2.transform(ranges, LongRange::copy)));
    }

    public ImmutableRangeSet<UnsignedLong> toImmutable() {
        return ImmutableRangeSet.copyOf(Collections2.transform(ranges, LongRange::toUnsigned));
    }

    private Iterator<LongRange> headIter(final LongRange range) {
        return ranges.headSet(range, true).descendingIterator();
    }

    private Iterator<LongRange> tailIter(final LongRange range) {
        return ranges.tailSet(range, true).iterator();
    }

    @Override
    public String toString() {
        final int size = ranges.size();
        final LongRange span;
        switch (size) {
            case 0:
                span = null;
                break;
            case 1:
                span = ranges.first();
                break;
            default:
                span = LongRange.of(ranges.first().lowerBits, ranges.last().upperBits);
        }

        return MoreObjects.toStringHelper(this).omitNullValues().add("span", span).add("rangeSize", size).toString();
    }
}
