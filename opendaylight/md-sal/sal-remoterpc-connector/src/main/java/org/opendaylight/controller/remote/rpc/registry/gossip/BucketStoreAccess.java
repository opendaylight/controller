/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.getBucketsByMembersMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.getLocalDataMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.getRemoteBucketsMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.removeBucketMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.updateRemoteBucketsMessage;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Address;
import org.apache.pekko.pattern.Patterns;

/**
 * Convenience access to {@link BucketStoreActor}. Used mostly by {@link Gossiper}.
 */
@VisibleForTesting
public final class BucketStoreAccess {
    private final ActorRef actorRef;
    private final Executor executor;
    private final Duration timeout;

    public BucketStoreAccess(final ActorRef actorRef, final Executor executor, final Duration timeout) {
        this.actorRef = requireNonNull(actorRef);
        this.executor = requireNonNull(executor);
        this.timeout = requireNonNull(timeout);
    }

    <T extends BucketData<T>> void getBucketsByMembers(final Collection<Address> members,
            final Consumer<Map<Address, Bucket<T>>> callback) {
        Patterns.ask(actorRef, getBucketsByMembersMessage(members), timeout).whenCompleteAsync((success, failure) -> {
            if (failure == null) {
                callback.accept((Map<Address, Bucket<T>>) success);
            }
        }, executor);
    }

    void getBucketVersions(final Consumer<Map<Address, Long>> callback) {
        Patterns.ask(actorRef, Singletons.GET_BUCKET_VERSIONS, timeout).whenCompleteAsync((success, failure) -> {
            if (failure == null) {
                callback.accept((Map<Address, Long>) success);
            }
        }, executor);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CompletionStage<Map<Address, Long>> getBucketVersions() {
        return (CompletionStage) Patterns.ask(actorRef, Singletons.GET_BUCKET_VERSIONS, timeout);
    }

    @SuppressWarnings("unchecked")
    void updateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
        actorRef.tell(updateRemoteBucketsMessage((Map<Address, Bucket<?>>) buckets), ActorRef.noSender());
    }

    void removeRemoteBucket(final Address addr) {
        actorRef.tell(removeBucketMessage(addr), ActorRef.noSender());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends BucketData<T>> CompletionStage<T> getLocalData() {
        return (CompletionStage) Patterns.ask(actorRef, getLocalDataMessage(), timeout);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends BucketData<T>> CompletionStage<Map<Address, Bucket<T>>> getRemoteBuckets() {
        return (CompletionStage) Patterns.ask(actorRef, getRemoteBucketsMessage(), timeout);
    }

    public enum Singletons {
        /**
         * Sent from Gossiper to BucketStore, response is an immutable {@code Map&lt;Address, Bucket&lt;?&gt;&gt;}.
         */
        GET_ALL_BUCKETS,
        /**
         * Sent from Gossiper to BucketStore, response is an immutable {@code Map&lt;Address, Long&gt;}.
         */
        GET_BUCKET_VERSIONS,
    }
}
