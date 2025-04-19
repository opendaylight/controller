/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.SnapshotManager.SnapshotComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorSnapshotMessageSupport.class);

    private final SnapshotManager snapshotManager;

    RaftActorSnapshotMessageSupport(final SnapshotManager snapshotManager) {
        this.snapshotManager = requireNonNull(snapshotManager);
    }

    boolean handleSnapshotMessage(final Object message) {
        switch (message) {
            case ApplyLeaderSnapshot msg -> snapshotManager.applyFromLeader(msg);
            case SnapshotComplete msg -> LOG.debug("{}: SnapshotComplete received", snapshotManager.memberId());
            default -> {
                return false;
            }
        }
        return true;
    }
}
