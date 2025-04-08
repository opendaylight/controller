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
import org.opendaylight.raft.spi.TransientFile;

/**
 * A reflection of a {@link LogEntry} on its way to stable storage.
 */
@NonNullByDefault
sealed interface ToStoreEntry extends StoreEntry {
    /**
     * An entry stored in a standalone file.
     */
    // FIXME: The idea is that we collect StoredEntry instances during writeout and sync(2) them accordingly:
    //        - ToFile means the file is temporary file, which needs to be sync()ed and moved into place before its
    //          corresponding journal is sync()ed.
    //        - ToJournal means no further sync
    record ToFile(EntryInfo meta, long size, TransientFile file) implements ToStoreEntry {
        public ToFile {
            requireNonNull(file);
            requireNonNull(meta);
            if (size <= MIN_SIZE) {
                throw new IllegalArgumentException("Invalid file size " + size);
            }
        }

        ToFile(final EntryMeta meta, final long size, final TransientFile file) {
            this(EntryInfo.of(meta), size, file);
        }
    }

    /**
     * An entry stored in {@link RaftJournal}.
     */
    record ToJournal(EntryInfo meta, long size) implements ToStoreEntry {
        public ToJournal {
            requireNonNull(meta);
            if (size <= MIN_SIZE) {
                throw new IllegalArgumentException("Invalid journal size " + size);
            }
        }

        ToJournal(final EntryMeta meta, final long size) {
            this(EntryInfo.of(meta), size);
        }
    }
}