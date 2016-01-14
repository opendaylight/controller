/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Reply message for PreVote.
 *
 * @author Thomas Pantelis
 */
public class PreVoteReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String serverId;
    private final long lastLogIndex;
    private final long lastLogTerm;
    private final boolean voteGranted;

    public PreVoteReply(String serverId, long lastLogIndex, long lastLogTerm, boolean voteGranted) {
        this.serverId = serverId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.voteGranted = voteGranted;
    }

    public String getServerId() {
        return serverId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "PreVoteReply [serverId=" + serverId + ", voteGranted=" + voteGranted + ", lastLogIndex=" + lastLogIndex
                + ", lastLogTerm=" + lastLogTerm + "]";
    }
}
