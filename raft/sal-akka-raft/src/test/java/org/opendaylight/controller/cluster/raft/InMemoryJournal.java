/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.persistence.AtomicWrite;
import org.apache.pekko.persistence.PersistentImpl;
import org.apache.pekko.persistence.PersistentRepr;
import org.apache.pekko.persistence.journal.japi.AsyncWriteJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.concurrent.Future;
import scala.jdk.javaapi.CollectionConverters;

/**
 * An {@link AsyncWriteJournal} implementation that stores data in memory. This is intended for testing.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "11.0.0", forRemoval = true)
public class InMemoryJournal extends AsyncWriteJournal {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryJournal.class);
    private static final ConcurrentHashMap<String, Map<Long, Object>> JOURNALS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CountDownLatch> DELETE_MESSAGES_COMPLETE_LATCHES =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CountDownLatch> BLOCK_READ_MESSAGES_LATCHES =
        new ConcurrentHashMap<>();

    private static Object deserialize(final Object data) {
        return data instanceof byte[] bytes ? SerializationUtils.deserialize(bytes) : data;
    }

    public static void addEntry(final String persistenceId, final long sequenceNr, final Object data) {
        final var journal = JOURNALS.computeIfAbsent(persistenceId, k -> new LinkedHashMap<>());

        synchronized (journal) {
            journal.put(sequenceNr,
                data instanceof Serializable serializable ? SerializationUtils.serialize(serializable) : data);
        }
    }

    public static void clear() {
        JOURNALS.clear();
        DELETE_MESSAGES_COMPLETE_LATCHES.clear();
        BLOCK_READ_MESSAGES_LATCHES.clear();
    }

    public static <T> List<T> get(final String persistenceId, final Class<T> type) {
        final var journalMap = JOURNALS.get(persistenceId);
        if (journalMap == null) {
            return List.of();
        }

        synchronized (journalMap) {
            final var journal = new ArrayList<T>(journalMap.size());
            for (var entry : journalMap.values()) {
                final var data = deserialize(entry);
                if (type.isInstance(data)) {
                    journal.add(type.cast(data));
                }
            }

            return journal;
        }
    }

    public static Map<Long, Object> get(final String persistenceId) {
        final var journalMap = JOURNALS.get(persistenceId);
        return journalMap != null ? journalMap : Map.of();
    }

    public static void dumpJournal(final String persistenceId) {
        final var sb = new StringBuilder(String.format("Journal log for %s:", persistenceId));
        final var journalMap = JOURNALS.get(persistenceId);
        if (journalMap != null) {
            synchronized (journalMap) {
                for (final var entry: journalMap.entrySet()) {
                    sb.append("\n    ").append(entry.getKey()).append(" = ").append(deserialize(entry.getValue()));
                }
            }
        }
        LOG.info(sb.toString());
    }

    public static void waitForDeleteMessagesComplete(final String persistenceId) {
        if (!Uninterruptibles.awaitUninterruptibly(DELETE_MESSAGES_COMPLETE_LATCHES.get(persistenceId),
                5, TimeUnit.SECONDS)) {
            throw new AssertionError("Delete messages did not complete");
        }
    }

    public static void addDeleteMessagesCompleteLatch(final String persistenceId) {
        DELETE_MESSAGES_COMPLETE_LATCHES.put(persistenceId, new CountDownLatch(1));
    }

    public static void addBlockReadMessagesLatch(final String persistenceId, final CountDownLatch latch) {
        BLOCK_READ_MESSAGES_LATCHES.put(persistenceId, latch);
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, final long fromSequenceNr,
            final long toSequenceNr, final long max, final Consumer<PersistentRepr> replayCallback) {
        LOG.trace("doAsyncReplayMessages for {}: fromSequenceNr: {}, toSequenceNr: {}", persistenceId, fromSequenceNr,
            toSequenceNr);
        return Futures.future(() -> {
            final var blockLatch = BLOCK_READ_MESSAGES_LATCHES.remove(persistenceId);
            if (blockLatch != null) {
                Uninterruptibles.awaitUninterruptibly(blockLatch);
            }

            final var journal = JOURNALS.get(persistenceId);
            if (journal == null) {
                return null;
            }

            synchronized (journal) {
                int count = 0;
                for (var entry : journal.entrySet()) {
                    final long key = entry.getKey();
                    if (++count <= max && key >= fromSequenceNr && key <= toSequenceNr) {
                        replayCallback.accept(new PersistentImpl(deserialize(entry.getValue()), key, persistenceId,
                            null, false, null, null, 0, Option.empty()));
                    }
                }
            }

            return null;
        }, context().dispatcher());
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(final String persistenceId, final long fromSequenceNr) {
        LOG.trace("doAsyncReadHighestSequenceNr for {}: fromSequenceNr: {}", persistenceId, fromSequenceNr);

        // Pekko calls this during recovery.
        final var journal = JOURNALS.get(persistenceId);
        if (journal == null) {
            return Futures.successful(fromSequenceNr);
        }

        synchronized (journal) {
            long highest = -1;
            for (var seqNr : journal.keySet()) {
                if (seqNr.longValue() >= fromSequenceNr && seqNr.longValue() > highest) {
                    highest = seqNr;
                }
            }
            return Futures.successful(highest);
        }
    }

    @Override
    public Future<Iterable<Optional<Exception>>> doAsyncWriteMessages(final Iterable<AtomicWrite> messages) {
        return Futures.future(() -> {
            for (var write : messages) {
                for (var repr : CollectionConverters.asJava(write.payload())) {
                    LOG.trace("doAsyncWriteMessages: id: {}: seqNr: {}, payload: {}", repr.persistenceId(),
                        repr.sequenceNr(), repr.payload());

                    addEntry(repr.persistenceId(), repr.sequenceNr(), repr.payload());
                }
            }

            return List.of();
        }, context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(final String persistenceId, final long toSequenceNr) {
        LOG.trace("doAsyncDeleteMessagesTo: {}", toSequenceNr);
        final var journal = JOURNALS.get(persistenceId);
        if (journal != null) {
            synchronized (journal) {
                journal.keySet().removeIf(num -> num <= toSequenceNr);
            }
        }

        final var latch = DELETE_MESSAGES_COMPLETE_LATCHES.get(persistenceId);
        if (latch != null) {
            latch.countDown();
        }

        return Futures.successful(null);
    }
}
