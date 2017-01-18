/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.Address;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * These messages are used by {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore} and
 * {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper} actors.
 */
public final class BucketStoreMessages {

    public static final class GetBucketsByMembers implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Set<Address> members;

        public GetBucketsByMembers(final Set<Address> members) {
            Preconditions.checkArgument(members != null, "members can not be null");
            this.members = ImmutableSet.copyOf(members);
        }

        public Set<Address> getMembers() {
            return members;
        }
    }

    public static class ContainsBuckets<T extends BucketData<T>> implements Serializable {
        private static final long serialVersionUID = -4940160367495308286L;

        private final Map<Address, Bucket<T>> buckets;

        protected ContainsBuckets(final Map<Address, Bucket<T>> buckets) {
            Preconditions.checkArgument(buckets != null, "buckets can not be null");
            this.buckets = ImmutableMap.copyOf(buckets);
        }

        public final Map<Address, Bucket<T>> getBuckets() {
            return buckets;
        }
    }

    public static final class GetAllBucketsReply<T extends BucketData<T>> extends BucketStoreMessages.ContainsBuckets<T> {
        private static final long serialVersionUID = 1L;

        public GetAllBucketsReply(final Map<Address, Bucket<T>> buckets) {
            super(buckets);
        }
    }

    public static final class GetBucketsByMembersReply<T extends BucketData<T>> extends BucketStoreMessages.ContainsBuckets<T>  {
        private static final long serialVersionUID = 1L;

        public GetBucketsByMembersReply(final Map<Address, Bucket<T>> buckets) {
            super(buckets);
        }
    }

    public static final class GetBucketVersions implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public static class ContainsBucketVersions implements Serializable {
        private static final long serialVersionUID = -8172148925383801613L;

        Map<Address, Long> versions;

        public ContainsBucketVersions(final Map<Address, Long> versions) {
            Preconditions.checkArgument(versions != null, "versions can not be null or empty");

            this.versions = ImmutableMap.copyOf(versions);
        }

        public Map<Address, Long> getVersions() {
            return versions;
        }
    }

    public static final class GetBucketVersionsReply extends BucketStoreMessages.ContainsBucketVersions {
        private static final long serialVersionUID = 1L;

        public GetBucketVersionsReply(final Map<Address, Long> versions) {
            super(versions);
        }
    }

    public static final class UpdateRemoteBuckets<T extends BucketData<T>> extends BucketStoreMessages.ContainsBuckets<T> {
        private static final long serialVersionUID = 1L;

        public UpdateRemoteBuckets(final Map<Address, Bucket<T>> buckets) {
            super(buckets);
        }
    }

    /**
     * Message sent from the gossiper to its parent, therefore not Serializable, requesting removal
     * of a bucket corresponding to an address.
     */
    public static final class RemoveRemoteBucket {
        private final Address address;

        public RemoveRemoteBucket(final Address address) {
            this.address = Preconditions.checkNotNull(address);
        }

        public Address getAddress() {
            return address;
        }
    }
}