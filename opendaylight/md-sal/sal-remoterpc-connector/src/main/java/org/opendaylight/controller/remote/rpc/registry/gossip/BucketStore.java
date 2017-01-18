/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Convenience access to {@link BucketStoreActor}. Used mostly by {@link Gossiper}.
 *
 * @author Robert Varga
 */
public final class BucketStore {
    private final ActorContext context;
    private final Timeout timeout;

    private BucketStore(final ActorContext context, final Timeout timeout) {
        this.context = Preconditions.checkNotNull(context);
        this.timeout = Preconditions.checkNotNull(timeout);
    }

    static BucketStore forContext(final ActorContext context, final Timeout timeout) {
        return new BucketStore(context, timeout);
    }

    <T extends BucketData<T>> void getBucketsByMembers(final Collection<Address> members,
            final BiConsumer<Throwable, Map<Address, Bucket<T>>> callback) {
        Patterns.ask(context.parent(), new GetBucketsByMembers(members), timeout).onComplete(new OnComplete<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onComplete(final Throwable arg0, final Object arg1) {
                callback.accept(arg0, (Map<Address, Bucket<T>>) arg1);
            }
        }, context.dispatcher());
    }

    void getBucketVersions(final BiConsumer<Throwable, Map<Address, Long>> callback) {
        Patterns.ask(context.parent(), Singletons.GET_BUCKET_VERSIONS, timeout).onComplete(new OnComplete<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onComplete(final Throwable arg0, final Object arg1) {
                callback.accept(arg0, (Map<Address, Long>) arg1);
            }
        }, context.dispatcher());
    }

    void updateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
        context.parent().tell(new UpdateRemoteBuckets(buckets), ActorRef.noSender());
    }

    void removeRemoteBucket(final Address address) {
        context.parent().tell(new RemoveRemoteBucket(address), ActorRef.noSender());
    }

    @VisibleForTesting
    public enum Singletons {
        GET_ALL_BUCKETS,
        GET_BUCKET_VERSIONS,
    }

    // Sent from Gossiper to BucketStore, response is an immutable Map<Address, Bucket<?>>
    static final class GetBucketsByMembers {
        private final Set<Address> members;

        private GetBucketsByMembers(final Collection<Address> members) {
            this.members = ImmutableSet.copyOf(members);
        }

        public Set<Address> getMembers() {
            return members;
        }
    }

    static final class UpdateRemoteBuckets {
        private final Map<Address, Bucket<?>> members;

        UpdateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
            this.members = ImmutableMap.copyOf(buckets);
        }

        public Map<Address, Bucket<?>> getBuckets() {
            return members;
        }
    }

    static final class RemoveRemoteBucket {
        private final Address address;

        RemoveRemoteBucket(final Address address) {
            this.address = Preconditions.checkNotNull(address);
        }

        public Address getAddress() {
            return address;
        }
    }
}
