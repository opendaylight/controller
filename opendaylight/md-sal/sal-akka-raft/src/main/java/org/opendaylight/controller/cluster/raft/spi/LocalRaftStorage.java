/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * An {@link EnabledRaftStorage} backed by a local directory.
 */
@NonNullByDefault
public final class LocalRaftStorage extends EnabledRaftStorage {
    private final String memberId;
    private final Path directory;

    public LocalRaftStorage(final String memberId, final Path directory) {
        this.memberId = requireNonNull(memberId);
        this.directory = requireNonNull(directory);
    }

    @Override
    protected String memberId() {
        return memberId;
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastSequenceNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return super.addToStringAtrributes(helper).add("directory", directory);
    }
}
