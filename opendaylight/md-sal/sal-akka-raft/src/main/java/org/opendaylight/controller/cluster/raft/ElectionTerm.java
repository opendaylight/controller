/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import javax.annotation.Nullable;

/**
 * ElectionTerm contains information about a RaftActors election term.
 * <p>
 * This information includes the last known current term of the RaftActor
 * and which candidate was voted for by the RaftActor in that term.
 * <p>
 * This class ensures that election term information is persisted.
 */
public interface ElectionTerm {
    /**
     * Returns the current leader's Raft term.
     *
     * @return the current leader's Raft term.
     */
    long getCurrentTerm();

    /**
     * Returns the id of the candidate that this server voted for in current term.
     *
     * @return candidate id that received the vote or null if no candidate was voted for.
     */
    @Nullable
    String getVotedFor();

    /**
     * This method updates the in-memory election term state. This method should be called when recovering election
     * state from persistent storage.
     *
     * @param term the election term.
     * @param votedFor the candidate id that was voted for.
     */
    void update(long term, @Nullable String votedFor);

    /**
     * This method updates the in-memory election term state and persists it so it can be recovered on next restart.
     * This method should be called when starting a new election or when a Raft RPC message is received  with a higher
     * term.
     *
     * @param term the election term.
     * @param votedFor the candidate id that was voted for.
     */
    void updateAndPersist(long term, @Nullable String votedFor);
}
