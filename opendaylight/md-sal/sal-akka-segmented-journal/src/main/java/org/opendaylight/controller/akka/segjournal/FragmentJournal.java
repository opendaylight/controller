/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Stopwatch;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to deal with tracking how a particular sequence number maps to file entries. In the most common case we
 * end up with each journal entry taking up a single file entry -- i.e. the sequence number is a function of file entry
 * index. Unfortunately there are also fragmented journal entries, each of which increases the distance between journal
 * and file entry indices.
 *
 * <p>
 * A trivial way to track this mapping would see a Map populated for each sequence number -- hence locating file
 * entries that go into a particular journal entry would be a matter of a single lookup. A further improvement to
 * deal with (typical) prefix trimming would organize these mappings into a NavigableMap.
 *
 * <p>
 * The downside of such tracking is heavy memory usage, as each journal entry also has to have a mapping, which
 * has non-trivial cost in terms of both tracked data and interior Map nodes.
 *
 * <p>
 * This class implements a smarter alternative, using a SegmentedJournal as the backing persistence store.
 */
final class FragmentJournal {
    // A nominal tuple describing a range of offsets within the file. We could model this as an offset/length
    // tuple, but it should not matter that much.
    static final class FragmentOffsets {
        final long start;
        final long end;

        FragmentOffsets(final long start) {
            this(start, start);
        }

        FragmentOffsets(final long start, final long end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Information about a fragmented entry. These records are maintained in a separate segmented file. Whenever we are
     * forced to fragment an entry, a corresponding FragmentInfo is persisted first.
     */
    private static final class FragmentInfo {
        final long sequenceNr;
        final long start;
        final long end;

        FragmentInfo(final long sequenceNr, final long start, final long end) {
            this.sequenceNr = sequenceNr;
            this.start = start;
            this.end = end;
        }
    }

    private static final class FragmentInfoSerializer extends Serializer<FragmentInfo> {
        static final FragmentInfoSerializer INSTANCE = new FragmentInfoSerializer();

        private FragmentInfoSerializer() {
            // Hidden on purpose
        }

        @Override
        public void write(final Kryo kryo, final Output output, final FragmentInfo object) {
            output.writeLong(object.sequenceNr, true);
            output.writeLong(object.start, true);
            output.writeLong(object.end, true);
        }

        @Override
        public FragmentInfo read(final Kryo kryo, final Input input, final Class<FragmentInfo> type) {
            return new FragmentInfo(input.readLong(true), input.readLong(true), input.readLong(true));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(FragmentJournal.class);

    // Storage of all known fragments organized by the sequenceNr they belong to. A key performance optimization is that
    // holes within this map also have semantics: they represent spans of single-file-entry journal entries. Except for
    // initial state we always keep the last entry around.
    private final NavigableMap<Long, FragmentOffsets> sequenceToOffsets = new TreeMap<>();
    private final SegmentedJournal<FragmentInfo> fragments;

    FragmentJournal(final StorageLevel storage, final File directory) {
        fragments = SegmentedJournal.<FragmentInfo>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("fragments")
                .withNamespace(Namespace.builder()
                    .register(FragmentInfoSerializer.INSTANCE, FragmentInfo.class)
                    .build())
                .withMaxEntrySize(64).withMaxSegmentSize(128 * 1024)
                .build();

        final Stopwatch sw = Stopwatch.createStarted();
        try (SegmentedJournalReader<FragmentInfo> reader = fragments.openReader(0)) {
            while (reader.hasNext()) {
                final FragmentInfo info = reader.next().entry();
                sequenceToOffsets.put(info.sequenceNr, new FragmentOffsets(info.start, info.end));
            }
        }
        LOG.debug("Recovered {} fragments in {}", sequenceToOffsets.size(), sw);
    }

    FragmentOffsets indexOf(final long sequenceNr) {
        if (!sequenceToOffsets.isEmpty()) {
         // We have some fragments, let's see if they apply to the requested index...
            final Map.Entry<Long, FragmentOffsets> floor = sequenceToOffsets.floorEntry(sequenceNr);
            if (floor != null) {
                // ... they do, floor contains the last preceding fragmented entry ...
                final long key = floor.getKey();
                if (key == sequenceNr) {
                    // ... which is exactly what we are looking for :)
                    return floor.getValue();
                }

                // ... it is a preceding entry. Take it's end offset and increment it by the difference between
                //     index and key (which is the significant part of the hole in the map).
                return new FragmentOffsets(floor.getValue().end + sequenceNr - key);
            }
        }

        // We either have initial state or the requested index is before the first fragmented entry we are tracking.
        return new FragmentOffsets(sequenceNr);
    }

    /**
     * Return the difference between the segmented journal index and sequence number at the end of the file. This
     * accounts for all fragmented entries we are currently tracking.
     *
     * @return A non-negative delta.
     */
    long tailDelta() {
        final Map.Entry<Long, FragmentOffsets> entry = sequenceToOffsets.lastEntry();
        return entry == null ? 0 : entry.getValue().end - entry.getKey();
    }
}
