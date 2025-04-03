/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.spi.StreamSource;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Marker interface for objects representing a snapshot of RAFT-replicated user state. A {@link StateSnapshot}
 * represents a logical sum of all {@link StateDelta}s applied to the journal at a particular point in time.
 */
@NonNullByDefault
public interface StateSnapshot extends Immutable {
    /**
     * Indicate whether the snapshot requires migration, i.e. a new snapshot should be created after recovery. Default
     * implementation returns {@code false}, i.e. do not re-snapshot.
     *
     * @return {@code true} if complete recovery based upon this snapshot should trigger a new snapshot.
     */
    default boolean needsMigration() {
        return false;
    }

    @FunctionalInterface
    interface Reader<T extends StateSnapshot> {
        /**
         * This method is called to de-serialize snapshot data that was previously serialized via
         * {@link Writer#writeSnapshot(StateSnapshot, OutputStream)}.
         *
         * @param source the {@link StreamSource} containing the serialized data
         * @return the converted snapshot State
         * @throws IOException if an error occurs accessing the ByteSource or de-serializing
         */
        // TODO: just DataInput once we have dealt with LZ4 decompression retried open
        T readSnapshot(StreamSource source) throws IOException;
    }

    @FunctionalInterface
    interface Writer<T extends StateSnapshot> {
        /**
         * Serialize a snapshot into an {@link OutputStream}.
         *
         * @param snapshot snapshot to serialize
         * @param out the {@link OutputStream}
         * @throws IOException if an I/O error occurs
         */
        void writeSnapshot(T snapshot, OutputStream out) throws IOException;
    }
}
