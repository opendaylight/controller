/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

/**
 * ElectionTerm contains information about a RaftActors election term.
 * <p>
 * This information includes the last known current term of the RaftActor
 * and which peer was voted for by the RaftActor in that term
 * <p>
 * This class ensures that election term information is persisted
 */
public interface ElectionTerm {
    /**
     * latest term server has seen (initialized to 0
     * on first boot, increases monotonically)
     */
    long getCurrentTerm();

    /**
     * candidateId that received vote in current
     * term (or null if none)
     */
    String getVotedFor();

    /**
     * To be called mainly when we are recovering in-memory election state from
     * persistent storage
     *
     * @param currentTerm
     * @param votedFor
     */
    void update(long currentTerm, String votedFor);

    /**
     * To be called when we need to update the current term either because we
     * received a message from someone with a more up-to-date term or because we
     * just voted for someone
     * <p>
     * This information needs to be persisted so that on recovery the replica
     * can start itself in the right term and know if it has already voted in
     * that term or not
     *
     * @param currentTerm
     * @param votedFor
     */
    void updateAndPersist(long currentTerm, String votedFor);
}
