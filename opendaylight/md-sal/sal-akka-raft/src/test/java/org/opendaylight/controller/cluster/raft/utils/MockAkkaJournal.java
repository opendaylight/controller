/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.utils;

import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentImpl;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.Callable;
import scala.concurrent.Future;

public class MockAkkaJournal extends AsyncWriteJournal {

    private static Map<Long, Object> journal = Maps.newLinkedHashMap();

    public static void addToJournal(long sequenceNr, Object message) {
        journal.put(sequenceNr, message);
    }

    public static void clearJournal() {
        journal.clear();
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, long fromSequenceNr,
        long toSequenceNr, long max, final Procedure<PersistentRepr> replayCallback) {

        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
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
    public Future<Long> doAsyncReadHighestSequenceNr(String s, long l) {
        return Futures.successful(new Long(0));
    }

    @Override
    public Future<Void> doAsyncWriteMessages(Iterable<PersistentRepr> persistentReprs) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncWriteConfirmations(Iterable<PersistentConfirmation> persistentConfirmations) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessages(Iterable<PersistentId> persistentIds, boolean b) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(String s, long l, boolean b) {
        return Futures.successful(null);
    }
}
