/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Status.Failure;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClients;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClientsReply;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.raft.LeadershipTransferFailedException;
import org.opendaylight.controller.cluster.raft.api.RaftActorAccess;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.state.ActiveStateMethods;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shard-managed piece of state. Contains the JMX.
 */
@NonNullByDefault
abstract sealed class ActiveShard extends ShardBehavior implements ActiveStateMethods
        permits StartingShard, StartedShard {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveShard.class);

    private final InactiveShard inactive;

    final RaftActorAccess actorAccess;
    final FrontendMetadata frontendMeta;
    // FIXME:
    //        final ShardDataTreeListenerInfoMXBeanImpl listenerInfoMXBean;
    final DefaultShardStatsMXBean shardStatsBean;

    ActiveShard(final InactiveShard inactive, final RaftActorAccess actorAccess,
            final DefaultShardStatsMXBean shardStatsBean) {
        super(inactive);
        this.inactive = requireNonNull(inactive);
        this.actorAccess = requireNonNull(actorAccess);
        this.shardStatsBean = requireNonNull(shardStatsBean);
        frontendMeta = new FrontendMetadata(shardStatsBean.getShardName());
    }

    ActiveShard(final ActiveShard prev) {
        super(prev);
        inactive = prev.inactive;
        actorAccess = prev.actorAccess;
        frontendMeta = prev.frontendMeta;
        shardStatsBean = prev.shardStatsBean;
    }

    // FIXME: this currently aliases to RaftActor, but we really would like to separate this out to mean shardActor
    final ActorRef self() {
        return actorAccess.raftActor();
    }

    @Override
    public final boolean handleRaftActorMessage(final Object message, final @Nullable ActorRef sender) {
        switch (message) {
            case FollowerInitialSyncUpStatus req -> {
                shardStatsBean.setFollowerInitialSyncStatus(req.isInitialSyncDone());
                // FIXME: Shard -> ShardManager. This should probably be routed from RaftActor
                // context().parent().tell(message, self());
            }
            // FIXME: these needs a better message, one with replyTo():
            case GetKnownClients req -> sender.tell(new GetKnownClientsReply(getClients()), self());
            case MakeLeaderLocal req -> {
                LOG.debug("{}: onMakeLeaderLocal received", id);
                handleMakeLeaderLocal(req, sender);
            }

            // modern shard things
            case ConnectClientRequest req -> req.getReplyTo().tell(handleConnectClient(req), ActorRef.noSender());
            // FIXME: DatastoreContext
            case RequestEnvelope req -> handleRequest(req);
            default -> {
                return false;
            }
        }
        return true;
    }

    ImmutableSet<ClientIdentifier> getClients() {
        return frontendMeta.getClients();
    }

    // overridden in LeaderShard
    Response<ClientIdentifier, ?> handleConnectClient(final ConnectClientRequest message) {
        LOG.info("{}: not currently leader, rejecting request {}.", id, message);
        return message.toRequestFailure(new NotLeaderException(self()));
    }

    void handleMakeLeaderLocal(final MakeLeaderLocal message, final ActorRef sender) {
        // Leader is not present. The cluster is most likely trying to
        // elect a leader and we should let that run its normal course

        // TODO we can wait for the election to complete and retry the
        // request. We can also let the caller retry by sending a flag
        // in the response indicating the request is "reTryable".
        sender.tell(new Failure(
            new LeadershipTransferFailedException(
                "We cannot initiate leadership transfer to local node. Currently there is no leader for " + id)),
            self());
    }

    private void handleRequest(final RequestEnvelope envelope) {
        final long now = ticker.read();
        try {
            final var success = handleRequest(envelope, now);
            if (success != null) {
                final long executionTime = tickerElapsed(now);
//                if (success instanceof SliceableMessage) {
//                    dispatchers.getDispatcher(DispatcherType.Serialization).execute(() ->
//                        responseMessageSlicer.slice(SliceOptions.builder().identifier(success.getTarget())
//                            .message(envelope.newSuccessEnvelope(success, executionTimeNanos))
//                            .sendTo(envelope.getMessage().getReplyTo()).replyTo(self())
//                            .onFailureCallback(t -> LOG.warn("Error slicing response {}", success, t)).build()));
//                } else {
                envelope.sendSuccess(success, executionTime);
//                }
            }
        } catch (RequestException e) {
            LOG.debug("{}: request {} failed", id, envelope, e);
            envelope.sendFailure(e, tickerElapsed(now));
        } catch (Exception e) {
            LOG.debug("{}: request {} caused failure", id, envelope, e);
            envelope.sendFailure(new RuntimeRequestException("Request failed to process", e), tickerElapsed(now));
        } finally {
            if (LOG.isTraceEnabled()) {
                LOG.trace("{}: request handled in {}ns", id, tickerElapsed(now));
            }
        }
    }

    // Default overridden in LeaderShard
    @Nullable RequestSuccess<?, ?> handleRequest(final RequestEnvelope envelope, final long now)
            throws RequestException {
        // we are not the leader
        LOG.debug("{}: not currently active leader, rejecting request {}", id, envelope);
        throw new NotLeaderException(self());
    }

    InactiveState stop() {
        shardStatsBean.unregisterMBean();
        // FIXME:
        //        listenerInfoMXBean.unregister();
        return inactive;
    }
}
