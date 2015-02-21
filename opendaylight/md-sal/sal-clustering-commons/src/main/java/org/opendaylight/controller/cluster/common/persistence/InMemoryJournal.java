/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.persistence;

import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentImpl;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import scala.concurrent.Future;

/**
 * An akka AsyncWriteJournal implementation that stores data in memory. This is intended for testing.
 *
 * @author Thomas Pantelis
 */
public class InMemoryJournal extends AsyncWriteJournal {

    private static final Map<String, Map<Long, Object>> journals = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> deleteMessagesCompleteLatches = new ConcurrentHashMap<>();

    private static final Map<String, CountDownLatch> blockReadMessagesLatches = new ConcurrentHashMap<>();

    public static void addEntry(String persistenceId, long sequenceNr, Object data) {
        Map<Long, Object> journal = journals.get(persistenceId);
        if(journal == null) {
            journal = Maps.newLinkedHashMap();
            journals.put(persistenceId, journal);
        }

        synchronized (journal) {
            journal.put(sequenceNr, data);
        }
    }

    public static void clear() {
        journals.clear();
    }

    public static Map<Long, Object> get(String persistenceId) {
        Map<Long, Object> journal = journals.get(persistenceId);
        return journal != null ? journal : Collections.<Long, Object>emptyMap();
    }

    public static void waitForDeleteMessagesComplete(String persistenceId) {
        if(!Uninterruptibles.awaitUninterruptibly(deleteMessagesCompleteLatches.get(persistenceId), 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Delete messages did not complete");
        }
    }

    public static void addDeleteMessagesCompleteLatch(String persistenceId) {
        deleteMessagesCompleteLatches.put(persistenceId, new CountDownLatch(1));
    }

    public static void addBlockReadMessagesLatch(String persistenceId, CountDownLatch latch) {
        blockReadMessagesLatches.put(persistenceId, latch);
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, long fromSequenceNr,
            long toSequenceNr, long max, final Procedure<PersistentRepr> replayCallback) {
        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                CountDownLatch blockLatch = blockReadMessagesLatches.remove(persistenceId);
                if(blockLatch != null) {
                    Uninterruptibles.awaitUninterruptibly(blockLatch);
                }

                Map<Long, Object> journal = journals.get(persistenceId);
                if(journal == null) {
                    return null;
                }

                synchronized (journal) {
                    for (Map.Entry<Long,Object> entry : journal.entrySet()) {
                        PersistentRepr persistentMessage =
                                new PersistentImpl(entry.getValue(), entry.getKey(), persistenceId,
                                        false, null, null);
                        replayCallback.apply(persistentMessage);
                    }
                }

                return null;
            }
        }, context().dispatcher());
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(String persistenceId, long fromSequenceNr) {
        return Futures.successful(-1L);
    }

    @Override
    public Future<Void> doAsyncWriteMessages(final Iterable<PersistentRepr> messages) {
        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (PersistentRepr repr : messages) {
                    Map<Long, Object> journal = journals.get(repr.persistenceId());
                    if(journal == null) {
                        journal = Maps.newLinkedHashMap();
                        journals.put(repr.persistenceId(), journal);
                    }

                    synchronized (journal) {
                        journal.put(repr.sequenceNr(), repr.payload());
                    }
                }
                return null;
            }
        }, context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncWriteConfirmations(Iterable<PersistentConfirmation> confirmations) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessages(Iterable<PersistentId> messageIds, boolean permanent) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(String persistenceId, long toSequenceNr, boolean permanent) {
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
