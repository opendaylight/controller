/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.getBucketsByMembersMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.removeBucketMessage;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor.updateRemoteBucketsMessage;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Convenience access to {@link BucketStoreActor}. Used mostly by {@link Gossiper}.
 *
 * @author Robert Varga
 */
@Beta
@VisibleForTesting
public final class BucketStoreAccess {
    private final ActorContext context;
    private final Timeout timeout;

    BucketStoreAccess(final ActorContext context, final Timeout timeout) {
        this.context = Preconditions.checkNotNull(context);
        this.timeout = Preconditions.checkNotNull(timeout);
    }

    <T extends BucketData<T>> void getBucketsByMembers(final Collection<Address> members,
            final Consumer<Map<Address, Bucket<T>>> callback) {
        Patterns.ask(context.parent(), getBucketsByMembersMessage(members), timeout)
            .onComplete(new OnComplete<Object>() {
                @SuppressWarnings("unchecked")
                @Override
                public void onComplete(final Throwable failure, final Object success) {
                    if (failure == null) {
                        callback.accept((Map<Address, Bucket<T>>) success);
                    }
                }
            }, context.dispatcher());
    }

    void getBucketVersions(final Consumer<Map<Address, Long>> callback) {
        Patterns.ask(context.parent(), Singletons.GET_BUCKET_VERSIONS, timeout).onComplete(new OnComplete<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure == null) {
                    callback.accept((Map<Address, Long>) success);
                }
            }
        }, context.dispatcher());
    }

    @SuppressWarnings("unchecked")
    void updateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
        context.parent().tell(updateRemoteBucketsMessage((Map<Address, Bucket<?>>) buckets), ActorRef.noSender());
    }

    void removeRemoteBucket(final Address addr) {
        context.parent().tell(removeBucketMessage(addr), ActorRef.noSender());
    }

    public enum Singletons {
        // Sent from Gossiper to BucketStore, response is an immutable Map<Address, Bucket<?>>
        GET_ALL_BUCKETS,
        // Sent from Gossiper to BucketStore, response is an immutable Map<Address, Long>
        GET_BUCKET_VERSIONS,
    }
}
