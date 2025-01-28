/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

@NonNullByDefault
public final class NoopEntryStore implements EntryStore {
    @Override
    public void persist(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        callback.accept(entry);
    }

    @Override
    public void persistAndSync(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        callback.accept(entry);
    }

    @Override
    public void removeFrom(final long fromIndex) {
        // No-op
    }

    @Override
    public void applyTo(final long toIndex) {
        // No-op
    }
}
