/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * Immutable implementation of ElectionTerm.
 *
 * @author Thomas Pantelis
 */
public class ImmutableElectionTerm implements ElectionTerm {
    private final long currentTerm;
    private final String votedFor;

    private ImmutableElectionTerm(long currentTerm, String votedFor) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public String getVotedFor() {
        return votedFor;
    }

    @Override
    public void update(long currentTerm, String votedFor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAndPersist(long currentTerm, String votedFor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ImmutableElectionTerm [currentTerm=" + currentTerm + ", votedFor=" + votedFor + "]";
    }

    public static ElectionTerm copyOf(ElectionTerm from) {
        return new ImmutableElectionTerm(from.getCurrentTerm(), from.getVotedFor());
    }
}
