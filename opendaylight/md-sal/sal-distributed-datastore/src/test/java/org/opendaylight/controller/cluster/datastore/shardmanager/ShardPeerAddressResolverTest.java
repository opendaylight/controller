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
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.BackoffSupervisorUtils;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

/**
 * Unit tests for ShardPeerAddressResolver.
 *
 * @author Thomas Pantelis
 */
public class ShardPeerAddressResolverTest {
    private static final MemberName MEMBER_1 = MemberName.forName("member-1");
    private static final MemberName MEMBER_2 = MemberName.forName("member-2");
    private static final MemberName MEMBER_3 = MemberName.forName("member-3");

    @Test
    public void testGetShardActorAddress() {
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver("config", MEMBER_1);

        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("default", MEMBER_2));

        Address address2 = new Address("tcp", "system2");
        resolver.addPeerAddress(MEMBER_2, address2);
        assertEquals("getPeerAddress", address2, resolver.getPeerAddress(MEMBER_2));

        Address address3 = new Address("tcp", "system3");
        resolver.addPeerAddress(MEMBER_3, address3);
        assertEquals("getPeerAddress", address3, resolver.getPeerAddress(MEMBER_3));

        assertEquals("getShardActorAddress", address2.toString() + "/user/shardmanager-config/"
                + BackoffSupervisorUtils.getChildActorName("shardmanager-config") + "/member-2-shard-default-config",
                resolver.getShardActorAddress("default", MEMBER_2));

        assertEquals("getShardActorAddress", address3.toString() + "/user/shardmanager-config/"
                + BackoffSupervisorUtils.getChildActorName("shardmanager-config") + "/member-3-shard-default-config",
                resolver.getShardActorAddress("default", MEMBER_3));

        assertEquals("getShardActorAddress", address2.toString() + "/user/shardmanager-config/"
                + BackoffSupervisorUtils.getChildActorName("shardmanager-config") + "/member-2-shard-topology-config",
                resolver.getShardActorAddress("topology", MEMBER_2));

        resolver.removePeerAddress(MEMBER_2);
        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("default", MEMBER_2));
        assertEquals("getShardActorAddress", null, resolver.getShardActorAddress("topology", MEMBER_2));
        assertEquals("getShardActorAddress", address3.toString() + "/user/shardmanager-config/"
                + BackoffSupervisorUtils.getChildActorName("shardmanager-config") + "/member-3-shard-default-config",
                resolver.getShardActorAddress("default", MEMBER_3));
    }

    @Test
    public void testResolve() {
        String type = "config";
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver(type, MEMBER_1);

        MemberName memberName = MEMBER_2;
        String peerId = ShardIdentifier.create("default", memberName, type).toString();

        assertEquals("resolve", null, resolver.resolve(peerId));

        Address address = new Address("tcp", "system");
        resolver.addPeerAddress(memberName, address);

        String shardAddress = resolver.getShardActorAddress("default", memberName);
        assertEquals("getShardActorAddress",
                address.toString() + "/user/shardmanager-" + type
                        + BackoffSupervisorUtils.getChildActorName("/shardmanager-" + type) + "/" + memberName.getName()
                        + "-shard-default-" + type,
                shardAddress);

        assertEquals("resolve", shardAddress, resolver.resolve(peerId));
    }

    @Test
    public void testSetResolved() {
        String type = "config";
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver(type, MEMBER_1);

        String peerId = ShardIdentifier.create("default", MEMBER_2, type).toString();

        String address = "akka.tcp://opendaylight-cluster-data@127.0.0.1:2550/user/shardmanager-" + type
                + "/" + BackoffSupervisorUtils.getChildActorName("shardmanager-" + type) + "/"
                + MEMBER_2.getName() + "-shard-default-" + type;

        resolver.setResolved(peerId, address);

        assertEquals("resolve", address, resolver.resolve(peerId));
    }

    @Test
    public void testGetShardManagerPeerActorAddresses() {
        ShardPeerAddressResolver resolver = new ShardPeerAddressResolver("config", MEMBER_1);

        resolver.addPeerAddress(MEMBER_1, new Address("tcp", "system1"));

        Address address2 = new Address("tcp", "system2");
        resolver.addPeerAddress(MEMBER_2, address2);

        Address address3 = new Address("tcp", "system3");
        resolver.addPeerAddress(MEMBER_3, address3);

        Collection<String> peerAddresses = resolver.getShardManagerPeerActorAddresses();
        assertEquals("getShardManagerPeerActorAddresses", Sets.newHashSet(
                address2.toString() + "/user/shardmanager-config",
                address3.toString() + "/user/shardmanager-config"), Sets.newHashSet(peerAddresses));
    }
}
