/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * Information about peers known to a {@link RaftActor}.
 */
@NonNullByDefault
final class PeerInfos implements Mutable {

    private final String memberId;
    private Map<String, PeerInfo> idToInfo;

    private boolean anyVotingPeers;

    private PeerInfos(final String memberId, final Map<String, PeerInfo> idToInfo) {
        this.memberId = requireNonNull(memberId);
        this.idToInfo = requireNonNull(idToInfo);
        anyVotingPeers = anyVotingPeers(idToInfo);
    }

    static PeerInfos ofMembers(final String memberId, final Map<String, String> idToAddress) {
        return new PeerInfos(memberId, idToAddress.entrySet().stream()
            .map(e -> new PeerInfo(e.getKey(), e.getValue(), VotingState.VOTING))
            .collect(Collectors.toMap(PeerInfo::getId, Function.identity())));
    }

    boolean isEmpty() {
        return idToInfo.isEmpty();
    }

    Set<String> peerIds() {
        return idToInfo.keySet();
    }

    Collection<PeerInfo> peerInfos() {
        return idToInfo.values();
    }

    @Nullable PeerInfo lookupPeerInfo(final String peerId) {
        return idToInfo.get(requireNonNull(peerId));
    }

    boolean anyVotingPeers() {
        return anyVotingPeers;
    }

    private static boolean anyVotingPeers(final Map<?, PeerInfo> map) {
        return map.values().stream().anyMatch(PeerInfo::isVoting);
    }

    void removePeerInfo(final String peerId) {
        idToInfo.remove(requireNonNull(peerId));
        anyVotingPeers = anyVotingPeers(idToInfo);
    }

    PeerInfo setPeerInfo(final String peerId, final @Nullable String address, final VotingState votingState) {
        final var ret = new PeerInfo(peerId, address, votingState);
        idToInfo.put(ret.getId(), ret);
        if (!anyVotingPeers) {
            anyVotingPeers = ret.isVoting();
        }
        return ret;
    }

    boolean applyVotingConfig(final Collection<ServerInfo> serverInfos) {
        final var newIdToInfo = new HashMap<String, PeerInfo>();

        boolean ret = false;
        boolean foundVoting = false;
        for (var server : serverInfos) {
            final var peerId = server.peerId();
            if (memberId.equals(peerId)) {
                ret = server.isVoting();
                continue;
            }

            final var newVotingState = server.isVoting() ? VotingState.VOTING : VotingState.NON_VOTING;
            final var existingInfo = lookupPeerInfo(peerId);
            final PeerInfo newInfo;
            if (existingInfo != null) {
                existingInfo.setVotingState(newVotingState);
                newInfo = existingInfo;
            } else {
                newInfo = new PeerInfo(peerId, null, newVotingState);
            }
            if (!foundVoting) {
                foundVoting = server.isVoting();
            }
            newIdToInfo.put(peerId, newInfo);
        }

        anyVotingPeers = anyVotingPeers(newIdToInfo);
        idToInfo = newIdToInfo;
        return ret;
    }
}
