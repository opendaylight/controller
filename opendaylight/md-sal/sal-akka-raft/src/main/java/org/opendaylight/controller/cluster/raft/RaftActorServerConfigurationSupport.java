/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation.FollowerState;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.cluster.raft.messages.FollowerCatchUpTimeout;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles server configuration related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorServerConfigurationSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupport.class);
    private final RaftActorContext context;
    // client follower queue
    private final Queue<CatchupFollowerInfo> followerInfoQueue = new LinkedList<CatchupFollowerInfo>();
    // flag to indicate current server config in-progress
    private boolean isServerConfigurationInProgress = false;
    // follower data sync timeout
    private final FiniteDuration FOLLOWER_CATCH_UP_TIMEOUT;
    // timeout handle
    private Cancellable followerTimeout = null;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.context = context;
        long timeInterval = (context.getConfigParams().getElectionTimeOutInterval().toMillis()) * 2;
        //TODO - Need to replace below hard coding to timeInterval
        FOLLOWER_CATCH_UP_TIMEOUT = new FiniteDuration(60000, TimeUnit.MILLISECONDS);
    }

    boolean handleMessage(Object message, RaftActor raftActor, ActorRef sender) {
        if(message instanceof AddServer) {
            onAddServer((AddServer)message, raftActor, sender);
            return true;
        } else if (message instanceof FollowerCatchUpTimeout){
            FollowerCatchUpTimeout followerTimeout  = (FollowerCatchUpTimeout)message;
            // abort follower catchup on timeout
            onFollowerCatchupTimeout(raftActor, sender, followerTimeout.getNewServerId());
            return true;
        } else if (message instanceof UnInitializedFollowerSnapshotReply){
            // snapshot installation is successful
            onUnInitializedFollowerSnapshotReply((UnInitializedFollowerSnapshotReply)message, raftActor,sender);
            return true;
        } else if(message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;
            Payload data = applyState.getReplicatedLogEntry().getData();
            if( data instanceof ServerConfigurationPayload){
                 LOG.info("Server configuration : {} has been replicated to a majority of cluster servers succesfully",
                                                                                    (ServerConfigurationPayload)data);
                 // respond ok to follower
                 respondToClient(raftActor, ServerChangeStatus.OK);
            }
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
        // cache AddServer request to be processed
        CatchupFollowerInfo followerInfo = new CatchupFollowerInfo(addServer,sender);
        followerInfoQueue.add(followerInfo);
        // process the request, if no server config in-progress
        if(!isServerConfigurationInProgress){
            processAddServer(raftActor);
        }
    }

    private void processAddServer(RaftActor raftActor){
        LOG.debug("In processAddServer");
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        // set server config in-progress
        isServerConfigurationInProgress = true;
        AddServer addSrv = followerInfoQueue.peek().getAddServer();
        context.addToPeers(addSrv.getNewServerId(), addSrv.getNewServerAddress());

        // if voting member - initialize to VOTING_NOT_INITIALIZED
        FollowerState initialState = addSrv.isVotingMember() ? FollowerState.VOTING_NOT_INITIALIZED :
            FollowerState.NON_VOTING;
        leader.addFollower(addSrv.getNewServerId(), initialState);

        // TODO
        // if initialState == FollowerState.VOTING_NOT_INITIALIZED
        //     Initiate snapshot via leader.initiateCaptureSnapshot(addServer.getNewServerId())
        //     Start a timer to abort the operation after a period of time (maybe 2 times election timeout)
        //     Set local instance state and wait for message from the AbstractLeader when install snapshot
        //     is done and return now
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
        if(initialState == FollowerState.VOTING_NOT_INITIALIZED){
            LOG.debug("Leader sending initiate capture snapshot to follower : {}", addSrv.getNewServerId());
            leader.initiateCaptureSnapshot(addSrv.getNewServerId());
            // schedule the catchup timeout timer
            followerTimeout = context.getActorSystem().scheduler().scheduleOnce(FOLLOWER_CATCH_UP_TIMEOUT,
                context.getActor(), new FollowerCatchUpTimeout(addSrv.getNewServerId()),
                context.getActorSystem().dispatcher(), context.getActor());
        } else {
            LOG.debug("Directly persisting  the new server configuration : {}", addSrv.getNewServerId());
            persistNewServerConfiguration(raftActor, followerInfoQueue.peek().getClientRequestor(),
                                                                                 addSrv.getNewServerId());
        }
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
            sender.tell(new AddServerReply(ServerChangeStatus.NO_LEADER, null), raftActor.self());
        }

        return true;
    }

    private void onUnInitializedFollowerSnapshotReply(UnInitializedFollowerSnapshotReply reply,
                                                 RaftActor raftActor, ActorRef sender){

        String followerId = reply.getFollowerId();
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        FollowerLogInformation followerLogInformation = leader.getFollower(followerId);
        stopFollowerTimer();
        followerLogInformation.setFollowerState(FollowerState.VOTING);
        leader.updateMinReplicaCountAndMinIsolatedLeaderPeerCount();
        persistNewServerConfiguration(raftActor, sender, followerId);
    }

    private void persistNewServerConfiguration(RaftActor raftActor, ActorRef sender, String followerId){
        /* get old server configuration list */
        Map<String, String> tempMap =  context.getPeerAddresses();
        List<String> cOld = new ArrayList<String>();
        for (Map.Entry<String, String> entry : tempMap.entrySet()) {
            if(!entry.getKey().equals(followerId)){
                cOld.add(entry.getKey());
            }
        }
        LOG.debug("Cold server configuration : {}",  cOld.toString());
        /* get new server configuration list */
        List <String> cNew = new ArrayList<String>(cOld);
        cNew.add(followerId);
        LOG.debug("Cnew server configuration : {}",  cNew.toString());
        // construct the peer list
        ServerConfigurationPayload servPayload = new ServerConfigurationPayload(cOld, cNew);
        /* TODO - persist new configuration - CHECK WHETHER USING getId below is correct */
        raftActor.persistData(sender, context.getId(), servPayload);
   }

   private void stopFollowerTimer() {
        if (followerTimeout != null && !followerTimeout.isCancelled()) {
            followerTimeout.cancel();
        }
   }

   private void onFollowerCatchupTimeout(RaftActor raftActor, ActorRef sender, String serverId){

        LOG.debug("onFollowerCatchupTimeout: {}",  serverId);
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        // cleanup
        context.removePeer(serverId);
        leader.removeFollower(serverId);
        LOG.warn("onFollowerCatchupTimeout - Timeout occured for server - {} while installing snapshot", serverId);
        respondToClient(raftActor,ServerChangeStatus.TIMEOUT);
   }

   private void respondToClient(RaftActor raftActor, ServerChangeStatus result){

        int size = followerInfoQueue.size();

        // remove the entry from the queue
        CatchupFollowerInfo fInfo = followerInfoQueue.remove();
        // get the sender
        ActorRef toClient = fInfo.getClientRequestor();

        toClient.tell(new AddServerReply(result, raftActor.getLeaderId()), raftActor.self());
        LOG.debug("Response returned is {} for server {} ",  result, fInfo.getAddServer().getNewServerId());
        // reset the instance flag
        isServerConfigurationInProgress = false;
        if(!followerInfoQueue.isEmpty()){
            processAddServer(raftActor);
        }
   }

    // mantain sender actorRef
    private class CatchupFollowerInfo {
        private final AddServer addServer;
        private final ActorRef clientRequestor;

        CatchupFollowerInfo(AddServer addSrv, ActorRef cliReq){
            addServer = addSrv;
            clientRequestor = cliReq;
        }
        public AddServer getAddServer(){
            return addServer;
        }
        public ActorRef getClientRequestor(){
            return clientRequestor;
        }
    }
}
