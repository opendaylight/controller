/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * {@link RaftActorSnapshotCohort} corresponding to {@link MockSnapshotState}.
 */
public interface MockRaftActorSnapshotCohort extends RaftActorSnapshotCohort<MockSnapshotState> {
    @Override
    default Class<MockSnapshotState> stateClass() {
        return MockSnapshotState.class;
    }
}
