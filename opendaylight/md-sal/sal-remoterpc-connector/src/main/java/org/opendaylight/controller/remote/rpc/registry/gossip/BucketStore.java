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
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import org.opendaylight.controller.utils.ConditionalProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A store that syncs its data across nodes in the cluster.
 * It maintains a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket} per node. Buckets are versioned.
 * A node can write ONLY to its bucket. This way, write conflicts are avoided.
 * <p>
 * Buckets are sync'ed across nodes using Gossip protocol (http://en.wikipedia.org/wiki/Gossip_protocol)<p>
 * This store uses a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}.
 *
 */
public class BucketStore<T extends Copier<T>> extends AbstractUntypedActorWithMetering {

    private static final Long NO_VERSION = -1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Bucket owned by the node
     */
    private final BucketImpl<T> localBucket = new BucketImpl<>();

    /**
     * Buckets ownded by other known nodes in the cluster
     */
    private final Map<Address, Bucket<T>> remoteBuckets = new HashMap<>();

    /**
     * Bucket version for every known node in the cluster including this node
     */
    private final Map<Address, Long> versions = new HashMap<>();

    /**
     * Cluster address for this node
     */
    private Address selfAddress;

    private ConditionalProbe probe;

    private final RemoteRpcProviderConfig config;

    public BucketStore(RemoteRpcProviderConfig config){
        this.config = Preconditions.checkNotNull(config);
    }

    @Override
    public void preStart(){
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if ( provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Props.create(Gossiper.class, config).withMailbox(config.getMailBoxName()), "gossiper");
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
        } else if (message instanceof GetAllBuckets) {
            receiveGetAllBuckets();
        } else if (message instanceof GetBucketsByMembers) {
            receiveGetBucketsByMembers(((GetBucketsByMembers) message).getMembers());
        } else if (message instanceof GetBucketVersions) {
            receiveGetBucketVersions();
        } else if (message instanceof UpdateRemoteBuckets) {
            receiveUpdateRemoteBuckets(((UpdateRemoteBuckets<T>) message).getBuckets());
        } else {
            if(log.isDebugEnabled()) {
                log.debug("Unhandled message [{}]", message);
            }
            unhandled(message);
        }
    }

    protected RemoteRpcProviderConfig getConfig() {
        return config;
    }

    /**
     * Returns all the buckets the this node knows about, self owned + remote
     */
    void receiveGetAllBuckets(){
        final ActorRef sender = getSender();
        sender.tell(new GetAllBucketsReply<T>(getAllBuckets()), getSelf());
    }

    /**
     * Helper to collect all known buckets
     *
     * @return self owned + remote buckets
     */
    Map<Address, Bucket<T>> getAllBuckets(){
        Map<Address, Bucket<T>> all = new HashMap<>(remoteBuckets.size() + 1);

        //first add the local bucket
        all.put(selfAddress, new BucketImpl<>(localBucket));

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
        Map<Address, Bucket<T>> buckets = getBucketsByMembers(members);
        sender.tell(new GetBucketsByMembersReply<T>(buckets), getSelf());
    }

    /**
     * Helper to collect buckets for requested memebers
     *
     * @param members requested members
     * @return buckets for requested memebers
     */
    Map<Address, Bucket<T>> getBucketsByMembers(Set<Address> members) {
        Map<Address, Bucket<T>> buckets = new HashMap<>();

        //first add the local bucket if asked
        if (members.contains(selfAddress)) {
            buckets.put(selfAddress, new BucketImpl<>(localBucket));
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
    void receiveUpdateRemoteBuckets(Map<Address, Bucket<T>> receivedBuckets){
        log.debug("{}: receiveUpdateRemoteBuckets: {}", selfAddress, receivedBuckets);
        if (receivedBuckets == null || receivedBuckets.isEmpty())
         {
            return; //nothing to do
        }

        //Remote cant update self's bucket
        receivedBuckets.remove(selfAddress);

        for (Map.Entry<Address, Bucket<T>> entry : receivedBuckets.entrySet()){

            Long localVersion = versions.get(entry.getKey());
            if (localVersion == null) {
                localVersion = NO_VERSION;
            }

            Bucket<T> receivedBucket = entry.getValue();

            if (receivedBucket == null) {
                continue;
            }

            Long remoteVersion = receivedBucket.getVersion();
            if (remoteVersion == null) {
                remoteVersion = NO_VERSION;
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

        onBucketsUpdated();
    }

    protected void onBucketsUpdated() {
    }

    public BucketImpl<T> getLocalBucket() {
        return localBucket;
    }

    protected void updateLocalBucket(T data) {
        localBucket.setData(data);
        versions.put(selfAddress, localBucket.getVersion());
    }

    public Map<Address, Bucket<T>> getRemoteBuckets() {
        return remoteBuckets;
    }

    public Map<Address, Long> getVersions() {
        return versions;
    }
}
