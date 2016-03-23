/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ConcurrentMap<String, Address> memberNameToAddress = new ConcurrentHashMap<>();
    private final String shardManagerIdentifier;
    private final String shardManagerType;
    private final String localMemberName;

    public ShardPeerAddressResolver(String shardManagerType, String localMemberName) {
        this.shardManagerIdentifier = ShardManagerIdentifier.builder().type(shardManagerType).build().toString();
        this.shardManagerType = shardManagerType;
        this.localMemberName = localMemberName;
    }

    void addPeerAddress(String memberName, Address address) {
        memberNameToAddress.put(memberName, address);
    }

    void removePeerAddress(String memberName) {
        memberNameToAddress.remove(memberName);
    }

    Address getPeerAddress(String memberName) {
        return memberNameToAddress.get(memberName);
    }

    Collection<String> getShardManagerPeerActorAddresses() {
        Collection<String> peerAddresses = new ArrayList<>();
        for(Map.Entry<String, Address> entry: memberNameToAddress.entrySet()) {
            if(!localMemberName.equals(entry.getKey())) {
                peerAddresses.add(getShardManagerActorPathBuilder(entry.getValue()).toString());
            }
        }

        return peerAddresses;
    }

    ShardIdentifier getShardIdentifier(String memberName, String shardName){
        return ShardIdentifier.builder().memberName(memberName).shardName(shardName).type(shardManagerType).build();
    }

    String getShardActorAddress(String shardName, String memberName) {
        Address memberAddress = memberNameToAddress.get(memberName);
        if(memberAddress != null) {
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
        if(peerId == null) {
            return null;
        }

        ShardIdentifier shardId = ShardIdentifier.builder().fromShardIdString(peerId).build();
        return getShardActorAddress(shardId.getShardName(), shardId.getMemberName());
    }
}
