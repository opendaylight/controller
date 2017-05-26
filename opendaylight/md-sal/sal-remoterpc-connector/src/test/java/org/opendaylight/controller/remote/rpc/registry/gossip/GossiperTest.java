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

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.typesafe.config.ConfigFactory;
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
        JavaTestKit.shutdownActorSystem(system);
    }

    @Before
    public void createMocks() {
        mockGossiper = spy(gossiper);
    }

    @After
    public void resetMocks() {
        reset(mockGossiper);
    }

    @Test
    public void testReceiveGossipTick_WhenNoRemoteMemberShouldIgnore() {
        mockGossiper.setClusterMembers();
        doNothing().when(mockGossiper).getLocalStatusAndSendTo(any(ActorSelection.class));
        mockGossiper.receiveGossipTick();
        verify(mockGossiper, times(0)).getLocalStatusAndSendTo(any(ActorSelection.class));
    }

    @Test
    public void testReceiveGossipTick_WhenRemoteMemberExistsShouldSendStatus() {
        mockGossiper.setClusterMembers(new Address("tcp", "member"));
        doNothing().when(mockGossiper).getLocalStatusAndSendTo(any(ActorSelection.class));
        mockGossiper.receiveGossipTick();
        verify(mockGossiper, times(1)).getLocalStatusAndSendTo(any(ActorSelection.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReceiveGossipStatus_WhenSenderIsNonMemberShouldIgnore() {
        Address nonMember = new Address("tcp", "non-member");
        GossipStatus remoteStatus = new GossipStatus(nonMember, mock(Map.class));

        //add a member
        mockGossiper.setClusterMembers(new Address("tcp", "member"));
        mockGossiper.receiveGossipStatus(remoteStatus);
        verify(mockGossiper, times(0)).getSender();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReceiveGossipWhenNotAddressedToSelfShouldIgnore() {
        doNothing().when(mockGossiper).updateRemoteBuckets(anyMap());
        Address notSelf = new Address("tcp", "not-self");
        mockGossiper.receiveGossip(new GossipEnvelope(notSelf, notSelf, mock(Map.class)));
        verify(mockGossiper, times(0)).updateRemoteBuckets(anyMap());
    }

    /**
     * Create Gossiper actor and return the underlying instance of Gossiper class.
     *
     * @return instance of Gossiper class
     */
    private static Gossiper createGossiper() {
        final RemoteRpcProviderConfig config =
                new RemoteRpcProviderConfig.Builder("unit-test")
                        .withConfigReader(ConfigFactory::load).build();
        final Props props = Gossiper.testProps(config);
        final TestActorRef<Gossiper> testRef = TestActorRef.create(system, props, "testGossiper");

        return testRef.underlyingActor();
    }
}
