/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for observing the recovery process.
 */
@Beta
@NonNullByDefault
public interface RecoveryObserver {

    void onSnapshotRecovered(StateSnapshot snapshot);

    void onCommandRecovered(StateMachineCommand command);

    void onRecoveryCompleted();
}
