/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;

/**
 * An immediate {@link DataPersistenceProvider}. Offloads asynchronous persist responses via {@link ExecuteInSelfActor}
 * exposed by {@link #actor()}.
 */
@NonNullByDefault
public interface ImmediateDataPersistenceProvider extends DataPersistenceProvider {

    ExecuteInSelfActor actor();

    @Override
    default boolean isRecoveryApplicable() {
        return false;
    }

    @Override
    default @Nullable SnapshotFile lastSnapshot() throws IOException {
        return null;
    }

    @Override
    default <T> void persist(final T entry, final Consumer<T> callback) {
        callback.accept(requireNonNull(entry));
    }

    @Override
    default void deleteEntries(final long fromIndex) {
        // No-op
    }

    @Override
    default <T> void persistAsync(final T entry, final Consumer<T> callback) {
        requireNonNull(entry);
        requireNonNull(callback);
        actor().executeInSelf(() -> callback.accept(entry));
    }

    @Override
    default void deleteSnapshots(final long maxTimestamp) {
        // no-op
    }

    @Override
    default void deleteMessages(final long sequenceNumber) {
        // no-op
    }

    @Override
    default long getLastSequenceNumber() {
        return -1;
    }

    @Override
    default boolean handleJournalResponse(final JournalProtocol.Response response) {
        return false;
    }

    @Override
    default boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return false;
    }
}
