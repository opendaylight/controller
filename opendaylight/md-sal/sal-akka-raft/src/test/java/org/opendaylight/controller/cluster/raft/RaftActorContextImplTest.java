/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorContextImpl.
 *
 * @author Thomas Pantelis
 */
public class RaftActorContextImplTest extends AbstractActorTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final TestActorRef<DoNothingActor> actor = actorFactory.createTestActor(
            Props.create(DoNothingActor.class), actorFactory.generateActorId("actor"));

    private final Logger log = LoggerFactory.getLogger(RaftActorContextImplTest.class);

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testGetPeerAddress() {
        Map<String, String> peerMap = new HashMap<>();
        peerMap.put("peer1", "peerAddress1");
        peerMap.put("peer2", null);
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                "test", new ElectionTermImpl(new NonPersistentDataProvider(), "test", log), -1, -1,
                peerMap, configParams, new NonPersistentDataProvider(), log);

        assertEquals("getPeerAddress", "peerAddress1", context.getPeerAddress("peer1"));
        assertEquals("getPeerAddress", null, context.getPeerAddress("peer2"));

        PeerAddressResolver mockResolver = mock(PeerAddressResolver.class);
        doReturn("peerAddress2").when(mockResolver).resolve("peer2");
        doReturn("peerAddress3").when(mockResolver).resolve("peer3");
        configParams.setPeerAddressResolver(mockResolver);

        assertEquals("getPeerAddress", "peerAddress2", context.getPeerAddress("peer2"));
        assertEquals("getPeerAddress", "peerAddress3", context.getPeerAddress("peer3"));

        reset(mockResolver);
        assertEquals("getPeerAddress", "peerAddress1", context.getPeerAddress("peer1"));
        assertEquals("getPeerAddress", "peerAddress2", context.getPeerAddress("peer2"));
        verify(mockResolver, never()).resolve(anyString());
    }

    @Test
    public void testSetPeerAddress() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                "test", new ElectionTermImpl(new NonPersistentDataProvider(), "test", log), -1, -1,
                Maps.newHashMap(ImmutableMap.<String, String>of("peer1", "peerAddress1")), configParams,
                new NonPersistentDataProvider(), log);

        context.setPeerAddress("peer1", "peerAddress1_1");
        assertEquals("getPeerAddress", "peerAddress1_1", context.getPeerAddress("peer1"));

        context.setPeerAddress("peer2", "peerAddress2");
        assertEquals("getPeerAddress", null, context.getPeerAddress("peer2"));
    }

    @Test
    public void testUpdatePeerIds() {
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                "self", new ElectionTermImpl(new NonPersistentDataProvider(), "test", log), -1, -1,
                Maps.newHashMap(ImmutableMap.<String, String>of("peer1", "peerAddress1")),
                new DefaultConfigParamsImpl(), new NonPersistentDataProvider(), log);

        context.updatePeerIds(new ServerConfigurationPayload(Arrays.asList(new ServerInfo("self", false),
                new ServerInfo("peer2", true), new ServerInfo("peer3", false))));
        verifyPeerInfo(context, "peer1", null);
        verifyPeerInfo(context, "peer2", true);
        verifyPeerInfo(context, "peer3", false);
        assertEquals("isVotingMember", false, context.isVotingMember());

        context.updatePeerIds(new ServerConfigurationPayload(Arrays.asList(new ServerInfo("self", true),
                new ServerInfo("peer2", true), new ServerInfo("peer3", true))));
        verifyPeerInfo(context, "peer2", true);
        verifyPeerInfo(context, "peer3", true);
        assertEquals("isVotingMember", true, context.isVotingMember());

        context.updatePeerIds(new ServerConfigurationPayload(Arrays.asList(new ServerInfo("peer2", true),
                new ServerInfo("peer3", true))));
        verifyPeerInfo(context, "peer2", true);
        verifyPeerInfo(context, "peer3", true);
        assertEquals("isVotingMember", false, context.isVotingMember());
    }

    private static void verifyPeerInfo(RaftActorContextImpl context, String peerId, Boolean voting) {
        PeerInfo peerInfo = context.getPeerInfo(peerId);
        if(voting != null) {
            assertNotNull("Expected peer " + peerId, peerInfo);
            assertEquals("getVotingState for " + peerId, voting.booleanValue() ? VotingState.VOTING : VotingState.NON_VOTING,
                    peerInfo.getVotingState());
        } else {
            assertNull("Unexpected peer " + peerId, peerInfo);
        }
    }
}
