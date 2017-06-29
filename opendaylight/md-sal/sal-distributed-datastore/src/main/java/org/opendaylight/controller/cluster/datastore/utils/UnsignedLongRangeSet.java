/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableRangeSet.Builder;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            final UnsignedLong ul = UnsignedLong.fromLongBits(value);
            return Range.closedOpen(ul, UnsignedLong.ONE.plus(ul));
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
            return Long.compareUnsigned(lower, longBits) <= 0 && Long.compareUnsigned(upper, longBits) >= 0;
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
            return Range.closedOpen(UnsignedLong.fromLongBits(lower),
                // FIXME: deal with overflow here
                UnsignedLong.ONE.plus(UnsignedLong.fromLongBits(upper)));
        }
    }

    private final List<DiscreteRange> ranges;

    private UnsignedLongRangeSet(final List<DiscreteRange> ranges) {
        this.ranges = Preconditions.checkNotNull(ranges);
    }

    public static UnsignedLongRangeSet create() {
        return new UnsignedLongRangeSet(new ArrayList<>());
    }

    public static UnsignedLongRangeSet create(final RangeSet<UnsignedLong> input) {
        final Set<Range<UnsignedLong>> rangeset = input.asRanges();
        final List<DiscreteRange> ranges = new ArrayList<>(rangeset.size());

        for (Range<UnsignedLong> range : rangeset) {
            // FIXME: convert range to DiscreteRange
        }

        return new UnsignedLongRangeSet(ranges);
    }

    public RangeSet<UnsignedLong> toImmutable() {
        Builder<UnsignedLong> b = ImmutableRangeSet.builder();

        for (DiscreteRange range : ranges) {
            b.add(range.toContinuous());
        }

        return b.build();
    }

    public void add(final long longBits) {
        // FIXME: implement this

    }

    public void add(final UnsignedLong value) {
        add(value.longValue());
    }

    public boolean contains(final UnsignedLong value) {
        return contains(value.longValue());
    }

    public boolean contains(final long longBits) {
        // FIXME: speed this up using bisection
        return ranges.stream().anyMatch(range -> range.contains(longBits));
    }

    public UnsignedLongRangeSet copy() {
        return new UnsignedLongRangeSet(new ArrayList<>(ranges));
    }
}
