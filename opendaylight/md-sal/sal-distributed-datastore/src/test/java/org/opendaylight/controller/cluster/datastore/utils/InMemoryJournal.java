/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.collect.Maps;
import scala.concurrent.Future;
import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentImpl;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;

public class InMemoryJournal extends AsyncWriteJournal {

    private static Map<String, Map<Long, Object>> journals = new ConcurrentHashMap<>();

    public static void addEntry(String persistenceId, long sequenceNr, Object data) {
        Map<Long, Object> journal = journals.get(persistenceId);
        if(journal == null) {
            journal = Maps.newLinkedHashMap();
            journals.put(persistenceId, journal);
        }

        journal.put(sequenceNr, data);
    }

    public static void clear() {
        journals.clear();
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, long fromSequenceNr,
            long toSequenceNr, long max, final Procedure<PersistentRepr> replayCallback) {
        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Map<Long, Object> journal = journals.get(persistenceId);
                if(journal == null) {
                    return null;
                }

                for (Map.Entry<Long,Object> entry : journal.entrySet()) {
                    PersistentRepr persistentMessage =
                        new PersistentImpl(entry.getValue(), entry.getKey(), persistenceId, false, null, null);
                    replayCallback.apply(persistentMessage);
                }

                return null;
            }
        }, context().dispatcher());
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(String persistenceId, long fromSequenceNr) {
        return Futures.successful(new Long(0));
    }

    @Override
    public Future<Void> doAsyncWriteMessages(Iterable<PersistentRepr> messages) {
        return Futures.successful(null);
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
        return Futures.successful(null);
    }
}
