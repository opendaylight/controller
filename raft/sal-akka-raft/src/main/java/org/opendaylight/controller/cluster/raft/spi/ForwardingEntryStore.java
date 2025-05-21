/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

@NonNullByDefault
public abstract class ForwardingEntryStore implements EntryStore {

    protected abstract EntryStore delegate();

    @Override
    public EntryStoreCompleter completer() {
        return delegate().completer();
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        delegate().persistEntry(entry, callback);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        delegate().startPersistEntry(entry, callback);
    }

    @Override
    public void discardTail(final long fromIndex) {
        delegate().discardTail(fromIndex);
    }

    @Override
    public void discardHead(final long sequenceNumber) {
        delegate().discardHead(sequenceNumber);
    }

    @Override
    public void markLastApplied(final long lastAppliedIndex) {
        delegate().markLastApplied(lastAppliedIndex);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("delegate", delegate()).toString();
    }
}
