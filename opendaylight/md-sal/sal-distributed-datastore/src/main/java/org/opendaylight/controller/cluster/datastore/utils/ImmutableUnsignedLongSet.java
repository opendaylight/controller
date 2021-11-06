/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSortedSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObject;

@Beta
public final class ImmutableUnsignedLongSet extends UnsignedLongSet implements Immutable, WritableObject {
    // Do not all
    private static final int ARRAY_MAX_ELEMENTS = 4096;

    private static final @NonNull ImmutableUnsignedLongSet EMPTY =
        new ImmutableUnsignedLongSet(ImmutableSortedSet.of());

    private ImmutableUnsignedLongSet(final NavigableSet<Entry> ranges) {
        super(ranges);
    }

    static @NonNull ImmutableUnsignedLongSet copyOf(final MutableUnsignedLongSet mutable) {
        if (mutable.isEmpty()) {
            return EMPTY;
        }
        if (mutable.size() <= ARRAY_MAX_ELEMENTS) {
            return new ImmutableUnsignedLongSet(ImmutableSortedSet.copyOfSorted(mutable.trustedRanges()));
        }
        return new ImmutableUnsignedLongSet(new TreeSet<>(mutable.trustedRanges()));
    }

    static @NonNull ImmutableUnsignedLongSet of(final TreeSet<Entry> ranges) {
        return ranges.isEmpty() ? EMPTY : new ImmutableUnsignedLongSet(ranges);
    }

    public static @NonNull ImmutableUnsignedLongSet of() {
        return EMPTY;
    }

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return this;
    }

    public static @NonNull ImmutableUnsignedLongSet readFrom(final DataInput in) throws IOException {
        return readFrom(in, in.readInt());
    }

    public static @NonNull ImmutableUnsignedLongSet readFrom(final DataInput in, final int size) throws IOException {
        if (size == 0) {
            return EMPTY;
        }

        final NavigableSet<Entry> ranges;
        if (size <= ARRAY_MAX_ELEMENTS) {
            final var entries = new ArrayList<Entry>(size);
            for (int i = 0; i < size; ++i) {
                entries.add(Entry.readUnsigned(in));
            }
            ranges = ImmutableSortedSet.copyOf(entries);
        } else {
            ranges = new TreeSet<>();
            for (int i = 0; i < size; ++i) {
                ranges.add(Entry.readUnsigned(in));
            }
        }
        return new ImmutableUnsignedLongSet(ranges);
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeInt(size());
        writeRanges(out);
    }

    public void writeRangesTo(final @NonNull DataOutput out, final int size) throws IOException {
        if (size != size()) {
            throw new IOException("Mismatched size: expected " + size() + ", got " + size);
        }
        writeRanges(out);
    }

    private void writeRanges(final @NonNull DataOutput out) throws IOException {
        for (var range : trustedRanges()) {
            range.writeUnsigned(out);
        }
    }
}
