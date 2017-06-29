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
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
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
    // FIXME: this is just to get us started
    private final RangeSet<UnsignedLong> rangeset;

    private UnsignedLongRangeSet(final RangeSet<UnsignedLong> rangeset) {
        this.rangeset = Preconditions.checkNotNull(rangeset);
    }

    public static UnsignedLongRangeSet create() {
        return new UnsignedLongRangeSet(TreeRangeSet.create());
    }

    public static UnsignedLongRangeSet create(final RangeSet<UnsignedLong> input) {
        return new UnsignedLongRangeSet(TreeRangeSet.create(input));
    }

    public RangeSet<UnsignedLong> toImmutable() {
        return ImmutableRangeSet.copyOf(rangeset);
    }

    public void add(final long longBits) {
        add(UnsignedLong.fromLongBits(longBits));
    }

    public void add(final UnsignedLong value) {
        rangeset.add(Range.closedOpen(value, UnsignedLong.ONE.plus(value)));
    }

    public boolean contains(final UnsignedLong value) {
        return rangeset.contains(value);
    }

    public boolean contains(final long longBits) {
        return contains(UnsignedLong.fromLongBits(longBits));
    }

    public UnsignedLongRangeSet copy() {
        return new UnsignedLongRangeSet(TreeRangeSet.create(rangeset));
    }
}
