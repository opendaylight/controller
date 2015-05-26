/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.cluster.Cluster;
import akka.cluster.ClusterActorRefProvider;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Gossiper that syncs bucket store across nodes in the cluster.
 * <p/>
 * It keeps a local scheduler that periodically sends Gossip ticks to
 * itself to send bucket store's bucket versions to a randomly selected remote
 * gossiper.
 * <p/>
 * When bucket versions are received from a remote gossiper, it is compared
 * with bucket store's bucket versions. Which ever buckets are newer
 * locally, are sent to remote gossiper. If any bucket is older in bucket store,
 * a gossip status is sent to remote gossiper so that it can send the newer buckets.
 * <p/>
 * When a bucket is received from a remote gossiper, its sent to the bucket store
 * for update.
 *
 */

public class Gossiper extends AbstractUntypedActorWithMetering {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Cluster cluster;

    /**
     * ActorSystem's address for the current cluster node.
     */
    private Address selfAddress;

    /**
     * All known cluster members
     */
    private List<Address> clusterMembers = new ArrayList<>();

    private Cancellable gossipTask;

    private Boolean autoStartGossipTicks = true;

    private RemoteRpcProviderConfig config;

    public Gossiper(){
        config = new RemoteRpcProviderConfig(getContext().system().settings().config());
    }

    /**
     * Helpful for testing
     * @param autoStartGossipTicks used for turning off gossip ticks during testing.
     *                             Gossip tick can be manually sent.
     */
    public Gossiper(Boolean autoStartGossipTicks){
        this.autoStartGossipTicks = autoStartGossipTicks;
    }

    @Override
    public void preStart(){
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if ( provider instanceof ClusterActorRefProvider ) {
            cluster = Cluster.get(getContext().system());
            cluster.subscribe(getSelf(),
                    ClusterEvent.initialStateAsEvents(),
                    ClusterEvent.MemberEvent.class,
                    ClusterEvent.UnreachableMember.class);
        }

        if (autoStartGossipTicks) {
            gossipTask = getContext().system().scheduler().schedule(
                    new FiniteDuration(1, TimeUnit.SECONDS),        //initial delay
                    config.getGossipTickInterval(),                 //interval
                    getSelf(),                                       //target
                    new Messages.GossiperMessages.GossipTick(),      //message
                    getContext().dispatcher(),                       //execution context
                    getSelf()                                        //sender
            );
        }
    }

    @Override
    public void postStop(){
        if (cluster != null) {
            cluster.unsubscribe(getSelf());
        }
        if (gossipTask != null) {
            gossipTask.cancel();
        }
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        //Usually sent by self via gossip task defined above. But its not enforced.
        //These ticks can be sent by another actor as well which is esp. useful while testing
        if (message instanceof GossipTick) {
            receiveGossipTick();
        } else if (message instanceof GossipStatus) {
            receiveGossipStatus((GossipStatus) message);
        } else if (message instanceof GossipEnvelope) {
            receiveGossip((GossipEnvelope) message);
        } else if (message instanceof ClusterEvent.MemberUp) {
            receiveMemberUp(((ClusterEvent.MemberUp) message).member());

        } else if (message instanceof ClusterEvent.MemberRemoved) {
            receiveMemberRemoveOrUnreachable(((ClusterEvent.MemberRemoved) message).member());

        } else if ( message instanceof ClusterEvent.UnreachableMember){
            receiveMemberRemoveOrUnreachable(((ClusterEvent.UnreachableMember) message).member());

        } else {
            unhandled(message);
        }
    }

    /**
     * Remove member from local copy of member list. If member down is self, then stop the actor
     *
     * @param member who went down
     */
    void receiveMemberRemoveOrUnreachable(Member member) {
        //if its self, then stop itself
        if (selfAddress.equals(member.address())){
            getContext().stop(getSelf());
            return;
        }

        clusterMembers.remove(member.address());
        if(log.isDebugEnabled()) {
            log.debug("Removed member [{}], Active member list [{}]", member.address(), clusterMembers);
        }
    }

    /**
     * Add member to the local copy of member list if it doesnt already
     * @param member
     */
    void receiveMemberUp(Member member) {

        if (selfAddress.equals(member.address()))
         {
            return; //ignore up notification for self
        }

        if (!clusterMembers.contains(member.address())) {
            clusterMembers.add(member.address());
        }
        if(log.isDebugEnabled()) {
            log.debug("Added member [{}], Active member list [{}]", member.address(), clusterMembers);
        }
    }

