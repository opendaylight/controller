/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persistence;

import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import com.google.common.annotations.VisibleForTesting;
import scala.concurrent.Future;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The NonPersistentJournal does not actually store any journal entries - not even in memory. It does keep track of the
 * count of entries so that it can let akka persistence answer questions like which is the highest sequence number in
 * a journal etc.
 */
public class NonPersistentJournal  extends AsyncWriteJournal {

    public static final Long EMPTY = new Long(-1L);
    private final Map<String, AtomicLong> journals = new ConcurrentHashMap<>();

    @Override
    public Future<Void> doAsyncReplayMessages(String persistenceId, long from, long to, long max,
                                              Procedure<PersistentRepr> persistentReprProcedure) {
        return Futures.successful(null);
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(String persistenceId, long from) {
        AtomicLong journal = journals.get(persistenceId);
        if (journal == null) {
            return Futures.successful(EMPTY);
        }
        return Futures.successful(journal.get());
    }

    @Override
    public Future<Void> doAsyncWriteMessages(final Iterable<PersistentRepr> persistentReprs) {
        return Futures.future(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (PersistentRepr repr : persistentReprs) {
                    String persistenceId = repr.persistenceId();
                    AtomicLong journal = journals.get(persistenceId);
                    if (journal == null) {
                        journal = new AtomicLong(-1L);
                        journals.put(persistenceId, journal);
                    }

                    journal.incrementAndGet();
                    }
                return null;
            }
        }, this.context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncWriteConfirmations(Iterable<PersistentConfirmation> persistentConfirmations) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessages(Iterable<PersistentId> persistentIds, boolean permanent) {
        return Futures.successful(null);
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(String persistenceId, long to, boolean permanent) {
        return Futures.successful(null);
    }

    @VisibleForTesting
    Map<String, AtomicLong> journals(){
        return this.journals;
    }
}
