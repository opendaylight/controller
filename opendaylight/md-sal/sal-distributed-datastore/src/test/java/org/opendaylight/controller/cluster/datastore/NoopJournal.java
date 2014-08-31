/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import scala.concurrent.Future;
import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.SyncWriteJournal;

public class NoopJournal extends SyncWriteJournal {

    @Override
    public void doWriteMessages(Iterable<PersistentRepr> messages) {
    }

    @Override
    public void doWriteConfirmations(Iterable<PersistentConfirmation> confirmations) {
    }

    @Override
    public void doDeleteMessages(Iterable<PersistentId> messageIds, boolean permanent) {
    }

    @Override
    public void doDeleteMessagesTo(String persistenceId, long toSequenceNr, boolean permanent) {
    }

    @Override
    public Future<Void> doAsyncReplayMessages(String persistenceId, long fromSequenceNr,
            long toSequenceNr, long max, Procedure<PersistentRepr> replayCallback) {
        return Futures.successful(null);
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(String persistenceId, long fromSequenceNr) {
        return Futures.successful(0L);
    }
}