    /**
     * Sends Gossip status to other members in the cluster. <br/>
     * 1. If there are no member, ignore the tick. </br>
     * 2. If there's only 1 member, send gossip status (bucket versions) to it. <br/>
     * 3. If there are more than one member, randomly pick one and send gossip status (bucket versions) to it.
     */
    void receiveGossipTick(){
        if (clusterMembers.size() == 0)
         {
            return; //no members to send gossip status to
        }

        Address remoteMemberToGossipTo;

        if (clusterMembers.size() == 1) {
            remoteMemberToGossipTo = clusterMembers.get(0);
        } else {
            Integer randomIndex = ThreadLocalRandom.current().nextInt(0, clusterMembers.size());
            remoteMemberToGossipTo = clusterMembers.get(randomIndex);
        }
        if(log.isDebugEnabled()) {
            log.debug("Gossiping to [{}]", remoteMemberToGossipTo);
        }
        getLocalStatusAndSendTo(remoteMemberToGossipTo);
    }

    /**
     * Process gossip status received from a remote gossiper. Remote versions are compared with
     * the local copy. <p>
     *
     * For each bucket
     * <ul>
     *  <li>If local copy is newer, the newer buckets are sent in GossipEnvelope to remote</li>
     *  <li>If local is older, GossipStatus is sent to remote so that it can reply with GossipEnvelope</li>
     *  <li>If both are same, noop</li>
     * </ul>
     *
     * @param status bucket versions from a remote member
     */
    void receiveGossipStatus(GossipStatus status){
        //Don't accept messages from non-members
        if (!clusterMembers.contains(status.from())) {
            return;
        }

        final ActorRef sender = getSender();
        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketVersions(), config.getAskDuration());

