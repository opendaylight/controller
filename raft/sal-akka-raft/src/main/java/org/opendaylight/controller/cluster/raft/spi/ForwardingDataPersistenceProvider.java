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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.spi.SnapshotSource;

@NonNullByDefault
public abstract class ForwardingDataPersistenceProvider implements DataPersistenceProvider {

    protected abstract DataPersistenceProvider delegate();

    @Override
    public boolean isRecoveryApplicable() {
        return delegate().isRecoveryApplicable();
    }

    @Override
    public @Nullable SnapshotSource tryLatestSnapshot() throws IOException {
        return delegate().tryLatestSnapshot();
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        delegate().persist(entry, callback);
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        delegate().persistAsync(entry, callback);
    }

    @Override
    public void saveSnapshot(final Snapshot entry) {
        delegate().saveSnapshot(entry);
    }

    @Override
    public void saveSnapshotForInstall(final WritableSnapshot snapshot,
            final BiConsumer<SnapshotSource, ? super Throwable> callback) {
        delegate().saveSnapshotForInstall(snapshot, callback);
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        delegate().deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        delegate().deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return delegate().getLastSequenceNumber();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return delegate().handleJournalResponse(response);
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return delegate().handleSnapshotResponse(response);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("delegate", delegate()).toString();
    }
}
