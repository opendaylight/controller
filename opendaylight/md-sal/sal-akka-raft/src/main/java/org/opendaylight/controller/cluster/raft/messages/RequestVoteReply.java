/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class RequestVoteReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 8427899326488775660L;
    // Flags
    private static final int VOTE_GRANTED = 0x10;

    // true means candidate received vote
    private final boolean voteGranted;

    public RequestVoteReply(final long term, final boolean voteGranted) {
        super(term);
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteReply [term=" + getTerm() + ", voteGranted=" + voteGranted + "]";
    }

    @Override
    Object writeReplace() {
        return new VR(this);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        WritableObjects.writeLong(out, getTerm(), isVoteGranted() ? VOTE_GRANTED : 0);
    }

    public static @NonNull RequestVoteReply readFrom(final DataInput in) throws IOException {
        final byte hdr = WritableObjects.readLongHeader(in);
        return new RequestVoteReply(WritableObjects.readLongBody(in, hdr),
                (WritableObjects.longHeaderFlags(hdr) & VOTE_GRANTED) != 0);
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private RequestVoteReply requestVoteReply;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final RequestVoteReply requestVoteReply) {
            this.requestVoteReply = requestVoteReply;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(requestVoteReply.getTerm());
            out.writeBoolean(requestVoteReply.voteGranted);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            long term = in.readLong();
            boolean voteGranted = in.readBoolean();

            requestVoteReply = new RequestVoteReply(term, voteGranted);
        }

        @java.io.Serial
        private Object readResolve() {
            return requestVoteReply;
        }
    }
}
