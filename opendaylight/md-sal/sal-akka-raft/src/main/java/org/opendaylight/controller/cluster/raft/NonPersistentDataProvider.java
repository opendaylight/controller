/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;

/**
 * A DataPersistenceProvider implementation with persistence disabled, essentially a no-op.
 */
@VisibleForTesting
abstract class NonPersistentDataProvider implements DataPersistenceProvider {
    private final ExecuteInSelfActor actor;

    NonPersistentDataProvider(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }

    @Override
    public final boolean isRecoveryApplicable() {
        return false;
    }

    @Override
    public final <T> void persist(final T entry, final Consumer<T> callback) {
        callback.accept(requireNonNull(entry));
    }

    @Override
    public final <T> void persistAsync(final T entry, final Consumer<T> callback) {
        requireNonNull(entry);
        requireNonNull(callback);
        actor.executeInSelf(() -> callback.accept(entry));
    }

    @Override
    public final void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        // no-op
    }

    @Override
    public final void deleteMessages(final long sequenceNumber) {
        // no-op
    }

    @Override
    public final long getLastSequenceNumber() {
        return -1;
    }

    @Override
    public final boolean handleJournalResponse(final JournalProtocol.Response response) {
        return false;
    }

    @Override
    public final boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return false;
    }
}
