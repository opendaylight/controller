/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * Storage for storing {@link ReplicatedLogEntry}.
 */
public interface EntryStore {

    void persist(@NonNull ReplicatedLogEntry entry, @NonNull Consumer<ReplicatedLogEntry> callback);

    void persistAndSync(@NonNull ReplicatedLogEntry entry, @NonNull Consumer<ReplicatedLogEntry> callback);

    // Note: synchronous operation
    void removeFrom(long fromIndex);

    void applyTo(long toIndex);
}
