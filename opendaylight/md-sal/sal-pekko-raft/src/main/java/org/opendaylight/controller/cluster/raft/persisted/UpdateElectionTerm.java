/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;

/**
 * Message class to persist election term information.
 */
public final class UpdateElectionTerm implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long currentTerm;
    private final String votedFor;

    public UpdateElectionTerm(final long currentTerm, final String votedFor) {
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
        return "UpdateElectionTerm [currentTerm=" + currentTerm + ", votedFor=" + votedFor + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new UT(this);
    }
}

