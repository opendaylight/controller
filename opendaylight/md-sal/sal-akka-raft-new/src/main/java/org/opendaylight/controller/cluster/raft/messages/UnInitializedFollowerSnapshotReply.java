/*
 * Copyright (c) 2015 Dell Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

/**
 * Local message sent to self on receiving InstallSnapshotReply from a follower, this message indicates that
 * the catchup of the follower is done succesfully during AddServer scenario
 */
public class  UnInitializedFollowerSnapshotReply {
    private final String followerId;

    public UnInitializedFollowerSnapshotReply(String followerId){
       this.followerId = followerId;
    }

    public String getFollowerId() {
        return followerId;
    }

    @Override
    public String toString() {
        return "UnInitializedFollowerSnapshotReply [followerId=" + followerId + "]";
    }
}
