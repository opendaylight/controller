/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.api.TermInfo;

/**
 * All supported snapshot file formats. Content closely mirrors {@link Snapshot}, but does not include {@link TermInfo}
 */
@NonNullByDefault
public enum SnapshotFileFormat {
    /**
     * First version of snapshot file format.
     */
    SNAPSHOT_FILE_V1 {
        @Override
        public SnapshotFile open(final Path file) throws IOException {
            return SnapshotFileV1.open(file);
        }
    };

    public static SnapshotFileFormat latest() {
        return SNAPSHOT_FILE_V1;
    }

    public static @Nullable SnapshotFileFormat forMagicBits(final int magicBits) {
        return switch (magicBits) {
            case 0xE34C80B7 -> SNAPSHOT_FILE_V1;
            default -> null;
        };
    }

    public static SnapshotFileFormat ofMagicBits(final int magicBits) throws IOException {
        final var ret = forMagicBits(magicBits);
        if (ret == null) {
            throw new IOException("Unrecognized magic " + Integer.toHexString(magicBits));
        }
        return ret;
    }

    public abstract SnapshotFile open(Path file) throws IOException;
}
