/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Marker interface for objects representing a snapshot of RAFT-replicated user state. A {@link StateSnapshot}
 * represents a logical sum of all {@link StateCommand}s applied to the journal at a particular point in time.
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
         * {@link Writer#writeSnapshot(StateSnapshot, OutputStream)}. The stream is guaranteed to contain at least one
         * byte.
         *
         * @param in the {@link InputStream} containing the serialized data
         * @return the converted snapshot State
         * @throws IOException if an I/O error occurs
         */
        T readSnapshot(InputStream in) throws IOException;
    }

    @FunctionalInterface
    interface Writer<T extends StateSnapshot> {
        /**
         * Serialize a snapshot into an {@link OutputStream}. Implementations are required to emit at least one byte.
         *
         * @param snapshot snapshot to serialize
         * @param out the {@link OutputStream}
         * @throws IOException if an I/O error occurs
         */
        void writeSnapshot(T snapshot, OutputStream out) throws IOException;
    }

    /**
     * A combination of a {@link Reader} and a {@link Writer} for a concrete {@link StateSnapshot} type, as indicated by
     * {@link #snapshotType()}.
     *
     * @param <T> the {@link StateMachineCommand} type
     */
    interface Support<T extends StateSnapshot> {
        /**
         * Returns the {@link StateSnapshot} type supported by this {@link Support}.
         *
         * @return the {@link StateSnapshot} type supported by this {@link Support}
         */
        Class<T> snapshotType();

        /**
         * Returns the {@link Reader}.
         *
         * @return the reader
         */
        Reader<T> reader();

        /**
         * Returns the {@link Writer}.
         *
         * @return the writer
         */
        Writer<T> writer();
    }
}
