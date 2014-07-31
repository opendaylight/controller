/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucket;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucketReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateBucket;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;


/**
 * The data stored must be thread-safe.
 * TODO: Clone remote buckets so that accidental/intentional local modification is not gossiped
 */
public class BucketStore extends UntypedActor {

    /**
     * Local rpc registry
     */
    private BucketImpl localBucket = new BucketImpl();;

    /**
     * Keeps buckets for each known member.
     */
    private ConcurrentMap<Address, Bucket> remoteBuckets = new ConcurrentHashMap<>();;

    /**
     * Maintains bucket version for every known node in the cluster
     */
    private ConcurrentMap<Address, Long> versions = new ConcurrentHashMap<>();;

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Address selfAddress = Cluster.get(getContext().system()).selfAddress();

    private ActorRef gossiper;

    public BucketStore(){
        gossiper = getContext().actorOf(Props.create(Gossiper.class), "gossiper");
    }

    public BucketStore(ActorRef gossiper){
        this.gossiper = gossiper;
    }

    @Override
    public void preStart(){
        log.debug("{} starting...", getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        Integer randomInt = ThreadLocalRandom.current().nextInt();
        log.info("{}-{}-Received message : [{}]", randomInt, selfAddress, message);
        log.info("{}-{}- Local Bucket : [{}]", randomInt, selfAddress, localBucket);
        log.info("{}-{}- Remote Bucket : [{}]", randomInt, selfAddress, remoteBuckets);

        if (message instanceof UpdateBucket)
            receiveUpdateBucket(((UpdateBucket) message).getBucket());

        else if (message instanceof GetAllBuckets)
            receiveGetAllBucket();

        else if (message instanceof GetLocalBucket)
            receiveGetLocalBucket();

        else if (message instanceof GetBucketsByMembers)
            receiveGetBucketsByMembers(((GetBucketsByMembers) message).getMembers());

        else if (message instanceof GetBucketVersions)
            receiveGetBucketVersions();

        else if (message instanceof UpdateRemoteBuckets)
            receiveUpdateRemoteBuckets(((UpdateRemoteBuckets) message).getBuckets());

        else {
            log.debug("Unhandled message [{}]", message);
            unhandled(message);
        }

    }

    private void receiveGetLocalBucket() {
        final ActorRef sender = getSender();
        GetLocalBucketReply reply = new GetLocalBucketReply(localBucket);
        sender.tell(reply, getSelf());
    }

    void receiveUpdateBucket(Bucket updatedBucket){

        if (updatedBucket == null)
            localBucket = new BucketImpl<>(); //reinitialize
        else
            localBucket = (BucketImpl) updatedBucket;

        versions.put(selfAddress, localBucket.getVersion());
        log.info("Version table [{}]",versions);
    }

    void receiveGetAllBucket(){
        final ActorRef sender = getSender();
        sender.tell(new GetAllBucketsReply(getAllBuckets()), getSelf());
    }

    Map<Address, Bucket> getAllBuckets(){
        Map<Address, Bucket> copy = new HashMap<>();

        //first add the local bucket
        copy.put(selfAddress, localBucket);

        //then get all remote buckets
        copy.putAll(remoteBuckets);

        return copy;
    }

    void receiveGetBucketsByMembers(Set<Address> members){
        final ActorRef sender = getSender();
        Map<Address, Bucket> buckets = getGetBucketsByMembers(members);
        sender.tell(new GetBucketsByMembersReply(buckets), getSelf());

    }

    Map<Address, Bucket> getGetBucketsByMembers(Set<Address> members) {
        Map<Address, Bucket> copy = new HashMap<>();

        //first add the local bucket if asked
        if (members.contains(selfAddress))
            copy.put(selfAddress, localBucket);

        //then get buckets for requested remote nodes
        for (Address address : members){
            if (remoteBuckets.containsKey(address))
                copy.put(address, remoteBuckets.get(address));
        }

        return copy;
    }

    void receiveGetBucketVersions(){
        final ActorRef sender = getSender();

        GetBucketVersionsReply reply = new GetBucketVersionsReply(versions);

        sender.tell(reply, getSelf());
        log.debug("Replied getBucketVersions");
    }

    /**
     * Update local copy of remote buckets where local copy's version is older
     *
     * @param receivedBuckets buckets sent by remote {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}
     */
    void receiveUpdateRemoteBuckets(Map<Address, Bucket> receivedBuckets){

        if (receivedBuckets == null || receivedBuckets.isEmpty())
            return; //nothing to do

        //remove bucket for self from the list if exists.
        receivedBuckets.remove(selfAddress);

        for (Map.Entry<Address, Bucket> entry : receivedBuckets.entrySet()){

            Long localVersion = versions.get(entry.getKey());
            if (localVersion == null) localVersion = -1L;

            Bucket receivedBucket = entry.getValue();

            if (receivedBucket == null)
                continue;

            Long remoteVersion = receivedBucket.getVersion();
            if (remoteVersion == null) remoteVersion = -1L;

            //update only if remote version is newer
            if ( remoteVersion > localVersion ) {
                remoteBuckets.put(entry.getKey(), receivedBucket);
                versions.put(entry.getKey(), remoteVersion);
            }
        }
    }

    ///
    ///Getter Setters
    ///

    BucketImpl getLocalBucket() {
        return localBucket;
    }

    void setLocalBucket(BucketImpl localBucket) {
        this.localBucket = localBucket;
    }

    ConcurrentMap<Address, Bucket> getRemoteBuckets() {
        return remoteBuckets;
    }

    void setRemoteBuckets(ConcurrentMap<Address, Bucket> remoteBuckets) {
        this.remoteBuckets = remoteBuckets;
    }

    ConcurrentMap<Address, Long> getVersions() {
        return versions;
    }

    void setVersions(ConcurrentMap<Address, Long> versions) {
        this.versions = versions;
    }

    Address getSelfAddress() {
        return selfAddress;
    }
}
