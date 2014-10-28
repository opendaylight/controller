/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Reply for the AppendEntriesRpc message
 */
public class AppendEntriesReply extends AbstractRaftRPC implements Externalizable {
    private static final long serialVersionUID = 1L;

    // true if follower contained entry matching
    // prevLogIndex and prevLogTerm
    private transient boolean success;

    // The index of the last entry in the followers log
    // This will be used to set the matchIndex for the follower on the
    // Leader
    private transient long logLastIndex;

    private transient long logLastTerm;

    // The followerId - this will be used to figure out which follower is
    // responding
    private transient String followerId;

    public AppendEntriesReply() {
        super(-1);
    }

    public AppendEntriesReply(String followerId, long term, boolean success, long logLastIndex, long logLastTerm) {
        super(term);

        this.followerId = followerId;
        this.success = success;
        this.logLastIndex = logLastIndex;
        this.logLastTerm = logLastTerm;
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

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        followerId = in.readUTF();
        setTerm(in.readLong());
        logLastIndex = in.readLong();
        logLastTerm = in.readLong();
        success = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(followerId);
        out.writeLong(getTerm());
        out.writeLong(logLastIndex);
        out.writeLong(logLastTerm);
        out.writeBoolean(success);
    }

    @Override public String toString() {
        final StringBuilder sb =
            new StringBuilder("AppendEntriesReply{");
        sb.append("term=").append(getTerm());
        sb.append(", success=").append(success);
        sb.append(", logLastIndex=").append(logLastIndex);
        sb.append(", logLastTerm=").append(logLastTerm);
        sb.append(", followerId='").append(followerId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
