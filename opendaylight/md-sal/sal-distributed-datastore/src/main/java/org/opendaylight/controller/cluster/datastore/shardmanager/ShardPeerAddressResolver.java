/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.Address;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
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
    private final ConcurrentMap<MemberName, Address> memberNameToAddress = new ConcurrentHashMap<>();
    private final String shardManagerIdentifier;
    private final String shardManagerType;
    private final MemberName localMemberName;

    ShardPeerAddressResolver(String shardManagerType, MemberName localMemberName) {
        this.shardManagerIdentifier = ShardManagerIdentifier.builder().type(shardManagerType).build().toString();
        this.shardManagerType = shardManagerType;
        this.localMemberName = Preconditions.checkNotNull(localMemberName);
    }

    void addPeerAddress(MemberName memberName, Address address) {
        memberNameToAddress.put(memberName, address);
    }

    void removePeerAddress(MemberName memberName) {
        memberNameToAddress.remove(memberName);
    }

    Address getPeerAddress(MemberName memberName) {
        return memberNameToAddress.get(memberName);
    }

    Collection<String> getShardManagerPeerActorAddresses() {
        Collection<String> peerAddresses = new ArrayList<>();
        for (Map.Entry<MemberName, Address> entry: memberNameToAddress.entrySet()) {
            if (!localMemberName.equals(entry.getKey())) {
                peerAddresses.add(getShardManagerActorPathBuilder(entry.getValue()).toString());
            }
        }

        return peerAddresses;
    }

    ShardIdentifier getShardIdentifier(MemberName memberName, String shardName) {
        return ShardIdentifier.create(shardName, memberName, shardManagerType);
    }

    String getShardActorAddress(String shardName, MemberName memberName) {
        Address memberAddress = memberNameToAddress.get(memberName);
        if (memberAddress != null) {
            return getShardManagerActorPathBuilder(memberAddress).append("/").append(
                    getShardIdentifier(memberName, shardName)).toString();
        }

        return null;
    }

    StringBuilder getShardManagerActorPathBuilder(Address address) {
        return new StringBuilder().append(address.toString()).append("/user/").append(shardManagerIdentifier);
    }

    @Override
    public String resolve(String peerId) {
        if (peerId == null) {
            return null;
        }

        ShardIdentifier shardId = ShardIdentifier.fromShardIdString(peerId);
        return getShardActorAddress(shardId.getShardName(), shardId.getMemberName());
    }
}
