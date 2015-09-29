/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.TerminationMonitor;


public class GossiperTest {

    private static ActorSystem system;
    private static Gossiper gossiper;

    private Gossiper mockGossiper;

    @BeforeClass
    public static void setup() throws InterruptedException {
        system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"));
        system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");

        gossiper = createGossiper();
    }

    @AfterClass
    public static void teardown() {
        if (system != null)
            system.shutdown();
    }

    @Before
    public void createMocks(){
        mockGossiper = spy(gossiper);
    }

    @After
    public void resetMocks(){
        reset(mockGossiper);

    }

    @Test
    public void testReceiveGossipTick_WhenNoRemoteMemberShouldIgnore(){

        mockGossiper.setClusterMembers(Collections.<Address>emptyList());
        doNothing().when(mockGossiper).getLocalStatusAndSendTo(any(Address.class));
        mockGossiper.receiveGossipTick();
        verify(mockGossiper, times(0)).getLocalStatusAndSendTo(any(Address.class));
    }

    @Test
    public void testReceiveGossipTick_WhenRemoteMemberExistsShouldSendStatus(){
        List<Address> members = new ArrayList<>();
        Address remote = new Address("tcp", "member");
        members.add(remote);

        mockGossiper.setClusterMembers(members);
        doNothing().when(mockGossiper).getLocalStatusAndSendTo(any(Address.class));
        mockGossiper.receiveGossipTick();
        verify(mockGossiper, times(1)).getLocalStatusAndSendTo(any(Address.class));
    }

    @Test
    public void testReceiveGossipStatus_WhenSenderIsNonMemberShouldIgnore(){

        Address nonMember = new Address("tcp", "non-member");
        GossipStatus remoteStatus = new GossipStatus(nonMember, mock(Map.class));

        //add a member
        List<Address> members = new ArrayList<>();
        members.add(new Address("tcp", "member"));

        mockGossiper.setClusterMembers(members);
        mockGossiper.receiveGossipStatus(remoteStatus);
        verify(mockGossiper, times(0)).getSender();
    }

    @Test
    public void testReceiveGossip_WhenNotAddressedToSelfShouldIgnore(){
        Address notSelf = new Address("tcp", "not-self");

        GossipEnvelope envelope = new GossipEnvelope(notSelf, notSelf, mock(Map.class));
        doNothing().when(mockGossiper).updateRemoteBuckets(anyMap());
        mockGossiper.receiveGossip(envelope);
        verify(mockGossiper, times(0)).updateRemoteBuckets(anyMap());
    }

    /**
     * Create Gossiper actor and return the underlying instance of Gossiper class.
     *
     * @return instance of Gossiper class
     */
    private static Gossiper createGossiper(){

        final Props props = Props.create(Gossiper.class, false, new RemoteRpcProviderConfig(system.settings().config()));
        final TestActorRef<Gossiper> testRef = TestActorRef.create(system, props, "testGossiper");

        return testRef.underlyingActor();
    }
}