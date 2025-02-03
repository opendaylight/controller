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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorRefProvider;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.cluster.ClusterActorRefProvider;
import org.apache.pekko.persistence.DeleteSnapshotsFailure;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.persistence.SnapshotOffer;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.slf4j.Logger;

/**
 * A store that syncs its data across nodes in the cluster.
 * It maintains a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket} per node. Buckets are versioned.
 * A node can write ONLY to its bucket. This way, write conflicts are avoided.
 *
 * <p>Buckets are sync'ed across nodes using Gossip protocol (http://en.wikipedia.org/wiki/Gossip_protocol).
 * This store uses a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}.
 */
public abstract class BucketStoreActor<T extends BucketData<T>> extends
        AbstractUntypedPersistentActorWithMetering {
    // Internal marker interface for messages which are just bridges to execute a method
    @FunctionalInterface
    private interface ExecuteInActor {

        void accept(@NonNull BucketStoreActor<?> actor);
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
        super(persistenceId);
        this.config = requireNonNull(config);
        this.initialData = requireNonNull(initialData);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    protected abstract Logger log();

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
        return actor -> actor.getSender().tell(actor.getLocalData(), actor.self());
    }

    static ExecuteInActor getRemoteBucketsMessage() {
        return actor -> actor.getSender().tell(ImmutableMap.copyOf(actor.getRemoteBuckets()), actor.self());
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
    public void preStart() {
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if (provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Gossiper.props(persistenceId(), config)
                .withMailbox(config.getMailBoxName()), "gossiper");
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

        if (GET_BUCKET_VERSIONS == message) {
            // FIXME: do we need to send ourselves?
            getSender().tell(ImmutableMap.copyOf(versions), self());
            return;
        }

        switch (message) {
            case ExecuteInActor execute -> execute.accept(this);
            case Terminated terminated -> actorTerminated(terminated);
            case DeleteSnapshotsSuccess deleteSuccess ->
                log().debug("{}: got command: {}", persistenceId(), deleteSuccess);
            case DeleteSnapshotsFailure deleteFailure ->
                log().warn("{}: failed to delete prior snapshots", persistenceId(), deleteFailure.cause());
            default -> {
                log().debug("Unhandled message [{}]", message);
                unhandled(message);
            }
        }
    }

    private void handleSnapshotMessage(final Object message) {
        switch (message) {
            case SaveSnapshotFailure saveFailure -> {
                log().error("{}: failed to persist state", persistenceId(), saveFailure.cause());
                persisting = false;
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
            case SaveSnapshotSuccess saveSuccess -> {
                log().debug("{}: got command: {}", persistenceId(), saveSuccess);
                deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                    saveSuccess.metadata().timestamp() - 1, 0L, 0L));
                persisting = false;
                unstash();
            }
            default -> {
                log().debug("{}: stashing command {}", persistenceId(), message);
                stash();
            }
        }
    }

    @Override
    protected final void handleRecover(final Object message) {
        switch (message) {
            case RecoveryCompleted msg -> onRecoveryCompleted(msg);
            case SnapshotOffer msg -> onSnapshotOffer(msg);
            default -> log().warn("{}: ignoring recovery message {}", persistenceId(), message);
        }
    }

    private void onRecoveryCompleted(final RecoveryCompleted msg) {
        final var prev = incarnation;
        final var current = prev != null ? incarnation + 1 : 0;
        localBucket = new LocalBucket<>(current, initialData);
        incarnation = current;
        initialData = null;

        log().debug("{}: persisting new incarnation {}", persistenceId(), incarnation);
        persisting = true;
        saveSnapshot(incarnation);
    }

    private void onSnapshotOffer(final SnapshotOffer msg) {
        incarnation = (Integer) msg.snapshot();
        log().debug("{}: recovered incarnation {}", persistenceId(), incarnation);
    }

    protected final RemoteOpsProviderConfig getConfig() {
        return config;
    }

    protected final void updateLocalBucket(final T data) {
        final var local = getLocalBucket();
        final boolean bumpIncarnation = local.setData(data);
        versions.put(selfAddress, local.getVersion());

        if (bumpIncarnation) {
            log().debug("Version wrapped. incrementing incarnation");

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
        final var all = new HashMap<Address, Bucket<T>>(remoteBuckets.size() + 1);

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
        final var buckets = new HashMap<Address, Bucket<T>>();

        //first add the local bucket if asked
        if (members.contains(selfAddress)) {
            buckets.put(selfAddress, getLocalBucket().snapshot());
        }

        //then get buckets for requested remote nodes
        for (var address : members) {
            if (remoteBuckets.containsKey(address)) {
                buckets.put(address, remoteBuckets.get(address));
            }
        }

        getSender().tell(buckets, self());
    }

    private void removeBucket(final Address addr) {
        final var bucket = remoteBuckets.remove(addr);
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
        log().debug("{}: receiveUpdateRemoteBuckets: {}", selfAddress, receivedBuckets);
        if (receivedBuckets == null || receivedBuckets.isEmpty()) {
            //nothing to do
            return;
        }

        final var newBuckets = new HashMap<Address, Bucket<T>>(receivedBuckets.size());
        for (var entry : receivedBuckets.entrySet()) {
            final var addr = entry.getKey();

            if (selfAddress.equals(addr)) {
                // Remote cannot update our bucket
                continue;
            }

            @SuppressWarnings("unchecked")
            final var receivedBucket = (Bucket<T>) entry.getValue();
            if (receivedBucket == null) {
                log().debug("Ignoring null bucket from {}", addr);
                continue;
            }

            // update only if remote version is newer
            final long remoteVersion = receivedBucket.getVersion();
            final Long localVersion = versions.get(addr);
            if (localVersion != null && remoteVersion <= localVersion.longValue()) {
                log().debug("Ignoring down-versioned bucket from {} ({} local {} remote)", addr, localVersion,
                    remoteVersion);
                continue;
            }
            newBuckets.put(addr, receivedBucket);
            versions.put(addr, remoteVersion);
            final var prevBucket = remoteBuckets.put(addr, receivedBucket);

            // Deal with DeathWatch subscriptions
            final var prevRef = prevBucket != null ? prevBucket.getWatchActor() : Optional.<ActorRef>empty();
            final var curRef = receivedBucket.getWatchActor();
            if (!curRef.equals(prevRef)) {
                prevRef.ifPresent(ref -> removeWatch(addr, ref));
                curRef.ifPresent(ref -> addWatch(addr, ref));
            }

            log().debug("Updating bucket from {} to version {}", entry.getKey(), remoteVersion);
        }

        log().debug("State after update - Local Bucket [{}], Remote Buckets [{}]", localBucket, remoteBuckets);

        onBucketsUpdated(newBuckets);
    }

    private void addWatch(final Address addr, final ActorRef ref) {
        if (!watchedActors.containsKey(ref)) {
            getContext().watch(ref);
            log().debug("Watching {}", ref);
        }
        watchedActors.put(ref, addr);
    }

    private void removeWatch(final Address addr, final ActorRef ref) {
        watchedActors.remove(ref, addr);
        if (!watchedActors.containsKey(ref)) {
            getContext().unwatch(ref);
            log().debug("No longer watching {}", ref);
        }
    }

    private void actorTerminated(final Terminated message) {
        log().info("Actor termination {} received", message);

        for (var addr : watchedActors.removeAll(message.getActor())) {
            versions.remove(addr);
            final Bucket<T> bucket = remoteBuckets.remove(addr);
            if (bucket != null) {
                log().debug("Source actor dead, removing bucket {} from {}", bucket, addr);
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
