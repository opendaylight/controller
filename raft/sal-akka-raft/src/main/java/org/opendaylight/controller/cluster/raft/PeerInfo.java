/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Stores information about a raft peer.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class PeerInfo {
    private final String id;

    private @Nullable String address;
    private VotingState votingState;

    /**
     * Constructs an instance.
     *
     * @param id the id of the peer.
     * @param address the address of the peer.
     * @param votingState the VotingState of the peer.
     */
    public PeerInfo(final String id, final @Nullable String address, final VotingState votingState) {
        this.id = requireNonNull(id);
        this.address = address;
        this.votingState = requireNonNull(votingState);
    }

    public String getId() {
        return id;
    }

    public @Nullable String getAddress() {
        return address;
    }

    public VotingState getVotingState() {
        return votingState;
    }

    public boolean isVoting() {
        return votingState == VotingState.VOTING;
    }

    public void setAddress(final @Nullable String address) {
        this.address = address;
    }

    public void setVotingState(final VotingState votingState) {
        this.votingState = requireNonNull(votingState);
    }

    @Override
    public String toString() {
        return "PeerInfo [id=" + id + ", address=" + address + ", votingState=" + votingState + "]";
    }
}
