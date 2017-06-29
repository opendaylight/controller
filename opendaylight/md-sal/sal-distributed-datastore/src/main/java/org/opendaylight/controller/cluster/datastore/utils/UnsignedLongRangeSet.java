/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.UnsignedLong.fromLongBits;
import static java.lang.Long.compareUnsigned;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableRangeSet.Builder;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * Utility {@link RangeSet}-like class, specialized for holding {@link UnsignedLong}. It does not directly implement
 * the {@link RangeSet} interface, but allows converting to and from it. Internal implementation takes advantage of
 * knowing that {@link UnsignedLong} is a discrete type and that it can be stored in a long.
 *
 * @author Robert Varga
 */
@Beta
@NotThreadSafe
public final class UnsignedLongRangeSet implements Mutable {
    private static abstract class DiscreteRange {

        abstract long lowerBound();

        abstract long upperBound();

        abstract boolean contains(long longBits);

        abstract Range<UnsignedLong> toContinuous();
    }

    private static final class SingletonRange extends DiscreteRange {
        private final long value;

        SingletonRange(final long value) {
            this.value = value;
        }

        @Override
        long lowerBound() {
            return value;
        }

        @Override
        long upperBound() {
            return value;
        }

        @Override
        boolean contains(final long longBits) {
            return value == longBits;
        }

        @Override
        Range<UnsignedLong> toContinuous() {
            final UnsignedLong ul = fromLongBits(value);
            return value == -1 ? Range.atLeast(ul) : Range.closedOpen(ul, fromLongBits(value + 1));
        }
    }

    private static final class ClosedRange extends DiscreteRange {
        private final long lower;
        private final long upper;

        ClosedRange(final long lower, final long upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        boolean contains(final long longBits) {
            return compareUnsigned(lower, longBits) <= 0 && compareUnsigned(upper, longBits) >= 0;
        }

        @Override
        long lowerBound() {
            return lower;
        }

        @Override
        long upperBound() {
            return upper;
        }

        @Override
        Range<UnsignedLong> toContinuous() {
            final UnsignedLong ul = fromLongBits(lower);
            return upper == -1 ? Range.atLeast(ul) : Range.closedOpen(ul, fromLongBits(upper + 1));
        }
    }

    private static final DiscreteRange[] EMPTY_RANGES = new DiscreteRange[0];

    private DiscreteRange[] ranges;

    private UnsignedLongRangeSet(final DiscreteRange[] ranges) {
        this.ranges = checkNotNull(ranges);
    }

    public static UnsignedLongRangeSet create() {
        return new UnsignedLongRangeSet(EMPTY_RANGES);
    }

    public static UnsignedLongRangeSet create(final RangeSet<UnsignedLong> input) {
        final Set<Range<UnsignedLong>> rangeset = input.asRanges();
        final List<DiscreteRange> ranges = new ArrayList<>(rangeset.size());

        for (Range<UnsignedLong> range : rangeset) {
            final long floor;
            if (range.hasLowerBound()) {
                switch (range.lowerBoundType()) {
                    case CLOSED:
                        floor = range.lowerEndpoint().longValue();
                        break;
                    case OPEN:
                        // XXX: overflows?
                        floor = range.lowerEndpoint().longValue() + 1;
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled bound type " + range.lowerBoundType());
                }
            } else {
                floor = 0;
            }

            final long ceil;
            if (range.hasUpperBound()) {
                switch (range.upperBoundType()) {
                    case CLOSED:
                        ceil = range.upperEndpoint().longValue();
                        break;
                    case OPEN:
                        // XXX: underflows?
                        ceil = range.upperEndpoint().longValue() - 1;
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled bound type " + range.lowerBoundType());
                }
            } else {
                ceil = -1;
            }

            final int cmp = compareUnsigned(floor, ceil);
            if (cmp < 0) {
                ranges.add(new ClosedRange(floor, ceil));
            } else if (cmp == 0) {
                ranges.add(new SingletonRange(ceil));
            } else {
                throw new IllegalArgumentException("Invalid range " + range);
            }
        }

        return new UnsignedLongRangeSet(ranges.toArray(new DiscreteRange[0]));
    }

    public RangeSet<UnsignedLong> toImmutable() {
        Builder<UnsignedLong> b = ImmutableRangeSet.builder();

        for (DiscreteRange range : ranges) {
            b.add(range.toContinuous());
        }

        return b.build();
    }

    public void add(final long longBits) {
        // This is a search-and-insert operation, where we can end up with multiple options:
        // - the value is already present in one of the ranges
        // - the value extends an already-present range
        // - the value forms a new range, which is inserted at some point

        int item;
        for (item = 0; item < ranges.length; ++item) {
            final DiscreteRange range = ranges[item];

            final int lcmp = Long.compareUnsigned(range.lowerBound(), longBits);
            final int ucmp = Long.compareUnsigned(range.upperBound(), longBits);
            if (lcmp == 0 || ucmp == 0) {
                // Already present, no op
                return;
            }

            if (ucmp > 0) {
                if (lcmp < 0) {
                    // Covers the range, we are done
                    return;
                }

                if (longBits != -1 && range.lowerBound() == longBits + 1) {
                    // This insert extends an existing range, update it
                    // FIXME: we need to check for merge with previous range.
                    ranges[item] = new ClosedRange(longBits, range.upperBound());
                    return;
                }

                break;
            }

            if (longBits != 0 && range.upperBound() == longBits - 1) {
                // This insert extends an existing range, update it
                // FIXME: we need to check for merge with next range.
                ranges[item] = new ClosedRange(range.lowerBound(), longBits);
                return;
            }

            // Beyond this range, proceed to next one.
        }

        // We need to create a new range. item points to the last item checked
        if (item == ranges.length) {
            // Append new item
            ranges = Arrays.copyOf(ranges, ranges.length + 1);
            ranges[item] = new SingletonRange(longBits);
            return;
        }

        final DiscreteRange[] tmp = new DiscreteRange[ranges.length + 1];
        System.arraycopy(ranges, 0, tmp, 0, item);
        tmp[item] = new SingletonRange(longBits);
        System.arraycopy(ranges, item, tmp, item + 1, ranges.length - item);
    }

    public void add(final UnsignedLong value) {
        add(value.longValue());
    }

    public boolean contains(final UnsignedLong value) {
        return contains(value.longValue());
    }

    public boolean contains(final long longBits) {
        // XXX: speed this up using bisection?
        return Arrays.stream(ranges).anyMatch(range -> range.contains(longBits));
    }

    public UnsignedLongRangeSet copy() {
        return new UnsignedLongRangeSet(ranges.clone());
    }

    private static int compare(final DiscreteRange x, final DiscreteRange y) {
        // Comparison rules:
        // - if the ranges overlap, they match, return 0
        // - compare upper bounds
        if (compareUnsigned(x.upperBound(), y.lowerBound()) < 0) {
            return -1;
        }
        if (compareUnsigned(x.lowerBound(), y.upperBound()) > 0) {
            return 1;
        }
        return 0;
    }
}
