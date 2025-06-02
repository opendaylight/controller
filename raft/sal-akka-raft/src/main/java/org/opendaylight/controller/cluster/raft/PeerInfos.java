/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about local member and peers.
 */
@NonNullByDefault
final class PeerInfos {
    private static final Logger LOG = LoggerFactory.getLogger(PeerInfos.class);

    private final Map<String, PeerInfo> peerInfoMap;
    private final String memberId;

    private boolean dynamicServerConfiguration = false;
    private boolean votingMember = true;
    private int numVotingPeers = -1;

    PeerInfos(final String memberId, final Map<String, String> peerAddresses) {
        this.memberId = requireNonNull(memberId);

        peerInfoMap = peerAddresses.entrySet().stream()
            .map(entry -> new PeerInfo(entry.getKey(), entry.getValue(), VotingState.VOTING))
            .collect(Collectors.toMap(PeerInfo::getId, Function.identity()));
    }

    Set<String> peerIds() {
        return peerInfoMap.keySet();
    }

    Collection<PeerInfo> peerInfos() {
        return peerInfoMap.values();
    }

    @Nullable PeerInfo lookupPeerInfo(final String peerId) {
        return peerInfoMap.get(requireNonNull(peerId));
    }

    boolean dynamicServerConfiguration() {
        return dynamicServerConfiguration;
    }

    void setDynamicServerConfiguration() {
        dynamicServerConfiguration = true;
    }

    boolean votingMember() {
        return votingMember;
    }

    boolean anyVotingPeers() {
        if (numVotingPeers < 0) {
            numVotingPeers = 0;
            for (var info : peerInfoMap.values()) {
                if (info.isVoting()) {
                    numVotingPeers++;
                }
            }
        }
        return numVotingPeers > 0;
    }

    void addPeer(final String peerId, final @Nullable String address, final VotingState votingState) {
        peerInfoMap.put(peerId, new PeerInfo(peerId, address, votingState));
        numVotingPeers = -1;
    }

    void removePeer(final String peerId) {
        if (memberId.equals(peerId)) {
            votingMember = false;
        } else if (peerInfoMap.remove(peerId) != null) {
            numVotingPeers = -1;
        }
    }

    public void setPeerAddress(final String peerId, final @Nullable String peerAddress) {
        final var peerInfo = peerInfoMap.get(peerId);
        if (peerInfo != null) {
            LOG.info("{}: Peer address for peer {} set to {}", memberId, peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    void updateVotingConfig(final VotingConfig votingConfig) {
        boolean newVotingMember = false;
        final var currentPeers = new HashSet<>(peerIds());
        for (var server : votingConfig.serverInfo()) {
            final var peerId = server.peerId();
            if (memberId.equals(peerId)) {
                newVotingMember = server.isVoting();
                continue;
            }

            final var votingState = server.isVoting() ? VotingState.VOTING : VotingState.NON_VOTING;
            if (currentPeers.contains(peerId)) {
                lookupPeerInfo(peerId).setVotingState(votingState);
                currentPeers.remove(peerId);
            } else {
                addPeer(peerId, null, votingState);
            }
        }

        for (var peerIdToRemove : currentPeers) {
            removePeer(peerIdToRemove);
        }

        votingMember = newVotingMember;
        dynamicServerConfiguration = true;
        LOG.debug("{}: Updated server config: isVoting: {}, peers: {}", memberId, votingMember, peerInfos());
    }

    @Nullable VotingConfig votingConfig(final boolean includeSelf) {
        if (!dynamicServerConfiguration) {
            return null;
        }
        final var peers = peerInfos();
        final var newConfig = ImmutableList.<ServerInfo>builderWithExpectedSize(peers.size() + (includeSelf ? 1 : 0));
        for (var peer : peers) {
            newConfig.add(new ServerInfo(peer.getId(), peer.isVoting()));
        }

        if (includeSelf) {
            newConfig.add(new ServerInfo(memberId, votingMember));
        }

        return new VotingConfig(newConfig.build());
    }
}
