/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2025 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The role a server is performing in terms of its contributing towards maintaining the RAFT journal. This is a
 * finer-grained view on RAFT than what {@link ServerRole} provices. Note that the unusual naming here is intentional to
 * differentiate the two.
 */
@NonNullByDefault
public enum RaftRole {
    /**
     * A candidate server.
     */
    Candidate(ServerRole.CANDIDATE),
    /**
     * A follower server.
     */
    Follower(ServerRole.FOLLOWER),
    /**
     * A leader server.
     */
    Leader(ServerRole.LEADER),
    /**
     * A leader server which cannot communicate with its peers.
     */
    // FIXME: better describe conditions for entering and exiting this state, based on our current implementation and/or
    //        any reference documentation:
    //        - we have an explicit timer for this
    //        - we are using Pekko Cluster to get some connectivity information, is it related?
    //        - we do not make progress in journal and are starting other throttling:
    //          - the datastore knows this and can start pushing back on its clients
    //          - the Entity Ownership Service we used to have propagated this as 'in jeopardy' flag
    IsolatedLeader(ServerRole.LEADER),
    /**
     * The winner of a RAFT election. It is acting as a {@link #Leader}, but is not known to have a persisted its
     * authority about its ownership of the term. This uncertainty is resolved once an entry for this term is applied
     * to the log.
     */
    PreLeader(ServerRole.LEADER);

    private final ServerRole serverRole;

    RaftRole(final ServerRole serverRole) {
        this.serverRole = requireNonNull(serverRole);
    }

    /**
     * Returns the corresponding {@link ServerRole}.
     *
     * @return the corresponding {@link ServerRole}
     */
    public ServerRole serverRole() {
        return serverRole;
    }
}
