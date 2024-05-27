/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.utils;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
 * An akka AsyncWriteJournal implementation that stores data in memory. This is intended for testing.
 *
 * @author Thomas Pantelis
 */
public class InMemoryJournal extends AsyncWriteJournal {

    private static class WriteMessagesComplete {
        final CountDownLatch latch;
        final Class<?> ofType;

        WriteMessagesComplete(final int count, final Class<?> ofType) {
            latch = new CountDownLatch(count);
            this.ofType = ofType;
        }
    }

    static final Logger LOG = LoggerFactory.getLogger(InMemoryJournal.class);

    private static final Map<String, Map<Long, Object>> JOURNALS = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> DELETE_MESSAGES_COMPLETE_LATCHES = new ConcurrentHashMap<>();

    private static final Map<String, WriteMessagesComplete> WRITE_MESSAGES_COMPLETE = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> BLOCK_READ_MESSAGES_LATCHES = new ConcurrentHashMap<>();

    private static Object deserialize(final Object data) {
        return data instanceof byte[] ? SerializationUtils.deserialize((byte[])data) : data;
    }

    public static void addEntry(final String persistenceId, final long sequenceNr, final Object data) {
        Map<Long, Object> journal = JOURNALS.computeIfAbsent(persistenceId, k -> new LinkedHashMap<>());

        synchronized (journal) {
            journal.put(sequenceNr, data instanceof Serializable
                    ? SerializationUtils.serialize((Serializable) data) : data);
        }
    }

    public static void clear() {
        JOURNALS.clear();
        DELETE_MESSAGES_COMPLETE_LATCHES.clear();
        WRITE_MESSAGES_COMPLETE.clear();
        BLOCK_READ_MESSAGES_LATCHES.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> get(final String persistenceId, final Class<T> type) {
        Map<Long, Object> journalMap = JOURNALS.get(persistenceId);
        if (journalMap == null) {
            return Collections.<T>emptyList();
        }

        synchronized (journalMap) {
            List<T> journal = new ArrayList<>(journalMap.size());
            for (Object entry: journalMap.values()) {
                Object data = deserialize(entry);
                if (type.isInstance(data)) {
                    journal.add((T) data);
                }
            }

            return journal;
        }
    }

    public static Map<Long, Object> get(final String persistenceId) {
        Map<Long, Object> journalMap = JOURNALS.get(persistenceId);
        return journalMap != null ? journalMap : Collections.<Long, Object>emptyMap();
    }

    public static void dumpJournal(final String persistenceId) {
        StringBuilder builder = new StringBuilder(String.format("Journal log for %s:", persistenceId));
        Map<Long, Object> journalMap = JOURNALS.get(persistenceId);
        if (journalMap != null) {
            synchronized (journalMap) {
                for (Map.Entry<Long, Object> e: journalMap.entrySet()) {
                    builder.append("\n    ").append(e.getKey()).append(" = ").append(deserialize(e.getValue()));
                }
            }
        }

        LOG.info(builder.toString());
    }

    public static void waitForDeleteMessagesComplete(final String persistenceId) {
        if (!Uninterruptibles.awaitUninterruptibly(DELETE_MESSAGES_COMPLETE_LATCHES.get(persistenceId),
                5, TimeUnit.SECONDS)) {
            throw new AssertionError("Delete messages did not complete");
        }
    }

    public static void waitForWriteMessagesComplete(final String persistenceId) {
        if (!Uninterruptibles.awaitUninterruptibly(WRITE_MESSAGES_COMPLETE.get(persistenceId).latch,
                5, TimeUnit.SECONDS)) {
            throw new AssertionError("Journal write messages did not complete");
        }
    }

    public static void addDeleteMessagesCompleteLatch(final String persistenceId) {
        DELETE_MESSAGES_COMPLETE_LATCHES.put(persistenceId, new CountDownLatch(1));
    }

    public static void addWriteMessagesCompleteLatch(final String persistenceId, final int count) {
        WRITE_MESSAGES_COMPLETE.put(persistenceId, new WriteMessagesComplete(count, null));
    }

    public static void addWriteMessagesCompleteLatch(final String persistenceId, final int count,
            final Class<?> ofType) {
        WRITE_MESSAGES_COMPLETE.put(persistenceId, new WriteMessagesComplete(count, ofType));
    }

    public static void addBlockReadMessagesLatch(final String persistenceId, final CountDownLatch latch) {
        BLOCK_READ_MESSAGES_LATCHES.put(persistenceId, latch);
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, final long fromSequenceNr,
            final long toSequenceNr, final long max, final Consumer<PersistentRepr> replayCallback) {
        LOG.trace("doAsyncReplayMessages for {}: fromSequenceNr: {}, toSequenceNr: {}", persistenceId,
                fromSequenceNr,toSequenceNr);
        return Futures.future(() -> {
            CountDownLatch blockLatch = BLOCK_READ_MESSAGES_LATCHES.remove(persistenceId);
            if (blockLatch != null) {
                Uninterruptibles.awaitUninterruptibly(blockLatch);
            }

            Map<Long, Object> journal = JOURNALS.get(persistenceId);
            if (journal == null) {
                return null;
            }

            synchronized (journal) {
                int count = 0;
                for (Map.Entry<Long,Object> entry : journal.entrySet()) {
                    if (++count <= max && entry.getKey() >= fromSequenceNr && entry.getKey() <= toSequenceNr) {
                        PersistentRepr persistentMessage =
                                new PersistentImpl(deserialize(entry.getValue()), entry.getKey(), persistenceId,
                                        null, false, null, null, 0, Option.empty());
                        replayCallback.accept(persistentMessage);
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
        Map<Long, Object> journal = JOURNALS.get(persistenceId);
        if (journal == null) {
            return Futures.successful(fromSequenceNr);
        }

        synchronized (journal) {
            long highest = -1;
            for (Long seqNr : journal.keySet()) {
                if (seqNr.longValue() >= fromSequenceNr && seqNr.longValue() > highest) {
                    highest = seqNr.longValue();
                }
            }

            return Futures.successful(highest);
        }
    }

    @Override
    public Future<Iterable<Optional<Exception>>> doAsyncWriteMessages(final Iterable<AtomicWrite> messages) {
        return Futures.future(() -> {
            for (AtomicWrite write : messages) {
                for (PersistentRepr repr : CollectionConverters.asJava(write.payload())) {
                    LOG.trace("doAsyncWriteMessages: id: {}: seqNr: {}, payload: {}", repr.persistenceId(),
                        repr.sequenceNr(), repr.payload());

                    addEntry(repr.persistenceId(), repr.sequenceNr(), repr.payload());

                    WriteMessagesComplete complete = WRITE_MESSAGES_COMPLETE.get(repr.persistenceId());
                    if (complete != null) {
                        if (complete.ofType == null || complete.ofType.equals(repr.payload().getClass())) {
                            complete.latch.countDown();
                        }
                    }
                }
            }

            return Collections.emptyList();
        }, context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(final String persistenceId, final long toSequenceNr) {
        LOG.trace("doAsyncDeleteMessagesTo: {}", toSequenceNr);
        Map<Long, Object> journal = JOURNALS.get(persistenceId);
        if (journal != null) {
            synchronized (journal) {
                journal.keySet().removeIf(num -> num <= toSequenceNr);
            }
        }

        CountDownLatch latch = DELETE_MESSAGES_COMPLETE_LATCHES.get(persistenceId);
        if (latch != null) {
            latch.countDown();
        }

        return Futures.successful(null);
    }
}
