/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Reply for the AppendEntries message.
 */
public final class AppendEntriesReply extends AbstractRaftRPC {
    private static final long serialVersionUID = -7487547356392536683L;
    // Flag bits
    private static final int SUCCESS                = 0x10;
    private static final int FORCE_INSTALL_SNAPSHOT = 0x20;
    private static final int NEEDS_LEADER_ADDRESS   = 0x40;

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
    public String toString() {
        return "AppendEntriesReply [term=" + getTerm() + ", success=" + success + ", followerId=" + followerId
                + ", logLastIndex=" + logLastIndex + ", logLastTerm=" + logLastTerm + ", forceInstallSnapshot="
                + forceInstallSnapshot + ", needsLeaderAddress=" + needsLeaderAddress
                + ", payloadVersion=" + payloadVersion + ", raftVersion=" + raftVersion
                + ", recipientRaftVersion=" + recipientRaftVersion + "]";
    }

    @Override
    Object writeReplace() {
        if (recipientRaftVersion <= RaftVersions.BORON_VERSION) {
            return new Proxy(this);
        }
        return recipientRaftVersion == RaftVersions.FLUORINE_VERSION ? new Proxy2(this) : new AR(this);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeShort(getRaftVersion());

        int flags = 0;
        if (isSuccess()) {
            flags |= SUCCESS;
        }
        if (isForceInstallSnapshot()) {
            flags |= FORCE_INSTALL_SNAPSHOT;
        }
        if (isNeedsLeaderAddress()) {
            flags |= NEEDS_LEADER_ADDRESS;
        }
        WritableObjects.writeLong(out, getTerm(), flags);

        out.writeUTF(getFollowerId());

        WritableObjects.writeLongs(out, getLogLastIndex(), getLogLastTerm());

        out.writeShort(getPayloadVersion());
    }

    public static @NonNull AppendEntriesReply readFrom(final DataInput in) throws IOException {
        short raftVersion = in.readShort();

        byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String followerId = in.readUTF();

        hdr = WritableObjects.readLongHeader(in);
        long logLastIndex = WritableObjects.readFirstLong(in, hdr);
        long logLastTerm = WritableObjects.readSecondLong(in, hdr);

        short payloadVersion = in.readShort();

        return new AppendEntriesReply(followerId, term, getFlag(flags, SUCCESS), logLastIndex,
                logLastTerm, payloadVersion, getFlag(flags, FORCE_INSTALL_SNAPSHOT), getFlag(flags, NEEDS_LEADER_ADDRESS),
                raftVersion, RaftVersions.CURRENT_VERSION);
    }

    private static boolean getFlag(final int flags, final int bit) {
        return (flags & bit) != 0;
    }

    /**
     * Fluorine version that adds the needsLeaderAddress flag.
     */
    private static class Proxy2 implements Externalizable {
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

        private Object readResolve() {
            return appendEntriesReply;
        }
    }

    /**
     * Pre-Fluorine version.
     */
    @Deprecated
    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private AppendEntriesReply appendEntriesReply;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final AppendEntriesReply appendEntriesReply) {
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

            appendEntriesReply = new AppendEntriesReply(followerId, term, success, logLastIndex, logLastTerm,
                    payloadVersion, forceInstallSnapshot, false, raftVersion, RaftVersions.CURRENT_VERSION);
        }

        private Object readResolve() {
            return appendEntriesReply;
        }
    }
}
