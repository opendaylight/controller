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
import akka.actor.Cancellable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation.FollowerState;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.FollowerCatchUpTimeout;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // timeout handle
    private Cancellable followerTimeout = null;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.context = context;
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
        } else if(message instanceof ApplyState) {
            return onApplyState((ApplyState) message, raftActor);
        } else {
            return false;
        }
    }

    private boolean onApplyState(ApplyState applyState, RaftActor raftActor) {
        Payload data = applyState.getReplicatedLogEntry().getData();
        if(data instanceof ServerConfigurationPayload) {
            CatchupFollowerInfo followerInfo = followerInfoQueue.peek();
            if(followerInfo != null && followerInfo.getContextId().equals(applyState.getIdentifier())) {
                LOG.info("{} has been successfully replicated to a majority of followers", data);

                // respond ok to follower
                respondToClient(raftActor, ServerChangeStatus.OK);
            }

            return true;
        }

        return false;
    }

    private void onAddServer(AddServer addServer, RaftActor raftActor, ActorRef sender) {
        LOG.debug("{}: onAddServer: {}", context.getId(), addServer);
        if(noLeaderOrForwardedToLeader(addServer, raftActor, sender)) {
            return;
        }

        CatchupFollowerInfo followerInfo = new CatchupFollowerInfo(addServer,sender);
        boolean process = followerInfoQueue.isEmpty();
        followerInfoQueue.add(followerInfo);
        if(process) {
            processAddServer(raftActor);
        }
    }

    /**
     * The algorithm for AddServer is as follows:
     * <ul>
     * <li>Add the new server as a peer.</li>
     * <li>Add the new follower to the leader.</li>
     * <li>If new server should be voting member</li>
     * <ul>
     *     <li>Initialize FollowerState to VOTING_NOT_INITIALIZED.</li>
     *     <li>Initiate install snapshot to the new follower.</li>
     *     <li>When install snapshot complete, mark the follower as VOTING and re-calculate majority vote count.</li>
     * </ul>
     * <li>Persist and replicate ServerConfigurationPayload with the new server list.</li>
     * <li>On replication consensus, respond to caller with OK.</li>
     * </ul>
     * If the install snapshot times out after a period of 2 * election time out
     * <ul>
     *     <li>Remove the new server as a peer.</li>
     *     <li>Remove the new follower from the leader.</li>
     *     <li>Respond to caller with TIMEOUT.</li>
     * </ul>
     */
    private void processAddServer(RaftActor raftActor){
        LOG.debug("{}: In processAddServer", context.getId());

        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        CatchupFollowerInfo followerInfo = followerInfoQueue.peek();
        AddServer addSrv = followerInfo.getAddServer();
        context.addToPeers(addSrv.getNewServerId(), addSrv.getNewServerAddress());

        // if voting member - initialize to VOTING_NOT_INITIALIZED
        FollowerState initialState = addSrv.isVotingMember() ? FollowerState.VOTING_NOT_INITIALIZED :
            FollowerState.NON_VOTING;
        leader.addFollower(addSrv.getNewServerId(), initialState);

        if(initialState == FollowerState.VOTING_NOT_INITIALIZED){
            LOG.debug("Leader sending initiate capture snapshot to follower : {}", addSrv.getNewServerId());
            leader.initiateCaptureSnapshot(addSrv.getNewServerId());
            // schedule the catchup timeout timer
            followerTimeout = context.getActorSystem().scheduler()
               .scheduleOnce(new FiniteDuration(((context.getConfigParams().getElectionTimeOutInterval().toMillis()) * 2),
                TimeUnit.MILLISECONDS),
                context.getActor(), new FollowerCatchUpTimeout(addSrv.getNewServerId()),
                context.getActorSystem().dispatcher(), context.getActor());
        } else {
            LOG.debug("Directly persisting  the new server configuration : {}", addSrv.getNewServerId());
            persistNewServerConfiguration(raftActor, followerInfo);
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
        CatchupFollowerInfo followerInfo = followerInfoQueue.peek();
        // Sanity check - it's possible we get a reply after it timed out.
        if(followerInfo == null) {
            return;
        }

        String followerId = reply.getFollowerId();
        AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
        FollowerLogInformation followerLogInformation = leader.getFollower(followerId);
        stopFollowerTimer();
        followerLogInformation.setFollowerState(FollowerState.VOTING);
        leader.updateMinReplicaCountAndMinIsolatedLeaderPeerCount();

        persistNewServerConfiguration(raftActor, followerInfo);
    }

    private void persistNewServerConfiguration(RaftActor raftActor, CatchupFollowerInfo followerInfo){
        List <String> cNew = new ArrayList<String>(context.getPeerAddresses().keySet());
        cNew.add(context.getId());

        LOG.debug("New server configuration : {}",  cNew.toString());

        ServerConfigurationPayload servPayload = new ServerConfigurationPayload(cNew, Collections.<String>emptyList());

        raftActor.persistData(followerInfo.getClientRequestor(), followerInfo.getContextId(), servPayload);
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
        LOG.warn("Timeout occured for new server {} while installing snapshot", serverId);
        respondToClient(raftActor,ServerChangeStatus.TIMEOUT);
   }

   private void respondToClient(RaftActor raftActor, ServerChangeStatus result){
        // remove the entry from the queue
        CatchupFollowerInfo fInfo = followerInfoQueue.remove();

        // get the sender
        ActorRef toClient = fInfo.getClientRequestor();

        toClient.tell(new AddServerReply(result, raftActor.getLeaderId()), raftActor.self());
        LOG.debug("Response returned is {} for server {} ",  result, fInfo.getAddServer().getNewServerId());
        if(!followerInfoQueue.isEmpty()){
            processAddServer(raftActor);
        }
   }

    // maintain sender actorRef
    private static class CatchupFollowerInfo {
        private final AddServer addServer;
        private final ActorRef clientRequestor;
        private final String contextId;

        CatchupFollowerInfo(AddServer addSrv, ActorRef cliReq){
            addServer = addSrv;
            clientRequestor = cliReq;
            contextId = UUID.randomUUID().toString();
        }

        String getContextId() {
            return contextId;
        }

        AddServer getAddServer(){
            return addServer;
        }

        ActorRef getClientRequestor(){
            return clientRequestor;
        }
    }
}
