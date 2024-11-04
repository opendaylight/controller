/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;

/**
 * Implementation PeerAddressResolver that resolves address for shard peer ids. This class is owned by the
 * ShardMaanager and passed to Shard actors via the ConfigParams.
 *
 * @author Thomas Pantelis
 */
class ShardPeerAddressResolver implements PeerAddressResolver {
    // Stores a mapping between a member name and the address of the member. The map is concurrent as it
    // will be accessed by multiple threads via the public resolve method.
    private final ConcurrentHashMap<MemberName, Address> memberNameToAddress = new ConcurrentHashMap<>();
    private final ShardManagerIdentifier shardManagerIdentifier;
    private final String shardManagerType;
    private final MemberName localMemberName;

    ShardPeerAddressResolver(final String shardManagerType, final MemberName localMemberName) {
        shardManagerIdentifier = new ShardManagerIdentifier(shardManagerType);
        this.shardManagerType = shardManagerType;
        this.localMemberName = requireNonNull(localMemberName);
    }

    void addPeerAddress(final MemberName memberName, final Address address) {
        memberNameToAddress.put(memberName, address);
    }

    void removePeerAddress(final MemberName memberName) {
        memberNameToAddress.remove(memberName);
    }

    Set<MemberName> getPeerMembers() {
        return memberNameToAddress.keySet();
    }

    Address getPeerAddress(final MemberName memberName) {
        return memberNameToAddress.get(memberName);
    }

    List<String> getShardManagerPeerActorAddresses() {
        final var peerAddresses = new ArrayList<String>();
        for (var entry: memberNameToAddress.entrySet()) {
            if (!localMemberName.equals(entry.getKey())) {
                peerAddresses.add(getShardManagerActorPathBuilder(entry.getValue()).toString());
            }
        }

        return peerAddresses;
    }

    ShardIdentifier getShardIdentifier(final MemberName memberName, final String shardName) {
        return ShardIdentifier.create(shardName, memberName, shardManagerType);
    }

    String getShardActorAddress(final String shardName, final MemberName memberName) {
        final var memberAddress = memberNameToAddress.get(memberName);
        return memberAddress == null ? null : getShardManagerActorPathBuilder(memberAddress)
            .append("/").append(getShardIdentifier(memberName, shardName))
            .toString();
    }

    StringBuilder getShardManagerActorPathBuilder(final Address address) {
        return new StringBuilder().append(address.toString())
            .append("/user/").append(shardManagerIdentifier.toActorName());
    }

    @Override
    public String resolve(final String peerId) {
        if (peerId == null) {
            return null;
        }

        final var shardId = ShardIdentifier.fromShardIdString(peerId);
        return getShardActorAddress(shardId.getShardName(), shardId.getMemberName());
    }

    @Override
    public void setResolved(final String peerId, final String address) {
        memberNameToAddress.put(ShardIdentifier.fromShardIdString(peerId).getMemberName(),
                AddressFromURIString.parse(address));
    }
}
