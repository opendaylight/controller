/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link EntryStore} based on {@link DataPersistenceProvider}.
 */
@NonNullByDefault
final class PersistenceEntryStore implements EntryStore {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceEntryStore.class);

    private final String logId;
    private final DataPersistenceProvider provider;

    PersistenceEntryStore(final String logId, final DataPersistenceProvider provider) {
        this.logId = requireNonNull(logId);
        this.provider = requireNonNull(provider);
    }

    @Override
    public void removeFrom(final long fromIndex) {
        final var de = new DeleteEntries(fromIndex);
        LOG.trace("{}: persisting in Pekko {}", logId, de);
        provider.persist(de, NoopProcedure.instance());
    }

    @Override
    public void persist(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        provider.persistAsync(entry, callback::accept);
    }

    @Override
    public void persistAndSync(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        provider.persist(entry, callback::accept);
    }

    @Override
    public void applyTo(final long toIndex) {
        LOG.debug("{}: Persisting ApplyJournalEntries with index={}", logId, toIndex);
        provider.persistAsync(new ApplyJournalEntries(toIndex), NoopProcedure.instance());
    }
}
