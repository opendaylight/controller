/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * Stores information about a raft peer.
 *
 * @author Thomas Pantelis
 */
public class PeerInfo {
    private final String id;
    private String address;
    private VotingState votingState;

    /**
     * Constructs an instance.
     *
     * @param id the id of the peer.
     * @param address the address of the peer.
     * @param votingState the VotingState of the peer.
     */
    public PeerInfo(String id, String address, VotingState votingState) {
        this.id = id;
        this.address = address;
        this.votingState = votingState;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public VotingState getVotingState() {
        return votingState;
    }

    public boolean isVoting() {
        return votingState == VotingState.VOTING;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setVotingState(VotingState votingState) {
        this.votingState = votingState;
    }

    @Override
    public String toString() {
        return "PeerInfo [id=" + id + ", address=" + address + ", votingState=" + votingState + "]";
    }
}
