/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmediateDataPersistenceProvider;

@NonNullByDefault
final class TestDataProvider implements ImmediateDataPersistenceProvider {
    private ExecuteInSelfActor actor;

    TestDataProvider() {
        this(Runnable::run);
    }

    TestDataProvider(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }

    @Override
    public ExecuteInSelfActor actor() {
        return actor;
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        // no-op
    }

    void setActor(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }
}
