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

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return ImmutableUnsignedLongSet.copyOf(this);
    }

    public void add(final long longBits) {
        final var ranges = trustedRanges();
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

    public ImmutableRangeSet<UnsignedLong> toRangeSet() {
        return ImmutableRangeSet.copyOf(Collections2.transform(trustedRanges(), Entry::toUnsigned));
    }
}
