/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal.index;

import com.google.common.primitives.Ints;
import java.util.Arrays;

/**
 *
 */
public final class ConstrainedJournalIndex implements JournalIndex {
    // Absolute index to the first index
    private final long firstIndex;

    // most significant 32 bits = relative index, unsigned
    // least significant 32 bits = file offset, signed
    // naturally sorted, as relative index in monotonic
    private long[] entries;
    private int prevOffset = -1;

    public ConstrainedJournalIndex(final long firstIndex) {
        this.firstIndex = firstIndex;
        // FIXME: derive from maxEntrySize and maxSegmentSize?
        entries = new long[32];
    }

    @Override
    public void index(final long index, final int position) {
        if (index < firstIndex) {
            throw new IllegalArgumentException("Bad index " + index);
        }
        if (position < 0) {
            throw new IllegalArgumentException("Bad position " + position);
        }

        final int relIndex = Ints.checkedCast(index - firstIndex);
        final int prevRelIndex = prevOffset < 0 ? -1 : (int) entries[prevOffset] >>> Integer.SIZE;
        if (prevRelIndex + 1 != relIndex) {
            throw new IllegalArgumentException("Cannot append " + index + " after " + (firstIndex + prevRelIndex));
        }

        if (entries.length == prevOffset) {
            // FIXME: smarter policy: start dropping entries at some point
            entries = Arrays.copyOf(entries, entries.length * 2);
        }

        entries[++prevOffset] = (long) relIndex << Integer.SIZE | position;
    }

    @Override
    public Position lookup(final long index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Position truncate(final long index) {
        // TODO Auto-generated method stub
        return null;
    }
}
