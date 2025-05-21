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
import java.util.function.LongConsumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * An immediate {@link EntryStore}. Offloads asynchronous persist responses via {@link ExecuteInSelfActor}
 * exposed via {@link #actor()}.
 */
@NonNullByDefault
public interface ImmediateEntryStore extends EntryStore {

    ExecuteInSelfActor actor();

    @Override
    default EntryLoader openLoader() {
        return EmptyEntryLoader.INSTANCE;
    }

    @Override
    default long persistEntry(final ReplicatedLogEntry entry) throws IOException {
        requireNonNull(entry);
        return 0;
    }

    @Override
    default void deleteEntries(final long fromIndex) {
        // No-op
    }

    @Override
    default void startPersistEntry(final ReplicatedLogEntry entry, final LongConsumer callback) {
        requireNonNull(entry);
        requireNonNull(callback);
        actor().executeInSelf(() -> callback.accept(0));
    }

    @Override
    default void markLastApplied(final long lastApplied) {
        // No-op
    }

    @Override
    default void deleteMessages(final long sequenceNumber) {
        // no-op
    }

    @Override
    default long lastSequenceNumber() {
        return -1;
    }

    @Override
    default boolean handleJournalResponse(final JournalProtocol.Response response) {
        return false;
    }
}