        futureReply.map(getMapperToProcessRemoteStatus(sender, status), getContext().dispatcher());

    }

    /**
     * Sends the received buckets in the envelope to the parent Bucket store.
     *
     * @param envelope contains buckets from a remote gossiper
     */
    void receiveGossip(GossipEnvelope envelope){
        //TODO: Add more validations
        if (!selfAddress.equals(envelope.to())) {
            if(log.isDebugEnabled()) {
                log.debug("Ignoring message intended for someone else. From [{}] to [{}]", envelope.from(), envelope.to());
            }
            return;
        }

        updateRemoteBuckets(envelope.getBuckets());

    }

    /**
     * Helper to send received buckets to bucket store
     *
     * @param buckets
     */
    void updateRemoteBuckets(Map<Address, Bucket> buckets) {

        UpdateRemoteBuckets updateRemoteBuckets = new UpdateRemoteBuckets(buckets);
        getContext().parent().tell(updateRemoteBuckets, getSelf());
    }

    /**
     * Gets the buckets from bucket store for the given node addresses and sends them to remote gossiper
     *
     * @param remote     remote node to send Buckets to
     * @param addresses  node addresses whose buckets needs to be sent
     */
    void sendGossipTo(final ActorRef remote, final Set<Address> addresses){

        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketsByMembers(addresses), config.getAskDuration());
        futureReply.map(getMapperToSendGossip(remote), getContext().dispatcher());
    }

    /**
     * Gets bucket versions from bucket store and sends to the supplied address
     *
     * @param remoteActorSystemAddress remote gossiper to send to
     */
    void getLocalStatusAndSendTo(Address remoteActorSystemAddress){

        //Get local status from bucket store and send to remote
        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketVersions(), config.getAskDuration());

        //Find gossiper on remote system
        ActorSelection remoteRef = getContext().system().actorSelection(
                remoteActorSystemAddress.toString() + getSelf().path().toStringWithoutAddress());

        if(log.isDebugEnabled()) {
            log.debug("Sending bucket versions to [{}]", remoteRef);
        }

        futureReply.map(getMapperToSendLocalStatus(remoteRef), getContext().dispatcher());

    }

    /**
     * Helper to send bucket versions received from local store
     * @param remote        remote gossiper to send versions to
     * @param localVersions bucket versions received from local store
     */
    void sendGossipStatusTo(ActorRef remote, Map<Address, Long> localVersions){

        GossipStatus status = new GossipStatus(selfAddress, localVersions);
        remote.tell(status, getSelf());
    }

    void sendGossipStatusTo(ActorSelection remote, Map<Address, Long> localVersions){

        GossipStatus status = new GossipStatus(selfAddress, localVersions);
        remote.tell(status, getSelf());
    }

    ///
    /// Private factories to create mappers
    ///

    private Mapper<Object, Void> getMapperToSendLocalStatus(final ActorSelection remote){

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {
                if (replyMessage instanceof GetBucketVersionsReply) {
                    GetBucketVersionsReply reply = (GetBucketVersionsReply) replyMessage;
                    Map<Address, Long> localVersions = reply.getVersions();

                    sendGossipStatusTo(remote, localVersions);

                }
                return null;
            }
        };
    }

    /**
     * Process bucket versions received from
     * {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore}.
     * Then this method compares remote bucket versions with local bucket versions.
     * <ul>
     *     <li>The buckets that are newer locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope}
     *     to remote
     *     <li>The buckets that are older locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus}
     *     to remote so that remote sends GossipEnvelop.
     * </ul>
     *
     * @param sender the remote member
     * @param status bucket versions from a remote member
     * @return a {@link akka.dispatch.Mapper} that gets evaluated in future
     *
     */
    private Mapper<Object, Void> getMapperToProcessRemoteStatus(final ActorRef sender, final GossipStatus status){

        final Map<Address, Long> remoteVersions = status.getVersions();

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {
                if (replyMessage instanceof GetBucketVersionsReply) {
                    GetBucketVersionsReply reply = (GetBucketVersionsReply) replyMessage;
                    Map<Address, Long> localVersions = reply.getVersions();

                    //diff between remote list and local
                    Set<Address> localIsOlder = new HashSet<>();
                    localIsOlder.addAll(remoteVersions.keySet());
                    localIsOlder.removeAll(localVersions.keySet());

                    //diff between local list and remote
                    Set<Address> localIsNewer = new HashSet<>();
                    localIsNewer.addAll(localVersions.keySet());
                    localIsNewer.removeAll(remoteVersions.keySet());


                    for (Address address : remoteVersions.keySet()){

                        if (localVersions.get(address) == null || remoteVersions.get(address) == null)
                         {
                            continue; //this condition is taken care of by above diffs
                        }
                        if (localVersions.get(address) <  remoteVersions.get(address)) {
                            localIsOlder.add(address);
                        } else if (localVersions.get(address) > remoteVersions.get(address)) {
                            localIsNewer.add(address);
                        }
                    }

                    if (!localIsOlder.isEmpty()) {
                        sendGossipStatusTo(sender, localVersions );
                    }

                    if (!localIsNewer.isEmpty())
                     {
                        sendGossipTo(sender, localIsNewer);//send newer buckets to remote
                    }

                }
                return null;
            }
        };
    }

    /**
     * Processes the message from {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore}
     * that contains {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket}.
     * These buckets are sent to a remote member encapsulated in
     * {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope}
     *
     * @param sender the remote member that sent
     *               {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus}
     *               in reply to which bucket is being sent back
     * @return a {@link akka.dispatch.Mapper} that gets evaluated in future
     *
     */
    private Mapper<Object, Void> getMapperToSendGossip(final ActorRef sender) {

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object msg) {
                if (msg instanceof GetBucketsByMembersReply) {
                    Map<Address, Bucket> buckets = ((GetBucketsByMembersReply) msg).getBuckets();
                    if(log.isDebugEnabled()) {
                        log.debug("Buckets to send from {}: {}", selfAddress, buckets);
                    }
                    GossipEnvelope envelope = new GossipEnvelope(selfAddress, sender.path().address(), buckets);
                    sender.tell(envelope, getSelf());
                }
                return null;
            }
        };
    }

    ///
    ///Getter Setters
    ///
    List<Address> getClusterMembers() {
        return clusterMembers;
    }

    void setClusterMembers(List<Address> clusterMembers) {
        this.clusterMembers = clusterMembers;
    }

    Address getSelfAddress() {
        return selfAddress;
    }

    void setSelfAddress(Address selfAddress) {
        this.selfAddress = selfAddress;
    }
}
