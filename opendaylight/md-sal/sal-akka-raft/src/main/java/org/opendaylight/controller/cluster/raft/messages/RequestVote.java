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
    private String candidateId;

    // index of candidate’s last log entry (§5.4)
    private long lastLogIndex;

    // term of candidate’s last log entry (§5.4)
    private long lastLogTerm;

    public RequestVote(long term, String candidateId, long lastLogIndex,
        long lastLogTerm) {
        super(term);
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    // added for testing while serialize-messages=on
    public RequestVote() {
    }

    @Override
    public long getTerm() {
        return term;
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

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override public String toString() {
        final StringBuilder sb =
            new StringBuilder("RequestVote{");
        sb.append("term='").append(getTerm()).append('\'');
        sb.append("candidateId='").append(candidateId).append('\'');
        sb.append(", lastLogIndex=").append(lastLogIndex);
        sb.append(", lastLogTerm=").append(lastLogTerm);
        sb.append('}');
        return sb.toString();
    }
}
