/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

@NonNullByDefault
public abstract class ForwardingEntryStore implements EntryStore {

    protected abstract EntryStore delegate();

    @Override
    public EntryLoader openLoader() {
        return delegate().openLoader();
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry) throws IOException {
        delegate().persistEntry(entry);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        delegate().startPersistEntry(entry, callback);
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        delegate().deleteEntries(fromIndex);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) throws IOException {
        delegate().deleteMessages(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return delegate().lastSequenceNumber();
    }

    @Override
    public void markLastApplied(final long lastAppliedIndex) {
        delegate().markLastApplied(lastAppliedIndex);
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return delegate().handleJournalResponse(response);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("delegate", delegate()).toString();
    }
}
