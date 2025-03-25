/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Invoked by candidates to gather votes (§5.2).
 */
public final class RequestVote extends RaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = -6967509186297108657L;

    // candidate requesting vote
    private final String candidateId;

    // index of candidate’s last log entry (§5.4)
    private final long lastLogIndex;

    // term of candidate’s last log entry (§5.4)
    private final long lastLogTerm;

    public RequestVote(final long term, final String candidateId, final long lastLogIndex, final long lastLogTerm) {
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
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("candidateId", candidateId)
            .add("lastLogIndex", lastLogIndex)
            .add("lastLogTerm", lastLogTerm);
    }

    @Override
    Object writeReplace() {
        return new RV(this);
    }
}
