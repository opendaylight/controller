/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Message that persists the current term information.
 */
public class UpdateElectionTerm implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long currentTerm;
    private final String votedFor;

    public UpdateElectionTerm(long currentTerm, String votedFor) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UpdateElectionTerm [currentTerm=").append(currentTerm).append(", votedFor=").append(votedFor)
                .append("]");
        return builder.toString();
    }
}