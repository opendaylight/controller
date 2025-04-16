/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmediateDataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.opendaylight.raft.spi.PlainSnapshotSource;

@VisibleForTesting
@NonNullByDefault
public final class TestDataProvider implements ImmediateDataPersistenceProvider {
    private ExecuteInSelfActor actor;

    public TestDataProvider() {
        this(Runnable::run);
    }

    public TestDataProvider(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }

    @Override
    public ExecuteInSelfActor actor() {
        return actor;
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        // no-op
    }

    @Override
    public <T extends StateSnapshot> void streamToInstall(final EntryInfo lastIncluded, final T snapshot,
            final StateSnapshot.Writer<T> writer, final RaftCallback<InstallableSnapshot> callback) {
        final byte[] bytes;
        try (var baos = new ByteArrayOutputStream()) {
            writer.writeSnapshot(snapshot, baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            actor.executeInSelf(() -> callback.invoke(e, null));
            return;
        }

        final var result = new InstallableSnapshotSource(lastIncluded, new PlainSnapshotSource(ByteArray.wrap(bytes)));
        actor.executeInSelf(() -> callback.invoke(null, result));
    }

    public void setActor(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }
}
