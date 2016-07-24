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
import org.opendaylight.controller.cluster.raft.RaftVersions;

/**
 * Reply for the AppendEntriesRpc message
 */
public class AppendEntriesReply extends AbstractRaftRPC {
    private static final long serialVersionUID = -7487547356392536683L;

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

    private final short payloadVersion;

    private final short raftVersion;

    private final boolean forceInstallSnapshot;

    public AppendEntriesReply(String followerId, long term, boolean success, long logLastIndex, long logLastTerm,
            short payloadVersion) {
        this(followerId, term, success, logLastIndex, logLastTerm, payloadVersion, false);
    }

    public AppendEntriesReply(String followerId, long term, boolean success, long logLastIndex, long logLastTerm,
            short payloadVersion, boolean forceInstallSnapshot) {
        this(followerId, term, success, logLastIndex, logLastTerm, payloadVersion, forceInstallSnapshot,
                RaftVersions.CURRENT_VERSION);

    }

    private AppendEntriesReply(String followerId, long term, boolean success, long logLastIndex, long logLastTerm,
                              short payloadVersion, boolean forceInstallSnapshot, short raftVersion) {
        super(term);

        this.followerId = followerId;
        this.success = success;
        this.logLastIndex = logLastIndex;
        this.logLastTerm = logLastTerm;
        this.payloadVersion = payloadVersion;
        this.forceInstallSnapshot = forceInstallSnapshot;
        this.raftVersion = raftVersion;
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

    public short getPayloadVersion() {
        return payloadVersion;
    }

    public short getRaftVersion() {
        return raftVersion;
    }

    public boolean isForceInstallSnapshot() {
        return forceInstallSnapshot;
    }

    @Override
    public String toString() {
        return "AppendEntriesReply [term=" + getTerm() + ", success=" + success + ", followerId=" + followerId
                + ", logLastIndex=" + logLastIndex + ", logLastTerm=" + logLastTerm + ", forceInstallSnapshot="
                + forceInstallSnapshot + ", payloadVersion=" + payloadVersion + ", raftVersion=" + raftVersion + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private AppendEntriesReply appendEntriesReply;

        public Proxy() {
        }

        Proxy(AppendEntriesReply appendEntriesReply) {
            this.appendEntriesReply = appendEntriesReply;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeShort(appendEntriesReply.raftVersion);
            out.writeLong(appendEntriesReply.getTerm());
            out.writeObject(appendEntriesReply.followerId);
            out.writeBoolean(appendEntriesReply.success);
            out.writeLong(appendEntriesReply.logLastIndex);
            out.writeLong(appendEntriesReply.logLastTerm);
            out.writeShort(appendEntriesReply.payloadVersion);
            out.writeBoolean(appendEntriesReply.forceInstallSnapshot);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            short raftVersion = in.readShort();
            long term = in.readLong();
            String followerId = (String) in.readObject();
            boolean success = in.readBoolean();
            long logLastIndex = in.readLong();
            long logLastTerm = in.readLong();
            short payloadVersion = in.readShort();
            boolean forceInstallSnapshot = in.readBoolean();

            appendEntriesReply = new AppendEntriesReply(followerId, term, success, logLastIndex, logLastTerm,
                    payloadVersion, forceInstallSnapshot, raftVersion);
        }

        private Object readResolve() {
            return appendEntriesReply;
        }
    }
}
