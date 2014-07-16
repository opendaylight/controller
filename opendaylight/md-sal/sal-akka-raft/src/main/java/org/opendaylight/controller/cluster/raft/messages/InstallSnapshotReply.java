/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

public class InstallSnapshotReply extends AbstractRaftRPC {

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;

    protected InstallSnapshotReply(long term, String followerId) {
        super(term);
        this.followerId = followerId;
    }

    public String getFollowerId() {
        return followerId;
    }
}
