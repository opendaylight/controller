/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import org.apache.pekko.persistence.DeleteMessagesSuccess;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryStoreCompleter;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
@NonNullByDefault
final class PekkoRaftStorage extends EnabledRaftStorage {
    private final RaftActor actor;

    PekkoRaftStorage(final EntryStoreCompleter completer, final RaftActor actor, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        super(completer, directory, compression, streamConfig);
        this.actor = requireNonNull(actor);
    }

    // TODO: at least
    //   - creates the directory if not present
    //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
    //   - scans the directory to:
    //     - clean up any temporary files
    //     - determine nextSequence

    @Override
    protected void postStart() {
        // No-op
    }

    // FIXME:  and more: more things:
    //   - terminates any incomplete operations, reporting a CancellationException to them
    //   - unlocks the file
    // - stop() that:
    // - a lava.lang.ref.Clearner which does the same as stop()
    // For scalability this locking should really be done on top-level stateDir so we have one file open for all shards,
    // not one file per shard.

    @Override
    protected void preStop() {
        // No-op
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final Runnable callback) {
        actor.persist(entry, callback);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final Runnable callback) {
        actor.persistAsync(entry, callback);
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        actor.deleteEntries(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        actor.markLastApplied(lastApplied);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        actor.deleteMessages(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return actor.lastSequenceNr();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return response instanceof DeleteMessagesSuccess;
    }
}
