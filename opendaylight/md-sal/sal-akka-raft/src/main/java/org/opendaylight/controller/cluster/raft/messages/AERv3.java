/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Sodium Externalizable proxy for AppendEntries.
 */
final class AERv3 implements Externalizable {
    private static final long serialVersionUID = 1L;

    private AppendEntriesReply appendEntriesReply;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AERv3() {
        // For Externalizable
    }

    AERv3(final AppendEntriesReply appendEntriesReply) {
        this.appendEntriesReply = requireNonNull(appendEntriesReply);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(appendEntriesReply.getRaftVersion());
        out.writeLong(appendEntriesReply.getTerm());
        out.writeObject(appendEntriesReply.getFollowerId());
        out.writeBoolean(appendEntriesReply.isSuccess());

        WritableObjects.writeLongs(out, appendEntriesReply.getLogLastIndex(), appendEntriesReply.getLogLastTerm());

        out.writeShort(appendEntriesReply.getPayloadVersion());
        out.writeBoolean(appendEntriesReply.isForceInstallSnapshot());
        out.writeBoolean(appendEntriesReply.isNeedsLeaderAddress());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final short raftVersion = in.readShort();
        final long term = in.readLong();
        final String followerId = (String) in.readObject();
        final boolean success = in.readBoolean();

        byte header = WritableObjects.readLongHeader(in);
        final long logLastIndex = WritableObjects.readFirstLong(in, header);
        final long logLastTerm = WritableObjects.readSecondLong(in, header);
        final short payloadVersion = in.readShort();
        final boolean forceInstallSnapshot = in.readBoolean();
        final boolean needsLeaderAddress = in.readBoolean();

        appendEntriesReply = new AppendEntriesReply(followerId, term, success, logLastIndex, logLastTerm,
                payloadVersion, forceInstallSnapshot, needsLeaderAddress, raftVersion,
                RaftVersions.CURRENT_VERSION);
    }

    private Object readResolve() {
        return appendEntriesReply;
    }
}
