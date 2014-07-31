/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Option;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipTick;

/**
 *
 * Gossiper that syncs bucket store across nodes in the cluster
 */

public class Gossiper extends UntypedActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    Cluster cluster = Cluster.get(getContext().system());

    private List<Address> clusterMembers = new ArrayList<>();

    private Map<Address, ActorRef> remoteGossiperCache = new HashMap<>();

    private Cancellable gossipTask;

    private Address selfAddress = cluster.selfAddress(); //actor system's address

    private Boolean autoStartGossipTicks = true;

    public Gossiper(){}

    public Gossiper(Boolean autoStartGossipTicks){
        this.autoStartGossipTicks = autoStartGossipTicks;
    }

    @Override
    public void preStart(){

        cluster.subscribe(getSelf(),
                          ClusterEvent.initialStateAsEvents(),
                          ClusterEvent.MemberEvent.class,
                          ClusterEvent.UnreachableMember.class);

        if (autoStartGossipTicks) {
            gossipTask = getContext().system().scheduler().schedule(
                    new FiniteDuration(5, TimeUnit.SECONDS),        //initial delay
                    new FiniteDuration(5, TimeUnit.SECONDS),  //interval
                    getSelf(),                                       //target
                    new Messages.GossiperMessages.GossipTick(),      //message
                    getContext().dispatcher(),                       //execution context
                    getSelf()                                        //sender
            );
        }

        log.debug("Gossiper {} starting...", getSelf());
    }

    @Override
    public void postStop(){
        if (cluster != null)
            cluster.unsubscribe(getSelf());
        if (gossipTask != null)
            gossipTask.cancel();
    }

    @Override
    public void onReceive(Object message) throws Exception {

        //log.info("Received message: [{}]", message);

        if (message instanceof GossipTick)
            receiveGossipTick();

        else if (message instanceof GossipStatus) //remote versions
            receiveGossipStatus((GossipStatus) message);

        else if (message instanceof GossipEnvelope) //remote buckets, newer only
            receiveGossip((GossipEnvelope) message);

        else if (message instanceof ClusterEvent.MemberUp) {
            receiveMemberUp(((ClusterEvent.MemberUp) message).member());

        } else if (message instanceof ClusterEvent.MemberRemoved) {
            receiveMemberDown(((ClusterEvent.MemberRemoved) message).member());

        } else if ( message instanceof ClusterEvent.UnreachableMember){
            receiveMemberDown(((ClusterEvent.UnreachableMember) message).member());

        } else
            unhandled(message);
    }

    void receiveMemberDown(Member member) {
        log.info("Member is down/unreachable [{}]", member);
        if (member.address().equals(selfAddress)){
            getContext().stop(getSelf());
        }
        clusterMembers.remove(member.address());
        log.info("Member list [{}]", clusterMembers);
    }

    void receiveMemberUp(Member member) {
        log.info("Member is up [{}]", member);
        if (selfAddress.equals(member.address()))
            return; //ignore up notification for self

        if (!clusterMembers.contains(member.address()))
            clusterMembers.add(member.address());

        log.info("Member list [{}]", clusterMembers);
    }

    /**
     * Sends Gossip status to other members in the cluster. <br/>
     * 1. If there are no member, ignore the tick. </br>
     * 2. If there's only 1 member, send gossip status to it. <br/>
     * 3. If there are more than one member, randomly pick one and send gossip status to it.
     */
    void receiveGossipTick(){
        log.info("Cluster member list [{}]", clusterMembers);
        log.info("Cluster Size is [{}]", clusterMembers.size());
        if (clusterMembers.size() == 0) return; //no members to send gossip status to

        Address remoteMemberToGossipTo = null;

        if (clusterMembers.size() == 1)
            remoteMemberToGossipTo = clusterMembers.get(0);
        else {
            Integer randomIndex = ThreadLocalRandom.current().nextInt(0, clusterMembers.size());
            remoteMemberToGossipTo = clusterMembers.get(randomIndex);
        }

        log.info("Gossiping to [{}]", remoteMemberToGossipTo);
        getLocalStatusAndSendTo(remoteMemberToGossipTo);
    }

    /**
     * Receives remote status.
     *
     * @param status bucket versions from a remote member
     */
    void receiveGossipStatus(GossipStatus status){
        //Dont want to accept messages from non-members
        if (!clusterMembers.contains(status.from()))
            return;

        final ActorRef sender = getSender();

        Future<Object> futureReply = Patterns.ask(getContext().parent(), new GetBucketVersions(), 1000);

        futureReply.map(getMapperToProcessRemoteStatus(sender, status), getContext().dispatcher());

    }

    void receiveGossip(GossipEnvelope envelope){
        //TODO: Add more validations
        if (!selfAddress.equals(envelope.to())) {
            log.info("Ignoring message intended for someone else. From [{}] to [{}]", envelope.from(), envelope.to());
            log.info("Self address [{}]", selfAddress);
            return;
        }
        if (envelope.getBuckets() == null)
            return; //nothing to do

        updateRemoteBuckets(envelope.getBuckets());

    }

    void updateRemoteBuckets(Map<Address, Bucket> buckets) {

        log.info("Got remote buckets to update [{}]", buckets);

        if (buckets == null || buckets.isEmpty())
            return; //nothing to merge

        UpdateRemoteBuckets updateRemoteBuckets = new UpdateRemoteBuckets(buckets);

        getContext().parent().tell(updateRemoteBuckets, getSelf());
    }

    void sendGossipTo(final ActorRef remote, final Set<Address> addresses){

        log.info("On {} ask store to send buckets for {}", selfAddress, addresses);
        Future<Object> futureReply = Patterns.ask(getContext().parent(), new GetBucketsByMembers(addresses), 1000);

        futureReply.map(getMapperToSendGossip(remote), getContext().dispatcher());

    }

    void getLocalStatusAndSendTo(Address remoteActorSystemAddress){

        Option<ActorRef> potentialReference = getGossiperFor(remoteActorSystemAddress);

        if (potentialReference.isEmpty())
            return; //Could not find remote gossiper. Ignoring sending status

        ActorRef remoteRef = potentialReference.get();
        log.info("Got remote actor reference [{}]", remoteRef);

        //Get local status from bucket store and send to remote
        Future<Object> futureReply = Patterns.ask(getContext().parent(), new GetBucketVersions(), 1000);
        futureReply.map(getMapperToSendLocalStatus(remoteRef), getContext().dispatcher());

    }

    void sendGossipStatusTo(ActorRef remote, Map<Address, Long> localVersions){

        GossipStatus status = new GossipStatus(selfAddress, localVersions);
        remote.tell(status, getSelf());
    }

    Option<ActorRef> getGossiperFor(Address remoteActorSystem){

        ActorRef remoteGossiper = null;

        if (remoteGossiperCache.containsKey(remoteActorSystem))
            remoteGossiper = remoteGossiperCache.get(remoteActorSystem);

        if (remoteGossiper == null || remoteGossiper.isTerminated() ) {
            remoteGossiper = findGossiperFor(remoteActorSystem);
        }

        return Option.option(remoteGossiper);

    }

    ActorRef findGossiperFor(Address remoteActorSystem) {

        ActorRef remoteGossiper = null;

        ActorSelection remote = getContext().system().actorSelection(
                remoteActorSystem.toString() + getSelf().path().toStringWithoutAddress());

        Future<ActorRef> future = remote.resolveOne(new FiniteDuration(100, TimeUnit.MILLISECONDS));

        try {
            remoteGossiper = Await.result(future, new FiniteDuration(200, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.debug("Could not get reference to remote gossiper for address [{}]. Got exception [{}]",
                       remoteActorSystem, e);
        }

        if ( remoteGossiper == null) {
            log.error("Could not get reference to remote gossiper for address [{}]", remoteActorSystem);
        } else {
            remoteGossiperCache.put(remoteActorSystem, remoteGossiper);
        }
        return remoteGossiper;
    }

    ///
    /// Private factories to create mappers
    ///

    /**
     *
     * @param remote
     * @return
     */
    private Mapper<Object, Void> getMapperToSendLocalStatus(final ActorRef remote){

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
     * Process bucket versions received from {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore}.
     * Then this method compares remote bucket versions with local bucket versions.
     * <ul>
     *     <li>The buckets that are newer locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope} to remote
     *     <li>The buckets that are older locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus} to remote so that
     *     remote sends GossipEnvelop.
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

                    log.info("Local versions on {}: [{}]", selfAddress, localVersions);
                    log.info("Remote versions: [{}]", remoteVersions);

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
                            continue; //this condition is taken care of by above diffs

                        if (localVersions.get(address) <  remoteVersions.get(address))
                            localIsOlder.add(address);
                        else if (localVersions.get(address) > remoteVersions.get(address))
                            localIsNewer.add(address);
                        else
                            continue;
                    }

                    if (!localIsOlder.isEmpty())
                        sendGossipStatusTo(sender, localVersions );

                    if (!localIsNewer.isEmpty())
                        sendGossipTo(sender, localIsNewer);//send newer buckets to remote

                }
                return null;
            }
        };
    }

    /**
     * Processes the message from {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore} that contains
     * {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket}. These buckets are sent to a remote member encapsulated
     * in {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope}
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
                    log.info("Buckets to send from {}: {}", selfAddress, buckets);
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
