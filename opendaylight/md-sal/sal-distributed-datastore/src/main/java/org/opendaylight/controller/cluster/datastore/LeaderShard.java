/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.controller.cluster.raft.state.CandidateState;
import org.opendaylight.controller.cluster.raft.state.FollowerState;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.opendaylight.controller.cluster.raft.state.IsolatedLeaderState;
import org.opendaylight.controller.cluster.raft.state.LeaderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
final class LeaderShard extends ElectedShard implements LeaderState {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderShard.class);

    LeaderShard(final CandidateShard candidate) {
        super(candidate);
    }

    @Override
    public CandidateState toCandidate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FollowerState toFollower() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IsolatedLeaderState toIsolatedLeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InactiveState toInactive() {
        // TODO Auto-generated method stub
        return stop();
    }

    @Override
    Response<ClientIdentifier, ?> handleConnectClient(final ConnectClientRequest message) {
        final var clientId = message.getTarget();
        final LeaderFrontendState frontend;
        final ABIVersion abiVersion;
        try {
            final var existing = lookupFrontend(clientId);
            if (existing != null) {
                existing.touch();
            }

            abiVersion = Shard.selectVersion(message);
            if (existing == null) {
                frontend = new LeaderFrontendState.Enabled(shardId, clientId, dataTree);
                knownFrontends.put(clientId.getFrontendId(), frontend);
                LOG.debug("{}: created state {} for client {}", id, frontend, clientId);
            } else {
                frontend = existing;
            }
        } catch (RequestException e) {
            return message.toRequestFailure(e);
        } catch (RuntimeException e) {
            return message.toRequestFailure(new RuntimeRequestException("Failed to establish connection state", e));
        }

        frontend.reconnect();
        return new ConnectClientSuccess(message.getTarget(), message.getSequence(), self(), ImmutableList.of(),
            dataTree.getDataTree(), Shard.CLIENT_MAX_MESSAGES)
            .toVersion(abiVersion);
    }

    @Override
    @Nullable RequestSuccess<?, ?> handleRequest(final RequestEnvelope envelope, final long now)
            throws RequestException {
        // FIXME: needs to be differentiated into separate states
        //        if (shuttingDown() || isLeadershipTransferInProgress() || paused) {
        //            return super.handleRequest(envelope, now);
        //        }

        final var request = envelope.getMessage();
        return switch (request) {
            case TransactionRequest<?> req -> handleTransaction(req, envelope, now);
            case LocalHistoryRequest<?> req -> handleHistory(req, envelope, now);
            default -> {
                LOG.warn("{}: rejecting unsupported request {}", id, request);
                throw new UnsupportedRequestException(request);
            }
        };
    }

    private @Nullable RequestSuccess<?, ?> handleHistory(final LocalHistoryRequest<?> req,
            final RequestEnvelope envelope, final long now) throws RequestException {
        return getFrontend(req.getTarget().getClientId()).handleLocalHistoryRequest(req, envelope, now);
    }

    private @Nullable RequestSuccess<?, ?> handleTransaction(final TransactionRequest<?> req,
            final RequestEnvelope envelope, final long now) throws RequestException {
        return getFrontend(req.getTarget().getHistoryId().getClientId()).handleTransactionRequest(req, envelope, now);
    }
}
