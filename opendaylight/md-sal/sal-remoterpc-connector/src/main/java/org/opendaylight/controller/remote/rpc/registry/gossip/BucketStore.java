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
import akka.cluster.ClusterActorRefProvider;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.RemoveRemoteBucket;
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
public class BucketStore<T extends Copier<T>> extends AbstractUntypedActorWithMetering {
    /**
     * Bucket owned by the node
     */
    private final BucketImpl<T> localBucket;

    /**
     * Buckets owned by other known nodes in the cluster.
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

    // FIXME: should be part of test-specific subclass
    private ConditionalProbe probe;

    private final RemoteRpcProviderConfig config;

    public BucketStore(final RemoteRpcProviderConfig config, final T initialData) {
        this.config = Preconditions.checkNotNull(config);
        this.localBucket = new BucketImpl<>(initialData);
    }

    @Override
    public void preStart(){
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if (provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Gossiper.props(config).withMailbox(config.getMailBoxName()), "gossiper");
        }
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (probe != null) {
            probe.tell(message, getSelf());
        }

        if (message instanceof GetBucketsByMembers) {
            receiveGetBucketsByMembers(((GetBucketsByMembers) message).getMembers());
        } else if (message instanceof GetBucketVersions) {
            receiveGetBucketVersions();
        } else if (message instanceof UpdateRemoteBuckets) {
            receiveUpdateRemoteBuckets(((UpdateRemoteBuckets<T>) message).getBuckets());
        } else if (message instanceof RemoveRemoteBucket) {
            removeBucket(((RemoveRemoteBucket) message).getAddress());
        } else if (message instanceof GetAllBuckets) {
            // GetAllBuckets is used only for unit tests.
            receiveGetAllBuckets();
        } else if (message instanceof ConditionalProbe) {
            // The ConditionalProbe is only used for unit tests.
            LOG.info("Received probe {} {}", getSelf(), message);
            probe = (ConditionalProbe) message;
            // Send back any message to tell the caller we got the probe.
            getSender().tell("Got it", getSelf());
        } else {
            LOG.debug("Unhandled message [{}]", message);
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
    void receiveGetBucketsByMembers(final Set<Address> members) {
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
    Map<Address, Bucket<T>> getBucketsByMembers(final Set<Address> members) {
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
    void receiveUpdateRemoteBuckets(final Map<Address, Bucket<T>> receivedBuckets) {
        LOG.debug("{}: receiveUpdateRemoteBuckets: {}", selfAddress, receivedBuckets);
        if (receivedBuckets == null || receivedBuckets.isEmpty()) {
            //nothing to do
            return;
        }

        final Map<Address, Bucket<T>> newBuckets = new HashMap<>(receivedBuckets.size());
        for (Entry<Address, Bucket<T>> entry : receivedBuckets.entrySet()) {
            if (selfAddress.equals(entry.getKey())) {
                // Remote cannot update our bucket
                continue;
            }

            final Bucket<T> receivedBucket = entry.getValue();
            if (receivedBucket == null) {
                LOG.debug("Ignoring null bucket from {}", entry.getKey());
                continue;
            }

            // update only if remote version is newer
            final long remoteVersion = receivedBucket.getVersion();
            final Long localVersion = versions.get(entry.getKey());
            if (localVersion != null && remoteVersion <= localVersion.longValue()) {
                LOG.debug("Ignoring down-versioned bucket from {} ({} local {} remote)", entry.getKey(), localVersion,
                    remoteVersion);
                continue;
            }

            newBuckets.put(entry.getKey(), receivedBucket);
            remoteBuckets.put(entry.getKey(), receivedBucket);
            versions.put(entry.getKey(), remoteVersion);
            LOG.debug("Updating bucket from {} to version {}", entry.getKey(), remoteVersion);
        }

        LOG.debug("State after update - Local Bucket [{}], Remote Buckets [{}]", localBucket, remoteBuckets);

        onBucketsUpdated(newBuckets);
    }

    private void removeBucket(final Address address) {
        final Bucket<T> bucket = remoteBuckets.remove(address);
        if (bucket != null) {
            onBucketRemoved(address, bucket);
        }
    }

    /**
     * Callback to subclasses invoked when a bucket is removed.
     *
     * @param address Remote address
     * @param bucket Bucket removed
     */
    protected void onBucketRemoved(final Address address, final Bucket<T> bucket) {
        // Default noop
    }

    /**
     * Callback to subclasses invoked when the set of remote buckets is updated.
     *
     * @param newBuckets Map of address to new bucket. Never null, but can be empty.
     */
    protected void onBucketsUpdated(final Map<Address, Bucket<T>> newBuckets) {
        // Default noop
    }

    public BucketImpl<T> getLocalBucket() {
        return localBucket;
    }

    protected void updateLocalBucket(final T data) {
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
