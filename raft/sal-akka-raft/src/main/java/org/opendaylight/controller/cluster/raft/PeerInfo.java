/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Stores information about a raft peer.
 *
 * @author Thomas Pantelis
 */
public final class PeerInfo {
    private final @NonNull String id;

    private @NonNull VotingState votingState;
    private @Nullable String address;

    /**
     * Constructs an instance.
     *
     * @param id the id of the peer.
     * @param address the address of the peer.
     * @param votingState the VotingState of the peer.
     */
    public PeerInfo(final String id, final String address, final VotingState votingState) {
        this.id = requireNonNull(id);
        this.address = address;
        this.votingState = requireNonNull(votingState);
    }

    public @NonNull String getId() {
        return id;
    }

    public @Nullable String getAddress() {
        return address;
    }

    public @NonNull VotingState getVotingState() {
        return votingState;
    }

    public boolean isVoting() {
        return votingState == VotingState.VOTING;
    }

    public void setAddress(final @Nullable String address) {
        this.address = address;
    }

    public void setVotingState(final VotingState newVotingState) {
        votingState = requireNonNull(newVotingState);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
            .add("id", id)
            .add("address", address)
            .add("votingState", votingState)
            .toString();
    }
}
