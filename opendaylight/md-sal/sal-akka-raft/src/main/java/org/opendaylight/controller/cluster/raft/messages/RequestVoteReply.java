/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

public final class RequestVoteReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 8427899326488775660L;

    // true means candidate received vote
    private final boolean voteGranted;

    public RequestVoteReply(long term, boolean voteGranted) {
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
}
