/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

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
}
