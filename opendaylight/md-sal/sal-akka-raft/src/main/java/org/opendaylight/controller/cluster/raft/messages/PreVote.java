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
 * Message sent to query if a node would be willing to grant its vote for a candidate (§9.6). This message
 * is similar to RequestVote.
 *
 * @author Thomas Pantelis
 */
public class PreVote implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long term;
    private final long lastLogIndex;
    private final long lastLogTerm;

    public PreVote(long term, long lastLogIndex, long lastLogTerm) {
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public long getTerm() {
        return term;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public String toString() {
        return "PreVote [term=" + term + ", lastLogIndex=" + lastLogIndex + ", lastLogTerm=" + lastLogTerm + "]";
    }
}
