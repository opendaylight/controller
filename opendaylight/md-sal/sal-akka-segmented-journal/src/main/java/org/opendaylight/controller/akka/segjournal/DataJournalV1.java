/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSystem;
import com.codahale.metrics.Histogram;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version 1 data journal. The segmented file is using small(ish) fixed-size entries. If we are requested to persist
 * a message which would end up being larger than a single entry, we fragment it. We actually keep two segmented files,
 * one for entries and one to keep track of index/offset relationship and chunk fragment information.
 *
 * @author Robert Varga
 */
final class DataJournalV1 extends DataJournal {
    /**
     * One of potentially many fragments of a journal entry. This class is used when a journal entry's serialized size
     * exceeds {@link DataJournalV1#FILE_ENTRY_SIZE} -- the serialized form is split into multiple fragments and
     * persisted that way. When we are reconstructing a journal, all fragments belonging to a particular journal entry
     * are reassembled.
     */
    private static final class Fragment extends DataJournalEntry {
        private final byte[] bytes;

        Fragment(final byte[] bytes) {
            this.bytes = requireNonNull(bytes);
        }
    }

    private static final class FragmentSerializer extends Serializer<Fragment> {
        static final FragmentSerializer INSTANCE = new FragmentSerializer();

        private FragmentSerializer() {
            // Hidden on purpose
        }

        @Override
        public void write(final Kryo kryo, final Output output, final Fragment object) {
            output.writeInt(object.bytes.length, true);
            output.write(object.bytes);
        }

        @Override
        public Fragment read(final Kryo kryo, final Input input, final Class<Fragment> type) {
            final int length = input.readInt(true);
            return new Fragment(input.readBytes(length));
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


    // A nominal tuple describing a range of offsets within the file. We could model this as an offset/length
    // tuple, but it should not matter that much.
    private static final class FragmentOffsets {
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
     * Helper class to deal with tracking how a particular index (a.k.a sequenceNr) maps to file entries. In the most
     * common case we end up with each journal entry taking up a single file entry -- i.e. the sequence number is a
     * function of file entry index. Unfortunately there are also fragmented journal entries, each of which increases
     * the distance between journal and file entry indices.
     *
     * A trivial way to track this mapping would see a Map populated for each sequence number -- hence locating file
     * entries that go into a particular journal entry would be a matter of a single lookup. A further improvement to
     * deal with (typical) prefix trimming would organize these mappings into a NavigableMap.
     *
     * The downside of such tracking is heavy memory usage, as each journal entry also has to have a mapping, which
     * has non-trivial cost in terms of both tracked data and interior Map nodes.
     *
     * This class implements a smarter alternative.
     */
    private static final class FragmentIndex {
        // Storage of all known fragments organized by the index (sequenceNr) they belong to. A key performance
        // optimization is that holes within this map also have semantics: they represent spans of single-file-entry
        // journal entries. Except for initial state we always keep the last entry around.
        private final NavigableMap<Long, FragmentOffsets> indexToOffsets = new TreeMap<>();
        private final SegmentedJournal<FragmentInfo> fragments;

        FragmentIndex(final SegmentedJournal<FragmentInfo> fragments) {
            this.fragments = requireNonNull(fragments);

            try (SegmentedJournalReader<FragmentInfo> reader = fragments.openReader(0)) {
                while (reader.hasNext()) {
                    final FragmentInfo info = reader.next().entry();
                    indexToOffsets.put(info.sequenceNr, new FragmentOffsets(info.start, info.end));
                }
            }
        }

        FragmentOffsets findIndex(final long index) {
            if (!indexToOffsets.isEmpty()) {
             // We have some fragments, let's see if they apply to the requested index...
                final Map.Entry<Long, FragmentOffsets> floor = indexToOffsets.floorEntry(index);
                if (floor != null) {
                    // ... they do, floor contains the last preceding fragmented entry ...
                    final long key = floor.getKey();
                    if (key == index) {
                        // ... which is exactly what we are looking for :)
                        return floor.getValue();
                    }

                    // ... it is a preceding entry. Take it's end offset and increment it by the difference between
                    //     index and key (which is the significant part of the hole in the map).
                    return new FragmentOffsets(floor.getValue().end + index - key);
                }
            }

            // We either have initial state or the requested index is before the first fragmented entry we are tracking.
            return new FragmentOffsets(index);
        }

        long tailDelta() {
            return delta(indexToOffsets.lastEntry());
        }

        private static long delta(final Map.Entry<Long, FragmentOffsets> entry) {
            return entry == null ? 0 : entry.getValue().end - entry.getKey();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);
    private static final int FILE_ENTRY_SIZE = 128 * 1024;

    private final SegmentedJournal<DataJournalEntry> entries;
    private final FragmentIndex index;

    DataJournalV1(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final File directory, final int maxSegmentSize) {
        super(persistenceId, messageSize);
        entries = SegmentedJournal.<DataJournalEntry>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("entries")
                .withNamespace(Namespace.builder()
                    .register(new DataJournalEntrySerializer(system), FromPersistence.class, ToPersistence.class)
                    .register(FragmentSerializer.INSTANCE, Fragment.class)
                    .build())
                .withMaxEntrySize(FILE_ENTRY_SIZE).withMaxSegmentSize(maxSegmentSize)
                .build();
        index = new FragmentIndex(SegmentedJournal.<FragmentInfo>builder()
            .withStorageLevel(storage).withDirectory(directory).withName("fragments")
            .withNamespace(Namespace.builder().register(FragmentInfoSerializer.INSTANCE, FragmentInfo.class).build())
            .withMaxEntrySize(64).withMaxSegmentSize(128 * 1024)
            .build());
    }

    @Override
    long lastWrittenIndex() {
        return entries.writer().getLastIndex() - index.tailDelta();
    }

    @Override
    void commitTo(final long index) {
        // TODO Auto-generated method stub

    }

    @Override
    void compactTo(final long index) {
        // TODO Auto-generated method stub

    }

    @Override
    void close() {
        entries.close();
    }

    @Override
    void handleReplayMessages(final ReplayMessages message, final long from) {


        // TODO Auto-generated method stub

    }

    @Override
    void handleWriteMessages(final WriteMessages message) {
        // TODO Auto-generated method stub

    }
}
