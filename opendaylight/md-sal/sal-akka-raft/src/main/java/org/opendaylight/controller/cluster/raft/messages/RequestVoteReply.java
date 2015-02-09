/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

public class RequestVoteReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 8427899326488775660L;

    // true means candidate received vot
    private final boolean voteGranted;

    public RequestVoteReply(long term, boolean voteGranted) {
        super(term);
        this.voteGranted = voteGranted;
    }

    @Override
    public long getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RequestVoteReply [term=").append(term).append(", voteGranted=").append(voteGranted).append("]");
        return builder.toString();
    }
}
