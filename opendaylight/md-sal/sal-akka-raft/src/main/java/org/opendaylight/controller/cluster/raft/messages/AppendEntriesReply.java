/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

/**
 * Reply for the AppendEntriesRpc message
 */
public class AppendEntriesReply {
    // currentTerm, for leader to update itself
    private final long term;

    // true if follower contained entry matching
    // prevLogIndex and prevLogTerm
    private final boolean success;

    public AppendEntriesReply(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }
}
