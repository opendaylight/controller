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
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.cluster.ClusterActorRefProvider;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons;
import org.slf4j.Logger;

/**
 * A store that syncs its data across nodes in the cluster.
 * It maintains a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket} per node. Buckets are versioned.
 * A node can write ONLY to its bucket. This way, write conflicts are avoided.
 *
 * <p>Buckets are sync'ed across nodes using Gossip protocol (http://en.wikipedia.org/wiki/Gossip_protocol).
 * This store uses a {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper}.
 */
public abstract class BucketStoreActor<T extends BucketData<T>> extends AbstractUntypedActorWithMetering {
    // Internal marker interface for messages which are just bridges to execute a method
    @FunctionalInterface
    private interface ExecuteInActor {

        void accept(@NonNull BucketStoreActor<?> actor);
    }

    private static final @NonNull Path INCARNATION_FILE = Path.of("incarnation-v1");

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

    private final @NonNull RemoteOpsProviderConfig config;
    private final @NonNull Path directory;

    /**
     * Cluster address for this node.
     */
    private Address selfAddress;

    /**
     * Bucket owned by the node. Initialized during recovery (due to incarnation number).
     */
    private LocalBucket<T> localBucket;
    private T initialData;
    private int incarnation;

    @NonNullByDefault
    protected BucketStoreActor(final RemoteOpsProviderConfig config, final Path directory, final String persistenceId,
            final T initialData) {
        super(persistenceId);
        this.config = requireNonNull(config);
        this.directory = directory.resolve(Path.of(persistenceId));
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

    private String persistenceId() {
        return getActorNameOverride();
    }

    @Override
    public void preStart() throws IOException {
        final var provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if (provider instanceof ClusterActorRefProvider) {
            getContext().actorOf(Gossiper.props(config).withMailbox(config.getMailBoxName()), "gossiper");
        }

        // Ensure directory and try to load the incarnation
        Files.createDirectories(directory);
        try (var dis = new DataInputStream(Files.newInputStream(directory.resolve(INCARNATION_FILE)))) {
            incarnation = dis.readInt() + 1;
        } catch (NoSuchFileException e) {
            final var log = log();
            if (!log.isTraceEnabled()) {
                e = null;
            }
            log.debug("{}: no incarnation file found", persistenceId(), e);
            incarnation = 0;
        }

        localBucket = new LocalBucket<>(incarnation, initialData);
        initialData = null;
        persistIncarnation();
    }

    @Override
    protected void handleReceive(final Object message) {
        switch (message) {
            // FIXME: we should use direct values, but that translates to duplicate instanceof cases, which SpotBugs
            //        does not like
            case Singletons singletons -> {
                switch (singletons) {
                    case null -> throw new NullPointerException();
                    case GET_ALL_BUCKETS ->
                        // GetAllBuckets is used only in testing
                        getSender().tell(getAllBuckets(), self());
                    case GET_BUCKET_VERSIONS ->
                        // FIXME: do we need to send ourselves?
                        getSender().tell(ImmutableMap.copyOf(versions), self());
                }
            }
            case ExecuteInActor execute -> execute.accept(this);
            case Terminated terminated -> actorTerminated(terminated);
            default -> {
                log().debug("Unhandled message [{}]", message);
                unhandled(message);
            }
        }
    }

    private void persistIncarnation() throws IOException {
        final var snapshot = incarnation;
        log().debug("{}: persisting new incarnation {}", persistenceId(), snapshot);

        final var tmpFile = Files.createTempFile(directory, INCARNATION_FILE.toString(), null);
        try {
            try (var dos = new DataOutputStream(Files.newOutputStream(tmpFile, SYNC, TRUNCATE_EXISTING, WRITE))) {
                dos.writeInt(snapshot);
            }
            Files.move(tmpFile, directory.resolve(INCARNATION_FILE), ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException e) {
                log().warn("{}: failed to delete {}", persistenceId(), tmpFile);
            }
        }

        log().debug("{}: new incarnation {} persisted", persistenceId(), snapshot);
    }

    protected final RemoteOpsProviderConfig getConfig() {
        return config;
    }

    protected final void updateLocalBucket(final T data) {
        final var local = getLocalBucket();
        final boolean bumpIncarnation = local.setData(data);
        versions.put(selfAddress, local.getVersion());

        if (bumpIncarnation) {
            log().debug("{}: Version wrapped. incrementing incarnation", persistenceId());

            verify(incarnation < Integer.MAX_VALUE, "Ran out of incarnations, cannot continue");
            incarnation = incarnation + 1;
            try {
                persistIncarnation();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to persist incarnation", e);
            }
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

    private LocalBucket<T> getLocalBucket() {
        checkState(localBucket != null, "Attempted to access local bucket before recovery completed");
        return localBucket;
    }
}
