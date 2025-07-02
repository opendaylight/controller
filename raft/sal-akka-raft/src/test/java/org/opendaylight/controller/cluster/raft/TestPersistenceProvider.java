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
import org.opendaylight.controller.cluster.raft.spi.ImmediateEntryStore;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;

@VisibleForTesting
@NonNullByDefault
public final class TestPersistenceProvider extends PersistenceProvider {
    private final AtomicReference<RaftStorageCompleter> completer;

    private TestPersistenceProvider(final AtomicReference<RaftStorageCompleter> completer) {
        super((ImmediateEntryStore) completer::get, (ImmediateSnapshotStore) completer::get);
        this.completer = requireNonNull(completer);
    }

    TestPersistenceProvider() {
        this(new RaftStorageCompleter("test", Runnable::run));
    }

    TestPersistenceProvider(final RaftStorageCompleter actor) {
        this(new AtomicReference<>(requireNonNull(actor)));
    }

    @Override
    RaftStorageCompleter completer() {
        return completer.get();
    }

    public void setActor(final ExecuteInSelfActor actor) {
        setCompleter(new RaftStorageCompleter(completer().memberId(), actor));
    }

    public void setCompleter(final RaftStorageCompleter newCompleter) {
        completer.set(requireNonNull(newCompleter));
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("completer", completer());
    }
}
