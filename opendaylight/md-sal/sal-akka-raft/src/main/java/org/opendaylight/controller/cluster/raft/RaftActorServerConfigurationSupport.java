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
import org.opendaylight.controller.cluster.raft.messages.ProcessAddServerRequest;
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
    private Queue<CatchupFollowerInfo> followerInfoQueue = new LinkedList<CatchupFollowerInfo>();
    // flag to indicate current server config in-progress
    private boolean isServerConfigurationInProgress = false;
    // follower data sync timeout
    private static FiniteDuration FOLLOWER_CATCH_UP_TIMEOUT;
    // timeout handle
    private Cancellable followerTimeout = null;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.context = context;
        long timeInterval = (context.getConfigParams().getElectionTimeOutInterval().toMillis()) * 2;
        FOLLOWER_CATCH_UP_TIMEOUT = new FiniteDuration(3000, TimeUnit.MILLISECONDS);
    }

    // mantain sender actorRef
    private class CatchupFollowerInfo {
        private AddServer addServer;
        private ActorRef clientRequestor;

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

    boolean handleMessage(Object message, RaftActor raftActor, ActorRef sender) {
        if(message instanceof AddServer) {
            onAddServer((AddServer)message, raftActor, sender);
            return true;
        } else if (message instanceof FollowerCatchUpTimeout){
            FollowerCatchUpTimeout followerTimeout  = (FollowerCatchUpTimeout)message;
            // abort follower catchup on timeout
            abortFollowerCatchUp(raftActor,sender,followerTimeout.getNewServerId());
            return true;
        } else if (message instanceof UnInitializedFollowerSnapshotReply){
            // snapshot installation is successful
            handleCatchupFollowerSnapshotReply((UnInitializedFollowerSnapshotReply)message, raftActor,sender);
            return true;
        } else if(message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;
            Payload data = applyState.getReplicatedLogEntry().getData();
            if( data instanceof ServerConfigurationPayload){
                 LOG.info("Server configuration : {} is replicated of majority of cluster servers succesfully",
                                                                                    (ServerConfigurationPayload)data);
                 // respond ok to follower
                 respondToClient(raftActor, ServerChangeStatus.OK);
            }
            return true;
        } else if(message instanceof ProcessAddServerRequest){
            // process self message posted to handle pending addserver req's in queue
            processAddServer(raftActor, sender);
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
            processAddServer(raftActor,sender);
        }
    }

    private void processAddServer(RaftActor raftActor,ActorRef sender){
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
        leader.initiateCaptureSnapshot(addSrv.getNewServerId());
        // schedule the catchup timeout timer
        followerTimeout = context.getActorSystem().scheduler().scheduleOnce(FOLLOWER_CATCH_UP_TIMEOUT,
            context.getActor(), new FollowerCatchUpTimeout(addSrv.getNewServerId()),
            context.getActorSystem().dispatcher(), context.getActor());

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

    private void handleCatchupFollowerSnapshotReply(UnInitializedFollowerSnapshotReply reply,
                                                 RaftActor raftActor,ActorRef sender){
        LOG.debug("handleCatchupFollowerSnapshotReply : {}",  reply);

        String followerId = reply.getFollowerId();
        LOG.info("Succesfully installed snapshot on server: {}",  followerId);
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        FollowerLogInformation followerLogInformation = leader.getFollower(followerId);
        stopFollowerTimer();
        CatchupFollowerInfo fInfo = followerInfoQueue.peek();
        // change the follower state only if it is a voting member
        // for non-voting, no state change required as it is initially set
        if(fInfo.getAddServer().isVotingMember()){
              followerLogInformation.setFollowerState(FollowerState.VOTING);
              leader.reCalculateMinReplicaCntAndMinIsolatedLeaderPeerCnt();
        }
        /* get old server configuration list */
        Map<String, String> tempMap =  context.getPeerAddresses();
        List<String> cOld = new ArrayList<String>();
        for(String serverId : tempMap.values()){
            if(!serverId.equals(followerId)){
                cOld.add(serverId);
            }
        }
        /* get new server configuration list */
        List <String> cNew = new ArrayList<String>(cOld);
        cNew.add(followerId);
        // construct the peer list
        ServerConfigurationPayload servPayload = new ServerConfigurationPayload(cOld, cNew);
        LOG.info("Persisting new server configuration : {}",  servPayload);
        /* TODO - persist new configuration - CHECK WHETHER USING getId below is correct */
        raftActor.persistData(context.getActor(),context.getId(),servPayload);
   }

   private void stopFollowerTimer() {
        if (followerTimeout != null && !followerTimeout.isCancelled()) {
            followerTimeout.cancel();
        }
   }

   private void abortFollowerCatchUp(RaftActor raftActor,ActorRef sender, String serverId){

        LOG.debug("abortFollowerCatchUp: {}",  serverId);
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        // cleanup
        context.removePeer(serverId);
        leader.removeUninitializedFollower(serverId);
        LOG.info("abortFollowerCatchUp - Timeout occured for server - {} while installing snapshot", serverId);
        respondToClient(raftActor,ServerChangeStatus.TIMEOUT);
   }

   private void respondToClient(RaftActor raftActor,ServerChangeStatus result){

        LOG.debug("In respondToClient");
        // remove the entry from the queue
        CatchupFollowerInfo fInfo = followerInfoQueue.remove();
        // get the sender
        ActorRef toClient = fInfo.getClientRequestor();

        if(result == ServerChangeStatus.OK){
            LOG.info("respondToClient SUCCESS : {} ",  fInfo.getAddServer().getNewServerId());
            toClient.tell(new AddServerReply(ServerChangeStatus.OK, raftActor.getLeaderId()), raftActor.self());
        }else{
            toClient.tell(new AddServerReply(ServerChangeStatus.TIMEOUT, raftActor.getLeaderId()), raftActor.self());
        }
        // reset the instance flag
        isServerConfigurationInProgress = false;
        if(!followerInfoQueue.isEmpty()){
             raftActor.self().tell(new ProcessAddServerRequest(),raftActor.self());
        }
   }
}
