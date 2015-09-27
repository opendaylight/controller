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
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.slf4j.Logger;

/**
 * Handles server configuration related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorServerConfigurationSupport {
    private final RaftActorContext context;
    private final Logger log;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.context = context;
        this.log = context.getLogger();
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
        log.debug("onAddServer: {}", addServer);

        if(noLeaderOrForwardedToLeader(addServer, raftActor, sender)) {
            return;
        }

        // TODO - check if a server config is in progress. If so, cache this AddServer request to be processed
        // after the current one is done.

        context.addToPeers(addServer.getNewServerId(), addServer.getNewServerAddress());

        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        leader.addUninitializedFollower(addServer.getNewServerId());

        // TODO
        // Initiate snapshot via leader.initiateCaptureSnapshot(addServer.getNewServerId());
        // Start a timer to abort the operation after a period of time (maybe 2 times election timeout)
        // Set local instance state and wait for message from the AbstractLeader when install snapshot is done and return now
        // When install snapshot message is received,
        //    1) tell AbstractLeader mark the follower as initialized and recalculate minReplicationCount and
        //        minIsolatedLeaderPeerCount
        //    3) persist and replicate ServerConfigurationPayload via
        //           raftActor.persistData(sender, uuid, newServerConfigurationPayload)
        //    4) Wait for commit complete via ApplyState message in RaftActor or time it out. In RaftActor,
        //       on ApplyState, check if ReplicatedLogEntry payload is ServerConfigurationPayload and call
        //       this class.
        //

        // TODO - temporary
        sender.tell(new AddServerReply(ServerChangeStatus.OK, raftActor.getLeaderId()), raftActor.self());
    }

    private boolean noLeaderOrForwardedToLeader(Object message, RaftActor raftActor, ActorRef sender) {
        if (raftActor.isLeader()) {
            return false;
        }

        ActorSelection leader = raftActor.getLeader();
        if (leader != null) {
            log.debug("Not leader - forwarding to leader {}", leader);
            leader.forward(message, raftActor.getContext());
        } else {
            log.debug("No leader - returning NO_LEADER AddServerReply");
            sender.tell(new AddServerReply(ServerChangeStatus.NO_LEADER, null), raftActor.self());
        }

        return true;
    }
}
