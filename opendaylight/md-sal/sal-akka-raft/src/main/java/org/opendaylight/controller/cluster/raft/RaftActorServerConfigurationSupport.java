/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation.FollowerState;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles server configuration related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorServerConfigurationSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupport.class);

    private final RaftActorContext context;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.context = context;
    }

    boolean handleMessage(Object message, RaftActor raftActor, ActorRef sender) {
        if(message instanceof AddServer) {
            onAddServer((AddServer)message, raftActor, sender);
            return true;
        } else {
            return false;
        }
    }

    private void onAddServer(AddServer addServer, RaftActor raftActor, ActorRef sender) {
        LOG.debug("onAddServer: {}", addServer);

        if(noLeaderOrForwardedToLeader(addServer, raftActor, sender)) {
            return;
        }

        // TODO - check if a server config is in progress. If so, cache this AddServer request to be processed
        // after the current one is done.

        context.addToPeers(addServer.getNewServerId(), addServer.getNewServerAddress());

        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        FollowerState initialState = addServer.isVotingMember() ? FollowerState.VOTING_NOT_INITIALIZED :
            FollowerState.NON_VOTING;
        leader.addFollower(addServer.getNewServerId(), initialState);

        // TODO
        // if initialState == FollowerState.VOTING_NOT_INITIALIZED
        //     Initiate snapshot via leader.initiateCaptureSnapshot(addServer.getNewServerId())
        //     Start a timer to abort the operation after a period of time (maybe 2 times election timeout)
        //     Set local instance state and wait for message from the AbstractLeader when install snapshot is done and return now
        //     When install snapshot message is received, go to step 1
        // else
        //     go to step 2
        //
        // 1) tell AbstractLeader mark the follower as VOTING and recalculate minReplicationCount and
        //        minIsolatedLeaderPeerCount
        // 2) persist and replicate ServerConfigurationPayload via
        //           raftActor.persistData(sender, uuid, newServerConfigurationPayload)
        // 3) Wait for commit complete via ApplyState message in RaftActor or time it out. In RaftActor,
        //       on ApplyState, check if ReplicatedLogEntry payload is ServerConfigurationPayload and call
        //       this class.
        //

        // TODO - temporary
        sender.tell(new AddServerReply(addServer.getNewServerId(), ServerChangeStatus.OK, raftActor.getLeaderId()), raftActor.self());
    }

    private boolean noLeaderOrForwardedToLeader(Object message, RaftActor raftActor, ActorRef sender) {
        if (raftActor.isLeader()) {
            return false;
        }

        ActorSelection leader = raftActor.getLeader();
        if (leader != null) {
            LOG.debug("Not leader - forwarding to leader {}", leader);
            leader.forward(message, raftActor.getContext());
        } else {
            LOG.debug("No leader - returning NO_LEADER AddServerReply");
            AddServer addServer = (AddServer) message;
            sender.tell(new AddServerReply(addServer.getNewServerId(), ServerChangeStatus.NO_LEADER, null), raftActor.self());
        }

        return true;
    }
}
