/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.utils;

import akka.dispatch.Futures;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentImpl;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * An akka AsyncWriteJournal implementation that stores data in memory. This is intended for testing.
 *
 * @author Thomas Pantelis
 */
public class InMemoryJournal extends AsyncWriteJournal {

    private static class WriteMessagesComplete {
        final CountDownLatch latch;
        final Class<?> ofType;

        public WriteMessagesComplete(int count, Class<?> ofType) {
            this.latch = new CountDownLatch(count);
            this.ofType = ofType;
        }
    }

    static final Logger LOG = LoggerFactory.getLogger(InMemoryJournal.class);

    private static final Map<String, Map<Long, Object>> journals = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> deleteMessagesCompleteLatches = new ConcurrentHashMap<>();

    private static final Map<String, WriteMessagesComplete> writeMessagesComplete = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> blockReadMessagesLatches = new ConcurrentHashMap<>();

    private static Object deserialize(Object data) {
        return data instanceof byte[] ? SerializationUtils.deserialize((byte[])data) : data;
    }

    public static void addEntry(String persistenceId, long sequenceNr, Object data) {
        Map<Long, Object> journal = journals.get(persistenceId);
        if(journal == null) {
            journal = Maps.newLinkedHashMap();
            journals.put(persistenceId, journal);
        }

        synchronized (journal) {
            journal.put(sequenceNr, data instanceof Serializable ?
                    SerializationUtils.serialize((Serializable) data) : data);
        }
    }

    public static void clear() {
        journals.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> get(String persistenceId, Class<T> type) {
        Map<Long, Object> journalMap = journals.get(persistenceId);
        if(journalMap == null) {
            return Collections.<T>emptyList();
        }

        synchronized (journalMap) {
            List<T> journal = new ArrayList<>(journalMap.size());
            for(Object entry: journalMap.values()) {
                Object data = deserialize(entry);
                if(type.isInstance(data)) {
                    journal.add((T) data);
                }
            }

            return journal;
        }
    }

    public static Map<Long, Object> get(String persistenceId) {
        Map<Long, Object> journalMap = journals.get(persistenceId);
        return journalMap != null ? journalMap : Collections.<Long, Object>emptyMap();
    }

    public static void dumpJournal(String persistenceId) {
        StringBuilder builder = new StringBuilder(String.format("Journal log for %s:", persistenceId));
        Map<Long, Object> journalMap = journals.get(persistenceId);
        if(journalMap != null) {
            synchronized (journalMap) {
                for(Map.Entry<Long, Object> e: journalMap.entrySet()) {
                    builder.append("\n    ").append(e.getKey()).append(" = ").append(e.getValue());
                }
            }
        }

        LOG.info(builder.toString());
    }

    public static void waitForDeleteMessagesComplete(String persistenceId) {
        if(!Uninterruptibles.awaitUninterruptibly(deleteMessagesCompleteLatches.get(persistenceId), 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Delete messages did not complete");
        }
    }

    public static void waitForWriteMessagesComplete(String persistenceId) {
        if(!Uninterruptibles.awaitUninterruptibly(writeMessagesComplete.get(persistenceId).latch, 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Journal write messages did not complete");
        }
    }

    public static void addDeleteMessagesCompleteLatch(String persistenceId) {
        deleteMessagesCompleteLatches.put(persistenceId, new CountDownLatch(1));
    }

    public static void addWriteMessagesCompleteLatch(String persistenceId, int count) {
        writeMessagesComplete.put(persistenceId, new WriteMessagesComplete(count, null));
    }

    public static void addWriteMessagesCompleteLatch(String persistenceId, int count, Class<?> ofType) {
        writeMessagesComplete.put(persistenceId, new WriteMessagesComplete(count, ofType));
    }

    public static void addBlockReadMessagesLatch(String persistenceId, CountDownLatch latch) {
        blockReadMessagesLatches.put(persistenceId, latch);
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, final long fromSequenceNr,
            final long toSequenceNr, final long max, final Consumer<PersistentRepr> replayCallback) {
        LOG.trace("doAsyncReplayMessages for {}: fromSequenceNr: {}, toSequenceNr: {}", persistenceId,
                fromSequenceNr,toSequenceNr);
        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                CountDownLatch blockLatch = blockReadMessagesLatches.remove(persistenceId);
                if(blockLatch != null) {
                    Uninterruptibles.awaitUninterruptibly(blockLatch);
                }

                Map<Long, Object> journal = journals.get(persistenceId);
                if (journal == null) {
                    return null;
                }

                synchronized (journal) {
                    int count = 0;
                    for (Map.Entry<Long,Object> entry : journal.entrySet()) {
                        if (++count <= max && entry.getKey() >= fromSequenceNr && entry.getKey() <= toSequenceNr) {
                            PersistentRepr persistentMessage =
                                    new PersistentImpl(deserialize(entry.getValue()), entry.getKey(), persistenceId,
                                            null, false, null, null);
                            replayCallback.accept(persistentMessage);
                        }
                    }
                }

                return null;
            }
        }, context().dispatcher());
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(String persistenceId, long fromSequenceNr) {
        LOG.trace("doAsyncReadHighestSequenceNr for {}: fromSequenceNr: {}", persistenceId, fromSequenceNr);

        // Akka calls this during recovery.
        Map<Long, Object> journal = journals.get(persistenceId);
        if(journal == null) {
            return Futures.successful(fromSequenceNr);
        }

        synchronized (journal) {
            long highest = -1;
            for (Long seqNr : journal.keySet()) {
                if(seqNr.longValue() >= fromSequenceNr && seqNr.longValue() > highest) {
                    highest = seqNr.longValue();
                }
            }

            return Futures.successful(highest);
        }
    }

    @Override
    public Future<Iterable<Optional<Exception>>> doAsyncWriteMessages(final Iterable<AtomicWrite> messages) {
        return Futures.future(new Callable<Iterable<Optional<Exception>>>() {
            @Override
            public Iterable<Optional<Exception>> call() throws Exception {
                for (AtomicWrite write : messages) {
                    // Copy to array - workaround for eclipse "ambiguous method" errors for toIterator, toIterable etc
                    PersistentRepr[] array = new PersistentRepr[write.payload().size()];
                    write.payload().copyToArray(array);
                    for(PersistentRepr repr: array) {
                        LOG.trace("doAsyncWriteMessages: id: {}: seqNr: {}, payload: {}", repr.persistenceId(),
                            repr.sequenceNr(), repr.payload());

                        addEntry(repr.persistenceId(), repr.sequenceNr(), repr.payload());

                        WriteMessagesComplete complete = writeMessagesComplete.get(repr.persistenceId());
                        if(complete != null) {
                            if(complete.ofType == null || complete.ofType.equals(repr.payload().getClass())) {
                                complete.latch.countDown();
                            }
                        }
                    }
                }

                return Collections.emptyList();
            }
        }, context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(String persistenceId, long toSequenceNr) {
        LOG.trace("doAsyncDeleteMessagesTo: {}", toSequenceNr);
        Map<Long, Object> journal = journals.get(persistenceId);
        if(journal != null) {
            synchronized (journal) {
                Iterator<Long> iter = journal.keySet().iterator();
                while(iter.hasNext()) {
                    Long n = iter.next();
                    if(n <= toSequenceNr) {
                        iter.remove();
                    }
                }
            }
        }

        CountDownLatch latch = deleteMessagesCompleteLatches.get(persistenceId);
        if(latch != null) {
            latch.countDown();
        }

        return Futures.successful(null);
    }
}
