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
import io.atomix.utils.serializer.Namespace;
import java.io.File;
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

    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);
    private static final int FILE_ENTRY_SIZE = 128 * 1024;

    private final SegmentedJournal<DataJournalEntry> entries;
    private final FragmentJournal fragments;

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
        fragments = new FragmentJournal(storage, directory);
    }

    @Override
    long lastWrittenSequenceNr() {
        return entries.writer().getLastIndex() - fragments.tailDelta();
    }

    @Override
    void deleteTo(final long sequenceNr) {
        // TODO Auto-generated method stub

    }

    @Override
    void compactTo(final long sequenceNr) {
        // TODO Auto-generated method stub

    }

    @Override
    void close() {
        entries.close();
    }

    @Override
    void handleReplayMessages(final ReplayMessages message, final long fromSequenceNr) {
        // TODO Auto-generated method stub

    }

    @Override
    void handleWriteMessages(final WriteMessages message) {
        // TODO Auto-generated method stub

    }
}
