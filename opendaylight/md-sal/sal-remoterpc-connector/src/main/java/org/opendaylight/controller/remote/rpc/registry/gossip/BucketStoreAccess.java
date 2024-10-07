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
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Address;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Convenience access to {@link BucketStoreActor}. Used mostly by {@link Gossiper}.
 */
@VisibleForTesting
public final class BucketStoreAccess {
    private final ActorRef actorRef;
    private final ExecutionContext dispatcher;
    private final Timeout timeout;

    public BucketStoreAccess(final ActorRef actorRef, final ExecutionContext dispatcher, final Timeout timeout) {
        this.actorRef = requireNonNull(actorRef);
        this.dispatcher = requireNonNull(dispatcher);
        this.timeout = requireNonNull(timeout);
    }

    <T extends BucketData<T>> void getBucketsByMembers(final Collection<Address> members,
            final Consumer<Map<Address, Bucket<T>>> callback) {
        Patterns.ask(actorRef, getBucketsByMembersMessage(members), timeout)
            .onComplete(new OnComplete<>() {
                @SuppressWarnings("unchecked")
                @Override
                public void onComplete(final Throwable failure, final Object success) {
                    if (failure == null) {
                        callback.accept((Map<Address, Bucket<T>>) success);
                    }
                }
            }, dispatcher);
    }

    void getBucketVersions(final Consumer<Map<Address, Long>> callback) {
        Patterns.ask(actorRef, Singletons.GET_BUCKET_VERSIONS, timeout).onComplete(new OnComplete<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure == null) {
                    callback.accept((Map<Address, Long>) success);
                }
            }
        }, dispatcher);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Future<Map<Address, Long>> getBucketVersions() {
        return (Future) Patterns.ask(actorRef, Singletons.GET_BUCKET_VERSIONS, timeout);
    }

    @SuppressWarnings("unchecked")
    void updateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
        actorRef.tell(updateRemoteBucketsMessage((Map<Address, Bucket<?>>) buckets), ActorRef.noSender());
    }

    void removeRemoteBucket(final Address addr) {
        actorRef.tell(removeBucketMessage(addr), ActorRef.noSender());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends BucketData<T>> Future<T> getLocalData() {
        return (Future) Patterns.ask(actorRef, getLocalDataMessage(), timeout);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends BucketData<T>> Future<Map<Address, Bucket<T>>> getRemoteBuckets() {
        return (Future) Patterns.ask(actorRef, getRemoteBucketsMessage(), timeout);
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
