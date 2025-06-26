/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.spi.EntryStoreCompleter;
import org.opendaylight.controller.cluster.raft.spi.ImmediateEntryStore;

@VisibleForTesting
@NonNullByDefault
public final class TestPersistenceProvider extends PersistenceProvider {
    private final AtomicReference<EntryStoreCompleter> actor;

    private TestPersistenceProvider(final AtomicReference<EntryStoreCompleter> actor) {
        super((ImmediateEntryStore) actor::get, (ImmediateSnapshotStore) actor::get);
        this.actor = requireNonNull(actor);
    }

    TestPersistenceProvider() {
        this(Runnable::run);
    }

    TestPersistenceProvider(final EntryStoreCompleter actor) {
        this(new AtomicReference<>(requireNonNull(actor)));
    }

    @Override
    EntryStoreCompleter completer() {
        return actor.get();
    }

    public void setActor(final ExecuteInSelfActor newActor) {
        actor.set(requireNonNull(newActor));
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("actor", actor());
    }
}
