/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.UniqueAddress;
import akka.util.Version;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import scala.collection.immutable.Set.Set1;

public class MockClusterWrapper implements ClusterWrapper {

    private Address selfAddress = new Address("akka", "test", "127.0.0.1", 2550);
    private final MemberName currentMemberName;

    public MockClusterWrapper() {
        this("member-1");
    }

    public MockClusterWrapper(final String currentMemberName) {
        this.currentMemberName = MemberName.forName(currentMemberName);
    }

    @Override
    public void subscribeToMemberEvents(final ActorRef actorRef) {
    }

    @Override
    public MemberName getCurrentMemberName() {
        return currentMemberName;
    }

    @Override
    public Address getSelfAddress() {
        return selfAddress;
    }

    public void setSelfAddress(final Address selfAddress) {
        this.selfAddress = selfAddress;
    }

    public static void sendMemberUp(final ActorRef to, final String memberName, final String address) {
        to.tell(createMemberUp(memberName, address), null);
    }

    public static void sendMemberRemoved(final ActorRef to, final String memberName, final String address) {
        to.tell(createMemberRemoved(memberName, address), null);
    }

    public static MemberRemoved createMemberRemoved(final String memberName, final String address) {
        UniqueAddress uniqueAddress = new UniqueAddress(AddressFromURIString.parse(address), 55L);
        Member member = new Member(uniqueAddress, 1, MemberStatus.removed(), new Set1<>(memberName), Version.Zero());

        return new MemberRemoved(member, MemberStatus.up());
    }

    public static MemberUp createMemberUp(final String memberName, final String address) {
        UniqueAddress uniqueAddress = new UniqueAddress(AddressFromURIString.parse(address), 55L);
        Member member = new Member(uniqueAddress, 1, MemberStatus.up(), new Set1<>(memberName), Version.Zero());

        return new MemberUp(member);
    }

    public static UnreachableMember createUnreachableMember(final String memberName, final String address) {
        UniqueAddress uniqueAddress = new UniqueAddress(AddressFromURIString.parse(address), 55L);
        Member member = new Member(uniqueAddress, 1, MemberStatus.up(), new Set1<>(memberName), Version.Zero());

        return new UnreachableMember(member);
    }

    public static ReachableMember createReachableMember(final String memberName, final String address) {
        UniqueAddress uniqueAddress = new UniqueAddress(AddressFromURIString.parse(address), 55L);
        Member member = new Member(uniqueAddress, 1, MemberStatus.up(), new Set1<>(memberName), Version.Zero());

        return new ReachableMember(member);
    }
}
