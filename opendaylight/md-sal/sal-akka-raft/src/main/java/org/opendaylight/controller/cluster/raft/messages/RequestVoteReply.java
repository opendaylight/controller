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

public final class RequestVoteReply extends AbstractRaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 8427899326488775660L;

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
