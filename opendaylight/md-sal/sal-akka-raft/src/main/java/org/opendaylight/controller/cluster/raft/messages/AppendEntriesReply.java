/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

/**
 * Reply for the AppendEntriesRpc message
 */
public class AppendEntriesReply extends AbstractRaftRPC {

    // true if follower contained entry matching
    // prevLogIndex and prevLogTerm
    private final boolean success;

    // The index of the last entry in the followers log
    // This will be used to set the matchIndex for the follower on the
    // Leader
    private final long logLastIndex;

    private final long logLastTerm;

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;

    public AppendEntriesReply(String followerId, long term, boolean success, long logLastIndex, long logLastTerm) {
        super(term);

        this.followerId = followerId;
        this.success = success;
        this.logLastIndex = logLastIndex;
        this.logLastTerm = logLastTerm;
    }

    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getLogLastIndex() {
        return logLastIndex;
    }

    public long getLogLastTerm() {
        return logLastTerm;
    }

    public String getFollowerId() {
        return followerId;
    }

    @Override public String toString() {
        final StringBuilder sb =
            new StringBuilder("AppendEntriesReply{");
        sb.append("term=").append(term);
        sb.append(", success=").append(success);
        sb.append(", logLastIndex=").append(logLastIndex);
        sb.append(", logLastTerm=").append(logLastTerm);
        sb.append(", followerId='").append(followerId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
