/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link AppendEntriesReply}.
 */
final class AR implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // Flag bits
    private static final int SUCCESS                = 0x10;
    private static final int FORCE_INSTALL_SNAPSHOT = 0x20;
    private static final int NEEDS_LEADER_ADDRESS   = 0x40;

    private AppendEntriesReply appendEntriesReply;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AR() {
        // For Externalizable
    }

    AR(final AppendEntriesReply appendEntriesReply) {
        this.appendEntriesReply = requireNonNull(appendEntriesReply);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(appendEntriesReply.getRaftVersion());

        int flags = 0;
        if (appendEntriesReply.isSuccess()) {
            flags |= SUCCESS;
        }
        if (appendEntriesReply.isForceInstallSnapshot()) {
            flags |= FORCE_INSTALL_SNAPSHOT;
        }
        if (appendEntriesReply.isNeedsLeaderAddress()) {
            flags |= NEEDS_LEADER_ADDRESS;
        }
        WritableObjects.writeLong(out, appendEntriesReply.getTerm(), flags);

        out.writeObject(appendEntriesReply.getFollowerId());

        WritableObjects.writeLongs(out, appendEntriesReply.getLogLastIndex(), appendEntriesReply.getLogLastTerm());

        out.writeShort(appendEntriesReply.getPayloadVersion());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        short raftVersion = in.readShort();

        byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String followerId = (String) in.readObject();

        hdr = WritableObjects.readLongHeader(in);
        long logLastIndex = WritableObjects.readFirstLong(in, hdr);
        long logLastTerm = WritableObjects.readSecondLong(in, hdr);

        short payloadVersion = in.readShort();

        appendEntriesReply = new AppendEntriesReply(followerId, term, getFlag(flags, SUCCESS), logLastIndex,
            logLastTerm, payloadVersion, getFlag(flags, FORCE_INSTALL_SNAPSHOT), getFlag(flags, NEEDS_LEADER_ADDRESS),
            raftVersion, RaftVersions.CURRENT_VERSION);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(appendEntriesReply);
    }

    private static boolean getFlag(final int flags, final int bit) {
        return (flags & bit) != 0;
    }
}
