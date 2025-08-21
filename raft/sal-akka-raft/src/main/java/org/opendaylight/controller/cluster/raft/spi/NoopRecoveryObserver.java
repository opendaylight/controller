/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link RecoveryObserver} which does nothing.
 */
@NonNullByDefault
public final class NoopRecoveryObserver implements RecoveryObserver {

    public static final NoopRecoveryObserver INSTANCE = new NoopRecoveryObserver();

    private NoopRecoveryObserver() {
        // Hidden on purpose
    }

    @Override
    public void onSnapshotRecovered(final StateSnapshot snapshot) {
        // No-op
    }

    @Override
    public void onCommandRecovered(final StateMachineCommand command) {
        // No-op
    }

    @Override
    public void onRecoveryCompleted() {
        // No-op
    }
}
