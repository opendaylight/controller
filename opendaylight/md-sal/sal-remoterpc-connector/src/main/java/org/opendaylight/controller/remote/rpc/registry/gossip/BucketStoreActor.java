/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons.GET_ALL_BUCKETS;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons.GET_BUCKET_VERSIONS;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.Address;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.cluster.ClusterActorRefProvider;
import akka.persistence.DeleteSnapshotsFailure;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;

/**
 * A store that syncs its data across nodes in the cluster.
 * It maintains a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket} per node. Buckets are versioned.
 * A node can write ONLY to its bucket. This way, write conflicts are avoided.
 *
 * <p>
 * Buckets are sync'ed across nodes using Gossip protocol (http://en.wikipedia.org/wiki/Gossip_protocol).
 * This store uses a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}.
 */
public abstract class BucketStoreActor<T extends BucketData<T>> extends
        AbstractUntypedPersistentActorWithMetering {
    // Internal marker interface for messages which are just bridges to execute a method
    @FunctionalInterface
    private interface ExecuteInActor extends Consumer<BucketStoreActor<?>> {

    }

    /**
     * Buckets owned by other known nodes in the cluster.
     */
    private final Map<Address, Bucket<T>> remoteBuckets = new HashMap<>();

    /**
     * Bucket version for every known node in the cluster including this node.
     */
    private final Map<Address, Long> versions = new HashMap<>();

    /**
     * {@link ActorRef}s being watched for liveness due to being referenced in bucket data. Each actor is monitored
     * once, possibly being tied to multiple addresses (and by extension, buckets).
     */
    private final SetMultimap<ActorRef, Address> watchedActors = HashMultimap.create(1, 1);

    private final RemoteOpsProviderConfig config;
    private final String persistenceId;

    /**
     * Cluster address for this node.
     */
    private Address selfAddress;

    /**
     * Bucket owned by the node. Initialized during recovery (due to incarnation number).
     */
    private LocalBucket<T> localBucket;
    private T initialData;
    private Integer incarnation;
    private boolean persisting;

    protected BucketStoreActor(final RemoteOpsProviderConfig config, final String persistenceId, final T initialData) {
        super(false);
        this.config = requireNonNull(config);
        this.initialData = requireNonNull(initialData);
        this.persistenceId = requireNonNull(persistenceId);
    }

    static ExecuteInActor getBucketsByMembersMessage(final Collection<Address> members) {
        return actor -> actor.getBucketsByMembers(members);
    }

    static ExecuteInActor removeBucketMessage(final Address addr) {
        return actor -> actor.removeBucket(addr);
    }

    static ExecuteInActor updateRemoteBucketsMessage(final Map<Address, Bucket<?>> buckets) {
        return actor -> actor.updateRemoteBuckets(buckets);
    }

    static ExecuteInActor getLocalDataMessage() {
        return actor -> actor.getSender().tell(actor.getLocalData(), actor.getSelf());
    }

    static ExecuteInActor getRemoteBucketsMessage() {
        return actor -> actor.getSender().tell(ImmutableMap.copyOf(actor.getRemoteBuckets()), actor.getSelf());
    }

    public final T getLocalData() {
        return getLocalBucket().getData();
    }

    public final Map<Address, Bucket<T>> getRemoteBuckets() {
        return remoteBuckets;
    }

    public final Map<Address, Long> getVersions() {
        return versions;
    }

    @Override
    public final String persistenceId() {
        return persistenceId;
    }

    @Override
    public void preStart() {
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if (provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Gossiper.props(config).withMailbox(config.getMailBoxName()), "gossiper");
        }
    }

    @Override
    protected void handleCommand(final Object message) throws Exception {
        if (GET_ALL_BUCKETS == message) {
            // GetAllBuckets is used only in testing
            getSender().tell(getAllBuckets(), self());
            return;
        }

        if (persisting) {
            handleSnapshotMessage(message);
            return;
        }

        if (message instanceof ExecuteInActor) {
            ((ExecuteInActor) message).accept(this);
        } else if (GET_BUCKET_VERSIONS == message) {
            // FIXME: do we need to send ourselves?
            getSender().tell(ImmutableMap.copyOf(versions), getSelf());
        } else if (message instanceof Terminated) {
            actorTerminated((Terminated) message);
        } else if (message instanceof DeleteSnapshotsSuccess) {
            LOG.debug("{}: got command: {}", persistenceId(), message);
        } else if (message instanceof DeleteSnapshotsFailure) {
            LOG.warn("{}: failed to delete prior snapshots", persistenceId(),
                ((DeleteSnapshotsFailure) message).cause());
        } else {
            LOG.debug("Unhandled message [{}]", message);
            unhandled(message);
        }
    }

    private void handleSnapshotMessage(final Object message) {
        if (message instanceof SaveSnapshotFailure) {
            LOG.error("{}: failed to persist state", persistenceId(), ((SaveSnapshotFailure) message).cause());
            persisting = false;
            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
        } else if (message instanceof SaveSnapshotSuccess) {
            LOG.debug("{}: got command: {}", persistenceId(), message);
            SaveSnapshotSuccess saved = (SaveSnapshotSuccess)message;
            deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                    saved.metadata().timestamp() - 1, 0L, 0L));
            persisting = false;
            unstash();
        } else {
            LOG.debug("{}: stashing command {}", persistenceId(), message);
            stash();
        }
    }

    @Override
    protected final void handleRecover(final Object message) {
        if (message instanceof RecoveryCompleted) {
            if (incarnation != null) {
                incarnation = incarnation + 1;
            } else {
                incarnation = 0;
            }

            this.localBucket = new LocalBucket<>(incarnation.intValue(), initialData);
            initialData = null;
            LOG.debug("{}: persisting new incarnation {}", persistenceId(), incarnation);
            persisting = true;
            saveSnapshot(incarnation);
        } else if (message instanceof SnapshotOffer) {
            incarnation = (Integer) ((SnapshotOffer)message).snapshot();
            LOG.debug("{}: recovered incarnation {}", persistenceId(), incarnation);
        } else {
            LOG.warn("{}: ignoring recovery message {}", persistenceId(), message);
        }
    }

    protected final RemoteOpsProviderConfig getConfig() {
        return config;
    }

    protected final void updateLocalBucket(final T data) {
        final LocalBucket<T> local = getLocalBucket();
        final boolean bumpIncarnation = local.setData(data);
        versions.put(selfAddress, local.getVersion());

        if (bumpIncarnation) {
            LOG.debug("Version wrapped. incrementing incarnation");

            verify(incarnation < Integer.MAX_VALUE, "Ran out of incarnations, cannot continue");
            incarnation = incarnation + 1;

            persisting = true;
            saveSnapshot(incarnation);
        }
    }

    /**
     * Callback to subclasses invoked when a bucket is removed.
     *
     * @param address Remote address
     * @param bucket Bucket removed
     */
    protected abstract void onBucketRemoved(Address address, Bucket<T> bucket);

    /**
     * Callback to subclasses invoked when the set of remote buckets is updated.
     *
     * @param newBuckets Map of address to new bucket. Never null, but can be empty.
     */
    protected abstract void onBucketsUpdated(Map<Address, Bucket<T>> newBuckets);

    /**
     * Helper to collect all known buckets.
     *
     * @return self owned + remote buckets
     */
    private Map<Address, Bucket<T>> getAllBuckets() {
        Map<Address, Bucket<T>> all = new HashMap<>(remoteBuckets.size() + 1);

        //first add the local bucket
        all.put(selfAddress, getLocalBucket().snapshot());

        //then get all remote buckets
        all.putAll(remoteBuckets);

        return all;
    }

    /**
     * Helper to collect buckets for requested members.
     *
     * @param members requested members
     */
    private void getBucketsByMembers(final Collection<Address> members) {
        Map<Address, Bucket<T>> buckets = new HashMap<>();

        //first add the local bucket if asked
        if (members.contains(selfAddress)) {
            buckets.put(selfAddress, getLocalBucket().snapshot());
        }

        //then get buckets for requested remote nodes
        for (Address address : members) {
            if (remoteBuckets.containsKey(address)) {
                buckets.put(address, remoteBuckets.get(address));
            }
        }

        getSender().tell(buckets, getSelf());
    }

    private void removeBucket(final Address addr) {
        final Bucket<T> bucket = remoteBuckets.remove(addr);
        if (bucket != null) {
            bucket.getWatchActor().ifPresent(ref -> removeWatch(addr, ref));
            onBucketRemoved(addr, bucket);
        }
        versions.remove(addr);
    }

    /**
     * Update local copy of remote buckets where local copy's version is older.
     *
     * @param receivedBuckets buckets sent by remote
     *                        {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}
     */
    @VisibleForTesting
    void updateRemoteBuckets(final Map<Address, Bucket<?>> receivedBuckets) {
        LOG.debug("{}: receiveUpdateRemoteBuckets: {}", selfAddress, receivedBuckets);
        if (receivedBuckets == null || receivedBuckets.isEmpty()) {
            //nothing to do
            return;
        }

        final Map<Address, Bucket<T>> newBuckets = new HashMap<>(receivedBuckets.size());
        for (Entry<Address, Bucket<?>> entry : receivedBuckets.entrySet()) {
            final Address addr = entry.getKey();

            if (selfAddress.equals(addr)) {
                // Remote cannot update our bucket
                continue;
            }

            @SuppressWarnings("unchecked")
            final Bucket<T> receivedBucket = (Bucket<T>) entry.getValue();
            if (receivedBucket == null) {
                LOG.debug("Ignoring null bucket from {}", addr);
                continue;
            }

            // update only if remote version is newer
            final long remoteVersion = receivedBucket.getVersion();
            final Long localVersion = versions.get(addr);
            if (localVersion != null && remoteVersion <= localVersion.longValue()) {
                LOG.debug("Ignoring down-versioned bucket from {} ({} local {} remote)", addr, localVersion,
                    remoteVersion);
                continue;
            }
            newBuckets.put(addr, receivedBucket);
            versions.put(addr, remoteVersion);
            final Bucket<T> prevBucket = remoteBuckets.put(addr, receivedBucket);

            // Deal with DeathWatch subscriptions
            final Optional<ActorRef> prevRef = prevBucket != null ? prevBucket.getWatchActor() : Optional.empty();
            final Optional<ActorRef> curRef = receivedBucket.getWatchActor();
            if (!curRef.equals(prevRef)) {
                prevRef.ifPresent(ref -> removeWatch(addr, ref));
                curRef.ifPresent(ref -> addWatch(addr, ref));
            }

            LOG.debug("Updating bucket from {} to version {}", entry.getKey(), remoteVersion);
        }

        LOG.debug("State after update - Local Bucket [{}], Remote Buckets [{}]", localBucket, remoteBuckets);

        onBucketsUpdated(newBuckets);
    }

    private void addWatch(final Address addr, final ActorRef ref) {
        if (!watchedActors.containsKey(ref)) {
            getContext().watch(ref);
            LOG.debug("Watching {}", ref);
        }
        watchedActors.put(ref, addr);
    }

    private void removeWatch(final Address addr, final ActorRef ref) {
        watchedActors.remove(ref, addr);
        if (!watchedActors.containsKey(ref)) {
            getContext().unwatch(ref);
            LOG.debug("No longer watching {}", ref);
        }
    }

    private void actorTerminated(final Terminated message) {
        LOG.info("Actor termination {} received", message);

        for (Address addr : watchedActors.removeAll(message.getActor())) {
            versions.remove(addr);
            final Bucket<T> bucket = remoteBuckets.remove(addr);
            if (bucket != null) {
                LOG.debug("Source actor dead, removing bucket {} from {}", bucket, addr);
                onBucketRemoved(addr, bucket);
            }
        }
    }

    @VisibleForTesting
    protected boolean isPersisting() {
        return persisting;
    }

    private LocalBucket<T> getLocalBucket() {
        checkState(localBucket != null, "Attempted to access local bucket before recovery completed");
        return localBucket;
    }
}
