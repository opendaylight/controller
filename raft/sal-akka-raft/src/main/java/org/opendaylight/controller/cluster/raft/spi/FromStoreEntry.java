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
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.spi.SizedStreamSource;

/**
 * A reflection of a {@link LogEntry} on its way from stable storage.
 *
 * @param journalIndex the index under which this entry is stored in {@link RaftJournal}
 */
// FIXME: this needs a bit more work:
//        - ByteBuf-backed IO implies refcounting
//        - readers (i.e. AppendEntries construction) require invalidation from writer (when we trim log), because:
//          - for files because the file may be reused
//          - for journal we want to free the backing ByteBuf as soon as possible
//          - we want to append different entries anyway -- perhaps even those which we have already transmitted
@NonNullByDefault
public record FromStoreEntry(long journalIndex, EntryInfo meta, SizedStreamSource io) implements StoreEntry {
    public FromStoreEntry {
        requireNonNull(meta);
        requireNonNull(io);
        if (journalIndex <= 0) {
            throw new IllegalArgumentException("Invalid journalIndex " + journalIndex);
        }
    }

    FromStoreEntry(final long journalIndex, final EntryMeta meta, final SizedStreamSource io) {
        this(journalIndex, EntryInfo.of(meta), io);
    }

    @Override
    public long size() {
        return io.size();
    }
}