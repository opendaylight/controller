/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.OutOfSequenceEnvelopeException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.raft.journal.AppendJournal;
import org.opendaylight.controller.cluster.raft.state.ElectedStateBehavior;
import org.opendaylight.controller.cluster.raft.state.LeaderStateException;

/**
 * A shard that has been promoted to {@link ElectedStateBehavior}, i.e. it is free to make RAFT leader decisions.
 */
@NonNullByDefault
abstract sealed class ElectedShard extends StartedShard
        permits IsolatedLeaderShard, PreLeaderShard, LeaderShard {
    final HashMap<FrontendIdentifier, LeaderFrontendState> knownFrontends = new HashMap<>();

    ElectedShard(final CandidateShard candidate) {
        super(candidate);
    }

    final @Nullable LeaderFrontendState lookupFrontend(final ClientIdentifier clientId) throws RequestException {
        // FIXME: from shard
        return null;
    }

    // FIXME: better name?
    final LeaderFrontendState getFrontend(final ClientIdentifier clientId) throws RequestException {
        final var frontend = lookupFrontend(clientId);
        if (frontend != null) {
            return frontend;
        }
        // TODO: a dedicated exception would be better, but this is technically true, too
        throw new OutOfSequenceEnvelopeException(0);
    }

    @Override
    @SuppressWarnings("null")
    final ImmutableSet<ClientIdentifier> getClients() {
        return knownFrontends.values().stream()
            .map(LeaderFrontendState::getIdentifier)
            .collect(ImmutableSet.toImmutableSet());
    }

    public final void onConsensusAchieved(final AppendJournal request) throws LeaderStateException {
        // TODO Auto-generated method stub

    }
}
