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
 * Invoked by candidates to gather votes (§5.2).
 */
public class RequestVote extends AbstractRaftRPC {
    private static final long serialVersionUID = -6967509186297108657L;

    // candidate requesting vote
    private final String candidateId;

    // index of candidate’s last log entry (§5.4)
    private final long lastLogIndex;

    // term of candidate’s last log entry (§5.4)
    private final long lastLogTerm;

    public RequestVote(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
        super(term);
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RequestVote [term=").append(getTerm()).append(", candidateId=").append(candidateId)
                .append(", lastLogIndex=").append(lastLogIndex).append(", lastLogTerm=").append(lastLogTerm)
                .append("]");
        return builder.toString();
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private RequestVote requestVote;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(RequestVote requestVote) {
            this.requestVote = requestVote;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(requestVote.getTerm());
            out.writeObject(requestVote.candidateId);
            out.writeLong(requestVote.lastLogIndex);
            out.writeLong(requestVote.lastLogTerm);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String candidateId = (String) in.readObject();
            long lastLogIndex = in.readLong();
            long lastLogTerm = in.readLong();

            requestVote = new RequestVote(term, candidateId, lastLogIndex, lastLogTerm);
        }

        private Object readResolve() {
            return requestVote;
        }
    }
}
