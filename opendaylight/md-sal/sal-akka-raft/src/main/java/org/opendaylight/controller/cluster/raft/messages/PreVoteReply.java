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

    private final boolean voteGranted;

    public PreVoteReply(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "PreVoteReply [voteGranted=" + voteGranted + "]";
    }
}
