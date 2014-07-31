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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Messages {

    public static class BucketStoreMessages{

        public static class GetLocalBucket implements Serializable{}

        public static class ContainsBucket implements Serializable {
            final private Bucket bucket;

            public ContainsBucket(Bucket bucket){
                Preconditions.checkArgument(bucket != null, "bucket can not be null");
                this.bucket = bucket;
            }

            public Bucket getBucket(){
                return bucket;
            }

        }

        public static class UpdateBucket extends ContainsBucket implements Serializable {
            public UpdateBucket(Bucket bucket){
                super(bucket);
            }
        }

        public static class GetLocalBucketReply extends ContainsBucket implements Serializable {
            public GetLocalBucketReply(Bucket bucket){
                super(bucket);
            }
        }

        public static class GetAllBuckets implements Serializable{}

        public static class GetBucketsByMembers implements Serializable{
            private Set<Address> members;

            public GetBucketsByMembers(Set<Address> members){
                Preconditions.checkArgument(members != null, "members can not be null");
                this.members = members;
            }

            public Set<Address> getMembers() {
                return new HashSet<>(members);
            }
        }

        public static class ContainsBuckets implements Serializable{
            private Map<Address, Bucket> buckets;

            public ContainsBuckets(Map<Address, Bucket> buckets){
                Preconditions.checkArgument(buckets != null, "buckets can not be null");
                this.buckets = buckets;
            }

            public Map<Address, Bucket> getBuckets() {
                System.out.println("******" + Thread.currentThread().getName() + " In getBucket, buckets: " + buckets);
                Map<Address, Bucket> copy = new HashMap<>(buckets.size());

                for (Map.Entry<Address, Bucket> entry : buckets.entrySet()){
                    //ignore null entries
                    if ( (entry.getKey() == null) || (entry.getValue() == null) )
                        continue;
                    System.out.println("******" + Thread.currentThread().getName() + " In getBucket, copying");
                    copy.put(entry.getKey(), entry.getValue());
                    System.out.println("******" + Thread.currentThread().getName() + " In getBucket, copied");
                }
                System.out.println("******" + Thread.currentThread().getName() + " In getBucket, copy: " + copy);
                return new HashMap<>(copy);
            }
        }

        public static class GetAllBucketsReply extends ContainsBuckets implements Serializable{
            public GetAllBucketsReply(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }

        public static class GetBucketsByMembersReply extends ContainsBuckets implements Serializable{
            public GetBucketsByMembersReply(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }

        public static class GetBucketVersions implements Serializable{}

        public static class ContainsBucketVersions implements Serializable{
            Map<Address, Long> versions;

            public ContainsBucketVersions(Map<Address, Long> versions) {
                Preconditions.checkArgument(versions != null, "versions can not be null");
                this.versions = versions;
            }

            public Map<Address, Long> getVersions() {
                return Collections.unmodifiableMap(versions);
            }

        }

        public static class GetBucketVersionsReply extends ContainsBucketVersions implements Serializable{
            public GetBucketVersionsReply(Map<Address, Long> versions) {
                super(versions);
            }
        }

        public static class UpdateRemoteBuckets extends ContainsBuckets implements Serializable{
            public UpdateRemoteBuckets(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }
    }

    public static class GossiperMessages{
        public static class Tick implements Serializable {}

        public static final class GossipTick extends Tick {}

        public static final class GossipStatus extends BucketStoreMessages.ContainsBucketVersions implements Serializable{
            private Address from;

            public GossipStatus(Address from, Map<Address, Long> versions) {
                super(versions);
                this.from = from;
            }

            public Address from() {
                return from;
            }
        }

        public static final class GossipEnvelope extends BucketStoreMessages.ContainsBuckets implements Serializable {
            private final Address from;
            private final Address to;

            public GossipEnvelope(Address from, Address to, Map<Address, Bucket> buckets) {
                super(buckets);
                this.to = to;
                this.from = from;
            }

            public Address from() {
                return from;
            }

            public Address to() {
                return to;
            }
        }
    }
}
