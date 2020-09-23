/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;

/**
 * Version 1 data journal. The segmented file is using small(ish) fixed-size entries. If we are requested to persist
 * a message which would end up being larger than a single entry, we fragment it.
 *
 * @author Robert Varga
 */
final class DataJournalV1 extends DataJournal {
    /**
     * A single entry in the data journal. We do not store persistenceId, as that is a journal-invariant. There are
     * three distinct classes we use for entries:
     * <ul>
     *   <li>{@link ToPersistence}, used when writing to the backing file. If the entry fits within the file entry
     *       limits, this is all that gets written. Such entries are read back as {@link FromPersistence}.</li>
     *   <li>{@link FromPersistence}, used when reading from the backing file and entry was the result of a successful
     *        {@link ToPersistence} write.</li>
     *   <li>{@link Fragment}, used if the data cannot fit into a single file entry. When that happens, the data is
     *       split into a number of fragments and each is written out separately. Fragments are also read back and are
     *       reassembled before they are handed back to the application.</li>
     * </ul>
     */
    private abstract static class Entry {

    }

    private static final class Fragment extends Entry {
        private final byte[] bytes;

        Fragment(final byte[] bytes) {
            this.bytes = requireNonNull(bytes);
        }
    }

    private static final class FromPersistence extends Entry {
        private final String manifest;
        private final String writerUuid;
        private final Object payload;

        FromPersistence(final String manifest, final String writerUuid, final Object payload) {
            this.manifest = manifest;
            this.writerUuid = requireNonNull(writerUuid);
            this.payload = requireNonNull(payload);
        }

        PersistentRepr toRepr(final String persistenceId, final long sequenceNr) {
            return PersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false, null, writerUuid);
        }
    }

    private static final class ToPersistence extends Entry {
        private final PersistentRepr repr;

        ToPersistence(final PersistentRepr repr) {
            this.repr = requireNonNull(repr);
        }

        PersistentRepr repr() {
            return repr;
        }
    }

    private static final class EntrySerializer extends Serializer<Entry> {
        private final JavaSerializer serializer = new JavaSerializer();
        private final ExtendedActorSystem actorSystem;

        EntrySerializer(final ActorSystem actorSystem) {
            this.actorSystem = requireNonNull((ExtendedActorSystem) actorSystem);
        }

        @Override
        public void write(final Kryo kryo, final Output output, final Entry object) {
            verify(object instanceof ToPersistence);
            final PersistentRepr repr = ((ToPersistence) object).repr();
            output.writeString(repr.manifest());
            output.writeString(repr.writerUuid());
            serializer.write(kryo, output, repr.payload());
        }

        @Override
        public Entry read(final Kryo kryo, final Input input, final Class<Entry> type) {
            final String manifest = input.readString();
            final String uuid = input.readString();
            final Object payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
                (Callable<Object>)() -> serializer.read(kryo, input, type));
            return new FromPersistence(manifest, uuid, payload);
        }
    }

    private static final class FragmentSerializer extends Serializer<Fragment> {
        private static final FragmentSerializer INSTANCE = new FragmentSerializer();

        private FragmentSerializer() {
            // Hidden on purpose
        }

        @Override
        public void write(final Kryo kryo, final Output output, final Fragment object) {
            output.writeInt(object.bytes.length);
            output.write(object.bytes);
        }

        @Override
        public Fragment read(final Kryo kryo, final Input input, final Class<Fragment> type) {
            final int length = input.readInt();
            return new Fragment(input.readBytes(length));
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
    private static final class FileEntryIndex {
        // Storage of all known fragments organized by the index (sequenceNr) they belong to. A key performance
        // optimization is that holes within this map also have semantics: they represent spans of single-file-entry
        // journal entries.
        private final NavigableMap<Long, FragmentOffsets> fragments = new TreeMap<>();

        // Offset delta at the head of the segmented file. This is the residual effect of all fragmented entries we
        // are no longer tracking.
        private final long headDelta = 0;
        // The index of the entry which we purged.
        private final long lastPurgedIndex = -1;
        // The index of the entry known to be the last stored
        private final long lastWrittenIndex = -1;

        FragmentOffsets findIndex(final long index) {
            if (lastPurgedIndex >= index || lastWrittenIndex < index) {
                // Requested index was not stored yet or has already been purged, hence it is not present
                return null;
            }
            if (!fragments.isEmpty()) {
                // We have some fragments, let's see if they apply to the requested index...
                final Map.Entry<Long, FragmentOffsets> floor = fragments.floorEntry(index);
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

            // We have no recorded fragment which would affect the offset mapping -- just take the initial delta
            return new FragmentOffsets(headDelta + index);
        }
    }


    private static final int FILE_ENTRY_SIZE = 128 * 1024;

    private final SegmentedJournal<Entry> dataJournal;
    private FileEntryIndex index;

    DataJournalV1(final String persistenceId, final Histogram messageSize, final ActorSystem system,
        final StorageLevel storage, final File directory, final int maxSegmentSize) {
        super(persistenceId, messageSize);
        dataJournal = SegmentedJournal.<Entry>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("data")
                .withNamespace(Namespace.builder()
                    .register(new EntrySerializer(system), FromPersistence.class, ToPersistence.class)
                    .register(FragmentSerializer.INSTANCE, Fragment.class)
                    .build())
                .withMaxEntrySize(FILE_ENTRY_SIZE).withMaxSegmentSize(maxSegmentSize)
                .build();
    }

    @Override
    long lastWrittenIndex() {
        ensureIndex();
        return index.lastWrittenIndex;
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
        dataJournal.close();
    }

    @Override
    void handleReplayMessages(final ReplayMessages message, final long from) {
        // TODO Auto-generated method stub

    }

    @Override
    void handleWriteMessages(final WriteMessages message) {
        // TODO Auto-generated method stub

    }

    private void ensureIndex() {
        if (index == null) {
            // FIXME: properly initialize from dataJournal
            index = new FileEntryIndex();
        }
    }
}
