/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.RestrictedObjectStreams;

/**
 * Access to the contents of a RAFT snapshot file.
 */
@NonNullByDefault
public interface SnapshotFile extends InstallableSnapshot {
    /**
     * Returns the instant this file was written.
     *
     * @return the instant this file was written
     */
    Instant timestamp();

    /**
     * {@return this file's {@link Path}}
     */
    Path path();

    /**
     * Returns the {@link RaftSnapshot} stored in this file.
     *
     * @param objectStreams the {@link RestrictedObjectStreams} context
     * @return the {@link RaftSnapshot}
     * @throws IOException if an I/O error occurs
     */
    @Beta
    // FIXME: note: we need data dictionary to interpret ByteStream to Payload for ReplicatedLogEntry.getData()
    RaftSnapshot readRaftSnapshot(RestrictedObjectStreams objectStreams) throws IOException;

    default <T extends StateSnapshot> @Nullable T readSnapshot(final StateSnapshot.Reader<? extends T> reader)
            throws IOException {
        final var source = source();
        return source == null ? null : reader.readSnapshot(source.toPlainSource().io().openBufferedStream());
    }
}
