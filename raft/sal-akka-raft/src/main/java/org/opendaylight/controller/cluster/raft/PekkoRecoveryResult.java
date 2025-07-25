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

/**
 * Result of a {@link PekkoRecovery} run.
 */
@NonNullByDefault
record PekkoRecoveryResult(RecoveryLog recoveryLog, boolean canRestoreFromSnapshot) {
    PekkoRecoveryResult {
        requireNonNull(recoveryLog);
    }
}
