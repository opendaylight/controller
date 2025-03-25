/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmediateDataPersistenceProvider;

@NonNullByDefault
final class TestDataProvider implements ImmediateDataPersistenceProvider {
    static final TestDataProvider INSTANCE = new TestDataProvider();

    private TestDataProvider() {
        // Hidden in purpose
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        // no-op
    }

    @Override
    public void executeInSelf(final Runnable runnable) {
        runnable.run();
    }
}
