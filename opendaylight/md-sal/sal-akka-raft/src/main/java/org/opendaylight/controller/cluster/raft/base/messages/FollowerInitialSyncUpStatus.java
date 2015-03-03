/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

/**
 * The FollowerInitialSyncUpStatus is sent by a Follower to inform any RaftActor subclass whether the Follower
 * is at least at the same commitIndex as the Leader was when it sent the follower the very first heartbeat.
 *
 * This status can be used to determine if a Follower has caught up with the current Leader in an upgrade scenario
 * for example.
 *
 */
public class FollowerInitialSyncUpStatus {
    private final boolean initialSyncDone;

    public FollowerInitialSyncUpStatus(boolean initialSyncDone){
        this.initialSyncDone = initialSyncDone;
    }

    public boolean isInitialSyncDone() {
        return initialSyncDone;
    }
}
