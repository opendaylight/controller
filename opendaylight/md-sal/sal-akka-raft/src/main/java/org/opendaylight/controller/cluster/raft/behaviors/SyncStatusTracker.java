/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SyncStatusTracker tracks if a Follower is in sync with any given Leader or not
 * When an update is received from the Leader and the update happens to be the first update
 * from that Leader then the SyncStatusTracker will not mark the Follower as not in-sync till the
 * Followers commitIndex matches the commitIndex that the Leader sent in it's very first update.
 * Subsequently when an update is received the tracker will consider the Follower to be out of
 * sync if it is behind by 'syncThreshold' commits.
 */
public class SyncStatusTracker {
    private static final class LeaderInfo {
        final long minimumCommitIndex;
        final String leaderId;

        LeaderInfo(final String leaderId, final long minimumCommitIndex) {
            this.leaderId = Preconditions.checkNotNull(leaderId);
            this.minimumCommitIndex = minimumCommitIndex;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SyncStatusTracker.class);

    private static final boolean IN_SYNC = true;
    private static final boolean NOT_IN_SYNC = false;

    private final long syncThreshold;
    private final ActorRef actor;
    private final String id;

    private LeaderInfo syncTarget;
    private boolean syncStatus;

    public SyncStatusTracker(final ActorRef actor, final String id, final long syncThreshold) {
        this.actor = Preconditions.checkNotNull(actor, "actor should not be null");
        this.id = Preconditions.checkNotNull(id, "id should not be null");
        Preconditions.checkArgument(syncThreshold >= 0, "syncThreshold should be greater than or equal to 0");
        this.syncThreshold = syncThreshold;
    }

    public void update(final String leaderId, final long leaderCommit, final long commitIndex) {
        Preconditions.checkNotNull(leaderId, "leaderId should not be null");

        if (syncTarget == null || !leaderId.equals(syncTarget.leaderId)) {
            LOG.debug("{}: Last sync leader does not match current leader {}, need to catch up to {}", id,
                leaderId, leaderCommit);
            changeSyncStatus(NOT_IN_SYNC, true);
            syncTarget = new LeaderInfo(leaderId, leaderCommit);
            return;
        }

        final long lag = leaderCommit - commitIndex;
        if (lag > syncThreshold) {
            LOG.debug("{}: Lagging {} entries behind leader {}", id, lag, leaderId);
            changeSyncStatus(NOT_IN_SYNC, false);
        } else if (commitIndex >= syncTarget.minimumCommitIndex) {
            LOG.debug("{}: Lagging {} entries behind leader and reached {} (of expected {})", id, lag, leaderId,
                commitIndex, syncTarget.minimumCommitIndex);
            changeSyncStatus(IN_SYNC, false);
        }
    }

    private void changeSyncStatus(final boolean newSyncStatus, final boolean forceStatusChange) {
        if (forceStatusChange || newSyncStatus != syncStatus) {
            actor.tell(new FollowerInitialSyncUpStatus(newSyncStatus, id), ActorRef.noSender());
            syncStatus = newSyncStatus;
        } else {
            LOG.trace("{}: No change in sync status of, dampening message", id);
        }
    }
}
