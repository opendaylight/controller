/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import akka.actor.Address;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * Resolver for remote {@link ShardedDataTreeActor}'s
 */
public class ShardingServiceAddressResolver {

    private final ConcurrentMap<MemberName, Address> memberNameToAddress = new ConcurrentHashMap<>();
    private final String shardingServiceActorIdentifier;
    private final MemberName localMemberName;

    public ShardingServiceAddressResolver(final String shardingServiceActorIdentifier, final MemberName localMemberName) {
        this.shardingServiceActorIdentifier = shardingServiceActorIdentifier;
        this.localMemberName = localMemberName;
    }

    void addPeerAddress(final MemberName memberName, final Address address) {
        memberNameToAddress.put(memberName, address);
    }

    void removePeerAddress(final MemberName memberName) {
        memberNameToAddress.remove(memberName);
    }

    Address getPeerAddress(final MemberName memberName) {
        return memberNameToAddress.get(memberName);
    }

    StringBuilder getActorPathBuilder(final Address address) {
        return new StringBuilder().append(address.toString()).append("/user/").append(shardingServiceActorIdentifier);
    }

    Collection<String> getShardingServicePeerActorAddresses() {
        final Collection<String> peerAddresses =
                memberNameToAddress
                        .entrySet()
                        .stream()
                        .filter(entry -> !localMemberName.equals(entry.getKey()))
                        .map(entry -> getActorPathBuilder(entry.getValue()).toString())
                        .collect(Collectors.toList());

        return peerAddresses;
    }

    public String resolve(final MemberName memberName) {
        Preconditions.checkNotNull(memberName);
        final Address address = memberNameToAddress.get(memberName);
        Preconditions.checkNotNull(address, "Requested member[%s] is not present in the resolver ", memberName.toString());

        return getActorPathBuilder(address).toString();
    }
}
