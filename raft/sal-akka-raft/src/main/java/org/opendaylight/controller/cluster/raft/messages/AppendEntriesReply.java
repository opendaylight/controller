/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;

/**
 * Reply for the AppendEntries message.
 */
public final class AppendEntriesReply extends RaftRPC {
    @java.io.Serial
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

    private final boolean needsLeaderAddress;

    private final short recipientRaftVersion;

    @VisibleForTesting
    public AppendEntriesReply(final String followerId, final long term, final boolean success, final long logLastIndex,
            final long logLastTerm, final short payloadVersion) {
        this(followerId, term, success, logLastIndex, logLastTerm, payloadVersion, false, false,
                RaftVersions.CURRENT_VERSION);
    }

    public AppendEntriesReply(final String followerId, final long term, final boolean success, final long logLastIndex,
            final long logLastTerm, final short payloadVersion, final boolean forceInstallSnapshot,
            final boolean needsLeaderAddress, final short recipientRaftVersion) {
        this(followerId, term, success, logLastIndex, logLastTerm, payloadVersion, forceInstallSnapshot,
                needsLeaderAddress, RaftVersions.CURRENT_VERSION, recipientRaftVersion);
    }

    AppendEntriesReply(final String followerId, final long term, final boolean success, final long logLastIndex,
            final long logLastTerm, final short payloadVersion, final boolean forceInstallSnapshot,
            final boolean needsLeaderAddress, final short raftVersion, final short recipientRaftVersion) {
        super(term);
        this.followerId = followerId;
        this.success = success;
        this.logLastIndex = logLastIndex;
        this.logLastTerm = logLastTerm;
        this.payloadVersion = payloadVersion;
        this.forceInstallSnapshot = forceInstallSnapshot;
        this.raftVersion = raftVersion;
        this.needsLeaderAddress = needsLeaderAddress;
        this.recipientRaftVersion = recipientRaftVersion;
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

    public boolean isNeedsLeaderAddress() {
        return needsLeaderAddress;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("success", success)
            .add("followerId", followerId)
            .add("logLastIndex", logLastIndex)
            .add("logLastTerm", logLastTerm)
            .add("forceInstallSnapshot", forceInstallSnapshot)
            .add("needsLeaderAddress", needsLeaderAddress)
            .add("payloadVersion", payloadVersion)
            .add("raftVersion", raftVersion)
            .add("recipientRaftVersion", recipientRaftVersion);
    }

    @Override
    Object writeReplace() {
        return recipientRaftVersion <= RaftVersions.FLUORINE_VERSION ? new Proxy2(this) : new AR(this);
    }

    /**
     * Fluorine version that adds the needsLeaderAddress flag.
     */
    private static class Proxy2 implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private AppendEntriesReply appendEntriesReply;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy2() {
        }

        Proxy2(final AppendEntriesReply appendEntriesReply) {
            this.appendEntriesReply = appendEntriesReply;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeShort(appendEntriesReply.raftVersion);
            out.writeLong(appendEntriesReply.getTerm());
            out.writeObject(appendEntriesReply.followerId);
            out.writeBoolean(appendEntriesReply.success);
            out.writeLong(appendEntriesReply.logLastIndex);
            out.writeLong(appendEntriesReply.logLastTerm);
            out.writeShort(appendEntriesReply.payloadVersion);
            out.writeBoolean(appendEntriesReply.forceInstallSnapshot);
            out.writeBoolean(appendEntriesReply.needsLeaderAddress);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            short raftVersion = in.readShort();
            long term = in.readLong();
            String followerId = (String) in.readObject();
            boolean success = in.readBoolean();
            long logLastIndex = in.readLong();
            long logLastTerm = in.readLong();
            short payloadVersion = in.readShort();
            boolean forceInstallSnapshot = in.readBoolean();
            boolean needsLeaderAddress = in.readBoolean();

            appendEntriesReply = new AppendEntriesReply(followerId, term, success, logLastIndex, logLastTerm,
                    payloadVersion, forceInstallSnapshot, needsLeaderAddress, raftVersion,
                    RaftVersions.CURRENT_VERSION);
        }

        @java.io.Serial
        private Object readResolve() {
            return appendEntriesReply;
        }
    }
}
