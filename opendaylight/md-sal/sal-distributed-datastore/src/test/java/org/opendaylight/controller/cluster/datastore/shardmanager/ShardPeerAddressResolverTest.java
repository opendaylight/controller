/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static org.junit.Assert.assertEquals;
import akka.actor.Address;
import com.google.common.collect.Sets;
import java.util.Collection;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

/**
 * Unit tests for ShardPeerAddressResolver.
 *
 * @author Thomas Pantelis
 */
public class ShardPeerAddressResolverTest {

    @Test
    public void testGetShardActorAddress() {
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver("config", "member-1");

        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("default", "member-2"));

        Address address2 = new Address("tcp", "system2");
        resolver.addPeerAddress("member-2", address2);
        assertEquals("getPeerAddress", address2, resolver.getPeerAddress("member-2"));

        Address address3 = new Address("tcp", "system3");
        resolver.addPeerAddress("member-3", address3);
        assertEquals("getPeerAddress", address3, resolver.getPeerAddress("member-3"));

        assertEquals("getShardActorAddress", address2.toString() +
                "/user/shardmanager-config/member-2-shard-default-config",
                resolver.getShardActorAddress("default", "member-2"));

        assertEquals("getShardActorAddress", address3.toString() +
                "/user/shardmanager-config/member-3-shard-default-config",
                resolver.getShardActorAddress("default", "member-3"));

        assertEquals("getShardActorAddress", address2.toString() +
                "/user/shardmanager-config/member-2-shard-topology-config",
                resolver.getShardActorAddress("topology", "member-2"));

        resolver.removePeerAddress("member-2");
        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("default", "member-2"));
        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("topology", "member-2"));
        assertEquals("getShardActorAddress", address3.toString() +
                "/user/shardmanager-config/member-3-shard-default-config",
                resolver.getShardActorAddress("default", "member-3"));
    }

    @Test
    public void testResolve() {
        String type = "config";
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver(type, "member-1");

        String memberName = "member-2";
        String peerId = ShardIdentifier.builder().memberName(memberName ).shardName("default").
                type(type).build().toString();

        assertEquals("resolve", null, resolver.resolve(peerId));

        Address address = new Address("tcp", "system");
        resolver.addPeerAddress(memberName, address);

        String shardAddress = resolver.getShardActorAddress("default", memberName);
        assertEquals("getShardActorAddress", address.toString() +
                "/user/shardmanager-" + type + "/" + memberName + "-shard-default-" + type, shardAddress);

        assertEquals("resolve", shardAddress, resolver.resolve(peerId));
    }

    @Test
    public void testGetShardManagerPeerActorAddresses() {
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver("config", "member-1");

        resolver.addPeerAddress("member-1", new Address("tcp", "system1"));

        Address address2 = new Address("tcp", "system2");
        resolver.addPeerAddress("member-2", address2);

        Address address3 = new Address("tcp", "system3");
        resolver.addPeerAddress("member-3", address3);

        Collection<String> peerAddresses = resolver.getShardManagerPeerActorAddresses();
        assertEquals("getShardManagerPeerActorAddresses", Sets.newHashSet(
                address2.toString() + "/user/shardmanager-config",
                address3.toString() + "/user/shardmanager-config"), Sets.newHashSet(peerAddresses));
    }
}
