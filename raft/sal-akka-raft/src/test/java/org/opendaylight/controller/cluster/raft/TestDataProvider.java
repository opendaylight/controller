/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmediateDataPersistenceProvider;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.PlainSnapshotSource;
import org.opendaylight.raft.spi.SnapshotSource;

@NonNullByDefault
final class TestDataProvider implements ImmediateDataPersistenceProvider {
    private ExecuteInSelfActor actor;

    TestDataProvider() {
        this(Runnable::run);
    }

    TestDataProvider(final ExecuteInSelfActor actor) {
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
    public void streamToInstall(final WritableSnapshot snapshot,
            final BiConsumer<SnapshotSource, ? super Throwable> callback) {
        final byte[] bytes;
        try (var baos = new ByteArrayOutputStream()) {
            snapshot.writeTo(baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            actor.executeInSelf(() -> callback.accept(null, e));
            return;
        }
        actor.executeInSelf(() -> callback.accept(new PlainSnapshotSource(ByteArray.wrap(bytes)), null));
    }

    void setActor(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }
}
