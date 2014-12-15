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
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.ClusterActorRefProvider;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucketReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateBucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import org.opendaylight.controller.utils.ConditionalProbe;

/**
 * A store that syncs its data across nodes in the cluster.
 * It maintains a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket} per node. Buckets are versioned.
 * A node can write ONLY to its bucket. This way, write conflicts are avoided.
 * <p>
 * Buckets are sync'ed across nodes using Gossip protocol (http://en.wikipedia.org/wiki/Gossip_protocol)<p>
 * This store uses a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}.
 *
 */
public class BucketStore extends AbstractUntypedActorWithMetering {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * Bucket owned by the node
     */
    private Bucket<RoutingTable> localBucket = new BucketImpl<RoutingTable>();

    /**
     * Buckets ownded by other known nodes in the cluster
     */
    private ConcurrentMap<Address, Bucket<RoutingTable>> remoteBuckets = new ConcurrentHashMap<>();

    /**
     * Bucket version for every known node in the cluster including this node
     */
    private ConcurrentMap<Address, Long> versions = new ConcurrentHashMap<>();

    /**
     * Cluster address for this node
     */
    private Address selfAddress;

    private ConditionalProbe probe;

    private final RemoteRpcProviderConfig config;

    public BucketStore(){
        config = new RemoteRpcProviderConfig(getContext().system().settings().config());
    }

    @Override
    public void preStart(){
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if ( provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Props.create(Gossiper.class).withMailbox(config.getMailBoxName()), "gossiper");
        }
    }


    @Override
    protected void handleReceive(Object message) throws Exception {
        if (probe != null) {
            probe.tell(message, getSelf());
        }

        if (message instanceof ConditionalProbe) {
            // The ConditionalProbe is only used for unit tests.
            log.info("Received probe {} {}", getSelf(), message);
            probe = (ConditionalProbe) message;
            // Send back any message to tell the caller we got the probe.
            getSender().tell("Got it", getSelf());
        } else if (message instanceof UpdateBucket) {
            receiveUpdateBucket(((UpdateBucket) message).getBucket());
        } else if (message instanceof GetAllBuckets) {
            receiveGetAllBucket();
        } else if (message instanceof GetLocalBucket) {
            receiveGetLocalBucket();
        } else if (message instanceof GetBucketsByMembers) {
            receiveGetBucketsByMembers(
                    ((GetBucketsByMembers) message).getMembers());
        } else if (message instanceof GetBucketVersions) {
            receiveGetBucketVersions();
        } else if (message instanceof UpdateRemoteBuckets) {
            receiveUpdateRemoteBuckets(
                    ((UpdateRemoteBuckets) message).getBuckets());
        } else {
            if(log.isDebugEnabled()) {
                log.debug("Unhandled message [{}]", message);
            }
            unhandled(message);
        }
    }

    /**
     * Returns a copy of bucket owned by this node
     */
    private void receiveGetLocalBucket() {
        final ActorRef sender = getSender();
        GetLocalBucketReply reply = new GetLocalBucketReply(localBucket);
        sender.tell(reply, getSelf());
    }

    /**
     * Updates the bucket owned by this node
     *
     * @param updatedBucket
     */
    void receiveUpdateBucket(Bucket<RoutingTable> updatedBucket){

        localBucket = updatedBucket;
        versions.put(selfAddress, localBucket.getVersion());
    }

    /**
     * Returns all the buckets the this node knows about, self owned + remote
     */
    void receiveGetAllBucket(){
        final ActorRef sender = getSender();
        sender.tell(new GetAllBucketsReply(getAllBuckets()), getSelf());
    }

    /**
     * Helper to collect all known buckets
     *
     * @return self owned + remote buckets
     */
    Map<Address, Bucket<RoutingTable>> getAllBuckets(){
        Map<Address, Bucket<RoutingTable>> all = new HashMap<>(remoteBuckets.size() + 1);

        //first add the local bucket
        all.put(selfAddress, localBucket);

        //then get all remote buckets
        all.putAll(remoteBuckets);

        return all;
    }

    /**
     * Returns buckets for requested members that this node knows about
     *
     * @param members requested members
     */
    void receiveGetBucketsByMembers(Set<Address> members){
        final ActorRef sender = getSender();
        Map<Address, Bucket<RoutingTable>> buckets = getBucketsByMembers(members);
        sender.tell(new GetBucketsByMembersReply(buckets), getSelf());
    }

    /**
     * Helper to collect buckets for requested memebers
     *
     * @param members requested members
     * @return buckets for requested memebers
     */
    Map<Address, Bucket<RoutingTable>> getBucketsByMembers(Set<Address> members) {
        Map<Address, Bucket<RoutingTable>> buckets = new HashMap<>();

        //first add the local bucket if asked
        if (members.contains(selfAddress)) {
            buckets.put(selfAddress, localBucket);
        }

        //then get buckets for requested remote nodes
        for (Address address : members){
            if (remoteBuckets.containsKey(address)) {
                buckets.put(address, remoteBuckets.get(address));
            }
        }

        return buckets;
    }

    /**
     * Returns versions for all buckets known
     */
    void receiveGetBucketVersions(){
        final ActorRef sender = getSender();
        GetBucketVersionsReply reply = new GetBucketVersionsReply(versions);
        sender.tell(reply, getSelf());
    }

    /**
     * Update local copy of remote buckets where local copy's version is older
     *
     * @param receivedBuckets buckets sent by remote
     *                        {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}
     */
    void receiveUpdateRemoteBuckets(Map<Address, Bucket<RoutingTable>> receivedBuckets){

        if (receivedBuckets == null || receivedBuckets.isEmpty())
         {
            return; //nothing to do
        }

        //Remote cant update self's bucket
        receivedBuckets.remove(selfAddress);

        for (Map.Entry<Address, Bucket<RoutingTable>> entry : receivedBuckets.entrySet()){

            Long localVersion = versions.get(entry.getKey());
            if (localVersion == null) {
                localVersion = -1L;
            }

            Bucket<RoutingTable> receivedBucket = entry.getValue();

            if (receivedBucket == null) {
                continue;
            }

            Long remoteVersion = receivedBucket.getVersion();
            if (remoteVersion == null) {
                remoteVersion = -1L;
            }

            //update only if remote version is newer
            if ( remoteVersion.longValue() > localVersion.longValue() ) {
                remoteBuckets.put(entry.getKey(), receivedBucket);
                versions.put(entry.getKey(), remoteVersion);
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("State after update - Local Bucket [{}], Remote Buckets [{}]", localBucket, remoteBuckets);
        }
    }

    ///
    ///Getter Setters
    ///

    Bucket<RoutingTable> getLocalBucket() {
        return localBucket;
    }

    void setLocalBucket(Bucket<RoutingTable> localBucket) {
        this.localBucket = localBucket;
    }

    ConcurrentMap<Address, Bucket<RoutingTable>> getRemoteBuckets() {
        return remoteBuckets;
    }

    void setRemoteBuckets(ConcurrentMap<Address, Bucket<RoutingTable>> remoteBuckets) {
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
