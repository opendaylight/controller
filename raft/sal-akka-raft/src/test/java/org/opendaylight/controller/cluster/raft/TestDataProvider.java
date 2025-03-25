/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmediateDataPersistenceProvider;
import org.opendaylight.raft.spi.PlainSnapshotSource;
import org.opendaylight.raft.spi.SnapshotSource;

final class TestDataProvider implements ImmediateDataPersistenceProvider {
    static final TestDataProvider INSTANCE = new TestDataProvider();

    private TestDataProvider() {
        // Hidden in purpose
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        // no-op
    }

    @Override
    public void saveSnapshotForInstall(final Snapshot snapshot, final WriteTo writeTo,
            final BiConsumer<SnapshotSource, ? super Throwable> callback) {
        final byte[] bytes;
        try (var baos = new ByteArrayOutputStream()) {
            writeTo.writeTo(baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            callback.accept(null, e);
            return;
        }
        callback.accept((PlainSnapshotSource) () -> new ByteArrayInputStream(bytes), null);
    }

    @Override
    public void executeInSelf(final Runnable runnable) {
        runnable.run();
    }
}
