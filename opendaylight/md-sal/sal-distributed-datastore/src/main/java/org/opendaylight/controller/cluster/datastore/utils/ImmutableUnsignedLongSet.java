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
import java.util.NavigableSet;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.slf4j.LoggerFactory;

@Beta
public final class ImmutableUnsignedLongSet extends UnsignedLongSet implements Immutable, WritableObject {
    // TreeSet has a large per-entry overhead (40-64 bytes as of OpenJDK 21), so we prefer allocating
    // an ImmutableSortedSet, which is backed by an array of up to this many elements. For larger sizes we opt to pay
    // the TreeMap overhead so as not to create huge objects on the heap.
    //
    // Assuming each Entry costs 32 bytes (as of OpenJDK 21).
    //
    //   size() | Entry | Array size | TreeSet.Entry | array objs | tree objs | array memory | tree memory
    //   --------------------------------------------------------------------------------------------------
    //     2K      64Ki     8/ 16Ki      80 - 128Ki      2K+3         ~4K        72  -  80Ki    144 -192Ki
    //     4K     128Ki    16/ 32Ki     160 - 256Ki      4K+3         ~8K       144  - 160Ki    288 -384Ki
    //     8K     256Ki    32/ 64Ki     320 - 512Ki      8K+3        ~16K       288  - 320Ki    576 -768Ki
    //    16K     512Ki    64/128Ki    0.63 -   1Mi     16K+3        ~32K       576  - 640Ki    1.1 -1.5Mi
    //    32K      ~1Mi   128/256Ki    1.25 -   2Mi     32K+3        ~64K       1.1  -1.25Mi    2.25-3  Mi
    //    64K      ~2Mi   256/512Ki    2.5  -   4Mi     64K+3       ~128K       2.25 -2.5 Mi    4.5 -6  Mi
    //
    // From performance perspective, ImmutableSortedSet performs a bisect search, so the amount of jumping around is
    // roughly the same, except the access pattern might be more recognizable by the CPU prefetch logic. Overall it
    // needs only 41.67-50% the memory.
    //
    // We need to take into consideration G1 GC (default in OpenJDK 21), documentation here:
    // https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html#GUID-D74F3CC7-CC9F-45B5-B03D-510AEEAC2DAC
    //
    // We aim to have arrays always smaller than half a region, i.e. always strictly less than 512KiB, we need to
    // account for Object[] sizing. In OpenJDK 21, arrays have 16 bytes of overhead, so we should rarely use more than
    // 65532 entries, just to be on the safe side. They should work fine with heaps of up to 2GiB. At 8GiB heap we can
    // expect objects to reliably fit into a single 4MiB region.
    //
    // The other part of the picture are cache sizes in modern CPUs. Here there is quite a bit of variation, but let us
    // assume:
    //  - 32-64KiB L1 cache per core
    //  - 0.5-2MiB L2 cache per core, perhaps shared between cores
    //  - 16-32MiB unified L3 cache
    //
    // Based on all of this, we choose to default to 8190 entries, so that we end up with 64KiB arrays. In this case the
    // array roughly fits in the L1 and the complete dataset fits into L2.
    //
    // For comparison, 65532 entries result in 524_272-byte arrays (fitting into L2) and the data set easily fitting
    // into L3.
    //
    // TODO: it would seem that a more lookup-performant (than array) and memory-efficient (than TreeMap) version could
    //       be achieved by employing S+Tree as detailed in https://curiouscoding.nl/posts/static-search-tree/
    //       and https://en.algorithmica.org/hpc/data-structures/s-tree/.
    //       Note that our key is either 8- or 16-bytes and indexing is another 8 bytes -- so we can fit two such
    //       objects into a single cache line. We can then decide how exactly to organize these nodes -- be it in an
    //       array, individual nodes, or something in-between. The in-between case might be interesting for mutations,
    //       as some operations (like extending a range) might become quite cheap.
    //       Perhaps a B2Tree, https://link.springer.com/article/10.1007/s13222-022-00409-y, is something we can
    //       leverage?
    private static final int DEFAULT_MAX_ARRAY = 8190;
    private static final String PROP_MAX_ARRAY =
        "org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet.max-array";
    private static final int MAX_ARRAY;

    static {
        final var logger = LoggerFactory.getLogger(ImmutableUnsignedLongSet.class);
        final int value = Integer.getInteger(PROP_MAX_ARRAY, DEFAULT_MAX_ARRAY);
        if (value < 0) {
            logger.warn("Ignoring invalid property {} value {}", PROP_MAX_ARRAY, value);
            MAX_ARRAY = DEFAULT_MAX_ARRAY;
        } else {
            MAX_ARRAY = value;
        }
        logger.debug("Using arrays for up to {} elements", MAX_ARRAY);
    }

    private static final @NonNull ImmutableUnsignedLongSet EMPTY =
        new ImmutableUnsignedLongSet(ImmutableSortedSet.of());

    private ImmutableUnsignedLongSet(final NavigableSet<Entry> ranges) {
        super(ranges);
    }

    static @NonNull ImmutableUnsignedLongSet copyOf(final MutableUnsignedLongSet mutable) {
        final var size = mutable.rangeSize();
        if (size == 0) {
            return of();
        }
        return new ImmutableUnsignedLongSet(size <= MAX_ARRAY ? ImmutableSortedSet.copyOfSorted(mutable.trustedRanges())
            : new TreeSet<>(mutable.trustedRanges()));
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
        return new ImmutableUnsignedLongSet(size <= MAX_ARRAY ? readArrayRanges(in, size) : readTreeRanges(in, size));
    }

    private static ImmutableSortedSet<Entry> readArrayRanges(final DataInput in, final int size) throws IOException {
        final var ranges = new Entry[size];
        for (int i = 0; i < size; ++i) {
            ranges[i] = Entry.readUnsigned(in);
        }
        return ImmutableSortedSet.copyOf(ranges);
    }

    private static TreeSet<Entry> readTreeRanges(final DataInput in, final int size) throws IOException {
        final var ranges = new TreeSet<Entry>();
        for (int i = 0; i < size; ++i) {
            ranges.add(Entry.readUnsigned(in));
        }
        return ranges;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeInt(rangeSize());
        writeRanges(out);
    }

    public void writeRangesTo(final @NonNull DataOutput out, final int size) throws IOException {
        final int rangeSize = rangeSize();
        if (size != rangeSize) {
            throw new IOException("Mismatched size: expected " + rangeSize + ", got " + size);
        }
        writeRanges(out);
    }

    private void writeRanges(final @NonNull DataOutput out) throws IOException {
        for (var range : trustedRanges()) {
            range.writeUnsigned(out);
        }
    }
}
