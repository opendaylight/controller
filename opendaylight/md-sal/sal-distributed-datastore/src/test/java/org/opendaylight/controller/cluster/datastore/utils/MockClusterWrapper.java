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
import akka.cluster.ClusterEvent;
import akka.cluster.MemberStatus;
import akka.cluster.UniqueAddress;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import scala.collection.JavaConversions;

public class MockClusterWrapper implements ClusterWrapper{

    private Address selfAddress = new Address("akka.tcp", "test", "127.0.0.1", 2550);
    private String currentMemberName = "member-1";

    public MockClusterWrapper() {
    }

    public MockClusterWrapper(String currentMemberName) {
        this.currentMemberName = currentMemberName;
    }

    @Override
    public void subscribeToMemberEvents(ActorRef actorRef) {
    }

    @Override
    public String getCurrentMemberName() {
        return currentMemberName;
    }

    @Override
    public Address getSelfAddress() {
        return selfAddress;
    }

    public void setSelfAddress(Address selfAddress) {
        this.selfAddress = selfAddress;
    }

    public static void sendMemberUp(ActorRef to, String memberName, String address){
        to.tell(createMemberUp(memberName, address), null);
    }

    public static void sendMemberRemoved(ActorRef to, String memberName, String address){
        to.tell(createMemberRemoved(memberName, address), null);
    }

    private static ClusterEvent.MemberRemoved createMemberRemoved(String memberName, String address) {
        akka.cluster.UniqueAddress uniqueAddress = new UniqueAddress(
            AddressFromURIString.parse(address), 55);

        Set<String> roles = new HashSet<>();

        roles.add(memberName);

        akka.cluster.Member member = new akka.cluster.Member(uniqueAddress, 1, MemberStatus
            .removed(),
            JavaConversions.asScalaSet(roles).<String>toSet());

        return new ClusterEvent.MemberRemoved(member, MemberStatus.up());

    }


    private static ClusterEvent.MemberUp createMemberUp(String memberName, String address) {
        akka.cluster.UniqueAddress uniqueAddress = new UniqueAddress(
            AddressFromURIString.parse(address), 55);

        Set<String> roles = new HashSet<>();

        roles.add(memberName);

        akka.cluster.Member member = new akka.cluster.Member(uniqueAddress, 1, MemberStatus.up(),
            JavaConversions.asScalaSet(roles).<String>toSet());

        return new ClusterEvent.MemberUp(member);
    }
}
