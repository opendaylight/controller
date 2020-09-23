/*
 * Copyright (c) 2019, 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.JournalSegment;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;

/**
 * Version 0 data journal, where every journal entry maps to exactly one segmented file entry.
 *
 * @author Robert Varga
 */
final class DataJournalV0 extends DataJournal {
    /**
     * A single entry in the data journal. We do not store {@code persistenceId} for each entry, as that is a
     * journal-invariant, nor do we store {@code sequenceNr}, as that information is maintained by
     * {@link JournalSegment}'s index.
     */
    private abstract static class Entry {

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

    /**
     * Kryo serializer for {@link Entry}. Each {@link SegmentedJournalActor} has its own instance, as well as
     * a nested JavaSerializer to handle the payload.
     *
     * <p>
     * Since we are persisting only parts of {@link PersistentRepr}, this class asymmetric by design:
     * {@link #write(Kryo, Output, Entry)} only accepts {@link ToPersistence} subclass, which is a wrapper around
     * a {@link PersistentRepr}, while {@link #read(Kryo, Input, Class)} produces an {@link FromPersistence}, which
     * needs further processing to reconstruct a {@link PersistentRepr}.
     */
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

    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);

    private final SegmentedJournal<Entry> dataJournal;

    DataJournalV0(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final File directory, final int maxEntrySize, final int maxSegmentSize) {
        super(persistenceId, messageSize);
        dataJournal = SegmentedJournal.<Entry>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("data")
                .withNamespace(Namespace.builder()
                    .register(new EntrySerializer(system), FromPersistence.class, ToPersistence.class)
                    .build())
                .withMaxEntrySize(maxEntrySize).withMaxSegmentSize(maxSegmentSize)
                .build();
    }

    @Override
    long lastWrittenIndex() {
        return dataJournal.writer().getLastIndex();
    }

    @Override
    void commitTo(final long index) {
        dataJournal.writer().commit(index);
    }

    @Override
    void compactTo(final long index) {
        dataJournal.compact(index);
    }

    @Override
    void close() {
        dataJournal.close();
    }

    @Override
    void handleReplayMessages(final ReplayMessages message, final long from) {
        try (SegmentedJournalReader<Entry> reader = dataJournal.openReader(from)) {
            int count = 0;
            while (reader.hasNext() && count < message.max) {
                final Indexed<Entry> next = reader.next();
                if (next.index() > message.toSequenceNr) {
                    break;
                }

                LOG.trace("{}: replay {}", persistenceId, next);
                updateLargestSize(next.size());
                final Entry entry = next.entry();
                verify(entry instanceof FromPersistence, "Unexpected entry %s", entry);

                final PersistentRepr repr = ((FromPersistence) entry).toRepr(persistenceId, next.index());
                LOG.debug("{}: replaying {}", persistenceId, repr);
                message.replayCallback.accept(repr);
                count++;
            }
            LOG.debug("{}: successfully replayed {} entries", persistenceId, count);
        } catch (Exception e) {
            LOG.warn("{}: failed to replay messages for {}", persistenceId, message, e);
            message.promise.failure(e);
        } finally {
            message.promise.success(null);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    void handleWriteMessages(final WriteMessages message) {
        final int count = message.size();
        final SegmentedJournalWriter<Entry> writer = dataJournal.writer();

        for (int i = 0; i < count; ++i) {
            final long mark = writer.getLastIndex();
            final AtomicWrite request = message.getRequest(i);
            try {
                for (PersistentRepr repr : CollectionConverters.asJava(request.payload())) {
                    final Object payload = repr.payload();
                    if (!(payload instanceof Serializable)) {
                        throw new UnsupportedOperationException("Non-serializable payload encountered "
                                + payload.getClass());
                    }

                    recordMessageSize(writer.append(new ToPersistence(repr)).size());
                }
            } catch (Exception e) {
                LOG.warn("{}: failed to write out request", persistenceId, e);
                message.setFailure(i, e);
                writer.truncate(mark);
                continue;
            }

            message.setSuccess(i);
        }
        writer.flush();
    }
}
