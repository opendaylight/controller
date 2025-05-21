/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * An iterator-like access to traverse entries stored in an {@link EntryStore}.
 */
@NonNullByDefault
public interface EntryLoader extends AutoCloseable {
    /**
     * An entry loaded from an {@link EntryStore}.
     */
    sealed interface LoadedEntry {

        long journalIndex();
    }

    /**
     * An update to {@code lastApplied} index loaded from an {@link EntryStore}.
     */
    record LoadedLastApplied(long journalIndex, long lastApplied) implements LoadedEntry {
        // Nothing else
    }

    /**
     * A {@link LogEntry} loaded from an {@link EntryStore}.
     */
    record LoadedLogEntry(long journalIndex, long index, long term, Payload command) implements LoadedEntry, LogEntry {
        public LoadedLogEntry {
            requireNonNull(command);
        }
    }

    @Nullable LoadedEntry loadNext();

    @Override
    void close();
}