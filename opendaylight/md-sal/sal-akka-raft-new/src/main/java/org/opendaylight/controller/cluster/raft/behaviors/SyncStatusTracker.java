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

/**
 * The SyncStatusTracker tracks if a Follower is in sync with any given Leader or not
 * When an update is received from the Leader and the update happens to be the first update
 * from that Leader then the SyncStatusTracker will not mark the Follower as not in-sync till the
 * Followers commitIndex matches the commitIndex that the Leader sent in it's very first update.
 * Subsequently when an update is received the tracker will consider the Follower to be out of
 * sync if it is behind by 'syncThreshold' commits.
 */
public class SyncStatusTracker {

    private static final boolean IN_SYNC = true;
    private static final boolean NOT_IN_SYNC = false;
    private static final boolean FORCE_STATUS_CHANGE = true;

    private final String id;
    private String syncedLeaderId = null;
    private final ActorRef actor;
    private final int syncThreshold;
    private boolean syncStatus = false;
    private long minimumExpectedIndex = -2L;

    public SyncStatusTracker(ActorRef actor, String id, int syncThreshold) {
        this.actor = Preconditions.checkNotNull(actor, "actor should not be null");
        this.id = Preconditions.checkNotNull(id, "id should not be null");
        Preconditions.checkArgument(syncThreshold >= 0, "syncThreshold should be greater than or equal to 0");
        this.syncThreshold = syncThreshold;
    }

    public void update(String leaderId, long leaderCommit, long commitIndex){
        leaderId = Preconditions.checkNotNull(leaderId, "leaderId should not be null");

        if(!leaderId.equals(syncedLeaderId)){
            minimumExpectedIndex = leaderCommit;
            changeSyncStatus(NOT_IN_SYNC, FORCE_STATUS_CHANGE);
            syncedLeaderId = leaderId;
            return;
        }

        if((leaderCommit - commitIndex) > syncThreshold){
            changeSyncStatus(NOT_IN_SYNC);
        } else if((leaderCommit - commitIndex) <= syncThreshold && commitIndex >= minimumExpectedIndex) {
            changeSyncStatus(IN_SYNC);
        }
    }

    private void changeSyncStatus(boolean newSyncStatus){
        changeSyncStatus(newSyncStatus, !FORCE_STATUS_CHANGE);
    }

    private void changeSyncStatus(boolean newSyncStatus, boolean forceStatusChange){
        if(syncStatus == newSyncStatus && !forceStatusChange){
            return;
        }
        actor.tell(new FollowerInitialSyncUpStatus(newSyncStatus, id), ActorRef.noSender());
        syncStatus = newSyncStatus;
    }
}