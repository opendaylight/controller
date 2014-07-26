/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.util.concurrent.atomic.AtomicLong;

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
    AtomicLong getCurrentTerm();

    /**
     * candidateId that received vote in current
     * term (or null if none)
     */
    String getVotedFor();

    /**
     * Called when we need to update the current term either because we received
     * a message from someone with a more uptodate term or because we just voted
     * for someone
     *
     * @param currentTerm
     * @param votedFor
     */
    void update(AtomicLong currentTerm, String votedFor);
}
