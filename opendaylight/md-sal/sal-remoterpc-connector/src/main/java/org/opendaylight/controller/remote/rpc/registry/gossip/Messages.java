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
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.ContainsBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.ContainsBuckets;


/**
 * These messages are used by {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore} and
 * {@link org.opendaylight.controller.remote.rpc.registry.gossip.Gossiper} actors.
 */
public class Messages {

    public static class BucketStoreMessages{

        public static class GetAllBuckets implements Serializable {
            private static final long serialVersionUID = 1L;
        }

        public static class GetBucketsByMembers implements Serializable{
            private static final long serialVersionUID = 1L;
            private final Set<Address> members;

            public GetBucketsByMembers(Set<Address> members){
                Preconditions.checkArgument(members != null, "members can not be null");
                this.members = members;
            }

            public Set<Address> getMembers() {
                return new HashSet<>(members);
            }
        }

        public static class ContainsBuckets implements Serializable{
            private static final long serialVersionUID = -4940160367495308286L;

            private final Map<Address, Bucket> buckets;

            public ContainsBuckets(Map<Address, Bucket> buckets){
                Preconditions.checkArgument(buckets != null, "buckets can not be null");
                this.buckets = buckets;
            }

            public Map<Address, Bucket> getBuckets() {
                Map<Address, Bucket> copy = new HashMap<>(buckets.size());

                for (Map.Entry<Address, Bucket> entry : buckets.entrySet()){
                    //ignore null entries
                    if ( (entry.getKey() == null) || (entry.getValue() == null) ) {
                        continue;
                    }
                    copy.put(entry.getKey(), entry.getValue());
                }
                return copy;
            }
        }

        public static class GetAllBucketsReply extends ContainsBuckets implements Serializable{
            private static final long serialVersionUID = 1L;
            public GetAllBucketsReply(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }

        public static class GetBucketsByMembersReply extends ContainsBuckets implements Serializable{
            private static final long serialVersionUID = 1L;
            public GetBucketsByMembersReply(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }

        public static class GetBucketVersions implements Serializable {
            private static final long serialVersionUID = 1L;
        }

        public static class ContainsBucketVersions implements Serializable{
            private static final long serialVersionUID = -8172148925383801613L;

            Map<Address, Long> versions;

            public ContainsBucketVersions(Map<Address, Long> versions) {
                Preconditions.checkArgument(versions != null, "versions can not be null or empty");

                this.versions = versions;
            }

            public Map<Address, Long> getVersions() {
                return Collections.unmodifiableMap(versions);
            }

        }

        public static class GetBucketVersionsReply extends ContainsBucketVersions implements Serializable{
            private static final long serialVersionUID = 1L;
            public GetBucketVersionsReply(Map<Address, Long> versions) {
                super(versions);
            }
        }

        public static class UpdateRemoteBuckets extends ContainsBuckets implements Serializable{
            private static final long serialVersionUID = 1L;
            public UpdateRemoteBuckets(Map<Address, Bucket> buckets) {
                super(buckets);
            }
        }
    }

    public static class GossiperMessages{
        public static class Tick implements Serializable {
            private static final long serialVersionUID = -4770935099506366773L;
        }

        public static final class GossipTick extends Tick {
            private static final long serialVersionUID = 5803354404380026143L;
        }

        public static final class GossipStatus extends ContainsBucketVersions implements Serializable{
            private static final long serialVersionUID = -593037395143883265L;

            private final Address from;

            public GossipStatus(Address from, Map<Address, Long> versions) {
                super(versions);
                this.from = from;
            }

            public Address from() {
                return from;
            }
        }

        public static final class GossipEnvelope extends ContainsBuckets implements Serializable {
            private static final long serialVersionUID = 8346634072582438818L;

            private final Address from;
            private final Address to;

            public GossipEnvelope(Address from, Address to, Map<Address, Bucket> buckets) {
                super(buckets);
                Preconditions.checkArgument(to != null, "Recipient of message must not be null");
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
