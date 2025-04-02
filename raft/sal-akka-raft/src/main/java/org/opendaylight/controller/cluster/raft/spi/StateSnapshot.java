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
 * Marker interface for objects representing a snapshot of RAFT-replicated user state.
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

        T readSnapshot(InputStream in) throws IOException;
    }

    @FunctionalInterface
    interface Writer<T extends StateSnapshot> {

        void writeSnapshot(T snapshot, OutputStream out) throws IOException;
    }
}
