/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.pekko.persistence.DeleteMessagesSuccess;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.raft.spi.SnapshotSource;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {
    private final RaftActor actor;

    PekkoRaftStorage(final RaftActor actor) {
        this.actor = requireNonNull(actor);
    }

    @Override
    protected String memberId() {
        return actor.memberId();
    }

    @Override
    protected void postStart() {
        // No-op
    }

    @Override
    protected void preStop() {
        // No-op
    }

    @Override
    public SnapshotSource tryLatestSnapshot() {
        // TODO: cache last encountered snapshot along with its lifecycle
        return null;
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        actor.persist(entry, callback::accept);
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        actor.persistAsync(entry, callback::accept);
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        actor.saveSnapshot(snapshot);
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot,
            final BiConsumer<@Nullable SnapshotSource, @Nullable ? super Throwable> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        actor.deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        actor.deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return actor.lastSequenceNr();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return response instanceof DeleteMessagesSuccess;
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
