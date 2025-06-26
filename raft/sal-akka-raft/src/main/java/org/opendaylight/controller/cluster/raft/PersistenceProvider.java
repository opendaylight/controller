/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.function.BiFunction;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.EntryStoreCompleter;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;

/**
 * This interface provides methods to persist data and is an abstraction of the akka-persistence persistence API.
 */
@NonNullByDefault
@VisibleForTesting
public abstract class PersistenceProvider {
    private SnapshotStore snapshotStore;
    private EntryStore entryStore;

    PersistenceProvider(final EntryStore entryStore, final SnapshotStore snapshotStore) {
        this.entryStore = requireNonNull(entryStore);
        this.snapshotStore = requireNonNull(snapshotStore);
    }

    public final EntryStore entryStore() {
        return entryStore;
    }

    public final SnapshotStore snapshotStore() {
        return snapshotStore;
    }

    @VisibleForTesting
    public final <T extends SnapshotStore> T decorateSnapshotStore(
            final BiFunction<SnapshotStore, EntryStoreCompleter, T> factory) {
        final var ret = verifyNotNull(factory.apply(snapshotStore, completer()));
        snapshotStore = ret;
        return ret;
    }

    @VisibleForTesting
    public final <T extends EntryStore> T decorateEntryStore(
            final BiFunction<EntryStore, EntryStoreCompleter, T> factory) {
        final var ret = verifyNotNull(factory.apply(entryStore, completer()));
        entryStore = ret;
        return ret;
    }

    abstract EntryStoreCompleter completer();

    final void setStorage(final EntryStore newEntryStore, final SnapshotStore newSnapshotStore) {
        entryStore = requireNonNull(newEntryStore);
        snapshotStore = requireNonNull(newSnapshotStore);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    abstract ToStringHelper addToStringAttributes(ToStringHelper helper);
}
